"""API local para la demostración de TutorIA con datos sintéticos."""

import csv
import sqlite3
from contextlib import asynccontextmanager, closing
from typing import Annotated

import joblib
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from harness import procesar_casos
from modelo import SALIDA, VARIABLES

BASE_REVISION = SALIDA / "revision.sqlite3"


@asynccontextmanager
async def cargar_datos(app):
    artefacto = joblib.load(SALIDA / "modelo.joblib")
    if tuple(artefacto["variables"]) != VARIABLES:
        raise RuntimeError("El esquema del modelo no coincide con la API.")
    with (SALIDA / "historial_sintetico.csv").open(encoding="utf-8", newline="") as archivo:
        filas = [
            {"id": fila["id"], "semestre": int(fila["semestre"]),
             **{variable: float(fila[variable]) for variable in VARIABLES}}
            for fila in csv.DictReader(archivo)
        ]
    app.state.modelo = artefacto["modelo"]
    app.state.filas = filas
    app.state.semestre = max(fila["semestre"] for fila in filas)
    yield


app = FastAPI(title="TutorIA · Demo local", lifespan=cargar_datos)


class Evento(BaseModel):
    semestre: Annotated[int, Field(ge=0)] | None = None


class CasoRevision(BaseModel):
    id: Annotated[str, Field(pattern=r"^SIM-\d+$")]
    semestre: Annotated[int, Field(ge=0)]
    riesgo_estimado: Annotated[float, Field(ge=0, le=1)]


def casos_priorizados(semestre: int | None):
    periodo = app.state.semestre if semestre is None else semestre
    filas = [fila for fila in app.state.filas if fila["semestre"] == periodo]
    if not filas:
        raise HTTPException(status_code=404, detail="Semestre sin registros")
    try:
        return procesar_casos(app.state.modelo, filas)
    except ValueError as error:
        raise HTTPException(status_code=422, detail=str(error)) from error


@app.get("/health")
def salud():
    return {"estado": "ok", "datos": "sinteticos", "semestre": app.state.semestre}


@app.get("/cases")
def consultar_casos(semestre: int | None = None):
    return casos_priorizados(semestre)


@app.post("/events")
def procesar_evento(evento: Evento):
    return {"datos": "sinteticos", "casos": casos_priorizados(evento.semestre)}


@app.get("/review-queue")
def consultar_revision(semestre: Annotated[int, Field(ge=0)] | None = None):
    periodo = app.state.semestre if semestre is None else semestre
    if not BASE_REVISION.exists():
        return []
    with closing(sqlite3.connect(BASE_REVISION)) as db:
        filas = db.execute("""
            SELECT alumno, riesgo, estado
            FROM revision
            WHERE semestre = ?
            ORDER BY riesgo DESC, alumno
        """, (periodo,)).fetchall()
    return [
        {"id": alumno, "semestre": periodo, "riesgo_estimado": riesgo, "estado": estado}
        for alumno, riesgo, estado in filas
    ]


@app.post("/review-queue")
def registrar_revision(caso: CasoRevision):
    esperado = next(
        (item for item in casos_priorizados(caso.semestre) if item["id"] == caso.id),
        None,
    )
    if esperado is None or esperado["riesgo_estimado"] != caso.riesgo_estimado:
        raise HTTPException(status_code=422, detail="El caso o el riesgo no coincide con el modelo")
    with closing(sqlite3.connect(BASE_REVISION)) as db, db:
        db.execute("""
            CREATE TABLE IF NOT EXISTS revision (
                semestre INTEGER NOT NULL,
                alumno TEXT NOT NULL,
                riesgo REAL NOT NULL,
                estado TEXT NOT NULL DEFAULT 'pendiente',
                PRIMARY KEY (semestre, alumno)
            )
        """)
        insercion = db.execute("""
            INSERT INTO revision (semestre, alumno, riesgo) VALUES (?, ?, ?)
            ON CONFLICT (semestre, alumno) DO NOTHING
        """, (caso.semestre, caso.id, caso.riesgo_estimado))
        riesgo, estado = db.execute("""
            SELECT riesgo, estado FROM revision WHERE semestre = ? AND alumno = ?
        """, (caso.semestre, caso.id)).fetchone()
    return {
        "id": caso.id,
        "semestre": caso.semestre,
        "riesgo_estimado": riesgo,
        "estado": estado,
        "nuevo": insercion.rowcount == 1,
    }
