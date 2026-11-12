"""Genera, entrena y evalúa el modelo didáctico de deserción."""

import csv
import json
import math
import random
from pathlib import Path

import joblib
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import average_precision_score, brier_score_loss, roc_auc_score
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

SALIDA = Path(__file__).resolve().parent / "resultados"
VARIABLES = ("promedio_previo", "creditos_aprobados_previos", "cursos_repetidos", "carga_actual")
CAPACIDAD = 20


def generar_historial():
    """Simula matrícula, egreso y abandono; reserva dos semestres de seguimiento."""
    rng = random.Random(2026)
    estudiantes = []
    filas = []
    matriculas = set()
    egresos = set()
    for semestre in range(16):
        for _ in range(130):
            estudiantes.append({
                "id": f"SIM-{len(estudiantes) + 1:05}",
                "avance": 0,
                "promedio": round(max(0, min(20, rng.gauss(13, 2.6))), 2),
                "creditos": 0,
                "repetidos": 0,
                "activo": True,
            })
        for estudiante in estudiantes:
            if not estudiante["activo"]:
                continue
            clave = (estudiante["id"], semestre)
            matriculas.add(clave)
            carga = rng.randint(12, 24)
            if estudiante["avance"] > 0:
                filas.append({
                    "id": clave[0],
                    "semestre": semestre,
                    "promedio_previo": estudiante["promedio"],
                    "creditos_aprobados_previos": estudiante["creditos"],
                    "cursos_repetidos": estudiante["repetidos"],
                    "carga_actual": carga,
                })
            estudiante["avance"] += 1
            if estudiante["avance"] == 10:
                egresos.add(clave)
                estudiante["activo"] = False
                continue
            logit = (
                -3.2 + 0.41 * (13 - estudiante["promedio"])
                + 0.31 * estudiante["repetidos"]
                + 0.09 * (18 - estudiante["creditos"])
                - 0.05 * (carga - 18)
            )
            if rng.random() < 1 / (1 + math.exp(-logit)):
                estudiante["activo"] = False
                continue
            promedio = round(max(0, min(20, estudiante["promedio"] + rng.gauss(0, 1.4))), 2)
            estudiante["promedio"] = promedio
            estudiante["creditos"] = max(0, min(carga, round(carga * promedio / 15 + rng.gauss(0, 2))))
            estudiante["repetidos"] = max(0, round((13 - promedio) / 2 + rng.gauss(0, 0.8)))

    for fila in filas:
        identificador, semestre = fila["id"], fila["semestre"]
        if semestre > 13 or (identificador, semestre) in egresos:
            fila["desercion"] = ""
        else:
            fila["desercion"] = int(all(
                (identificador, futuro) not in matriculas
                for futuro in (semestre + 1, semestre + 2)
            ))
    return filas


def matriz(filas):
    return [[fila[variable] for variable in VARIABLES] for fila in filas]


def escribir_csv(nombre, filas):
    with (SALIDA / nombre).open("w", encoding="utf-8", newline="") as archivo:
        escritor = csv.DictWriter(archivo, fieldnames=list(filas[0]))
        escritor.writeheader()
        escritor.writerows(filas)


def evaluar(modelo, filas):
    etiquetas = [fila["desercion"] for fila in filas]
    riesgos = modelo.predict_proba(matriz(filas))[:, 1]
    resumen = []
    for semestre in sorted({fila["semestre"] for fila in filas}):
        casos = [(fila, riesgo) for fila, riesgo in zip(filas, riesgos) if fila["semestre"] == semestre]
        priorizados = sorted(casos, key=lambda caso: (-caso[1], caso[0]["id"]))[:CAPACIDAD]
        regla = sorted(
            (fila for fila, _ in casos if fila["promedio_previo"] < 11),
            key=lambda fila: (fila["promedio_previo"], fila["id"]),
        )[:CAPACIDAD]
        resumen.append({
            "semestre": semestre,
            "estudiantes": len(casos),
            "deserciones": sum(fila["desercion"] for fila, _ in casos),
            "alertas_modelo": len(priorizados),
            "aciertos_modelo": sum(fila["desercion"] for fila, _ in priorizados),
            "alertas_regla": len(regla),
            "aciertos_regla": sum(fila["desercion"] for fila in regla),
        })
    return {
        "roc_auc": round(roc_auc_score(etiquetas, riesgos), 4),
        "average_precision": round(average_precision_score(etiquetas, riesgos), 4),
        "brier": round(brier_score_loss(etiquetas, riesgos), 4),
        "prevalencia": round(sum(etiquetas) / len(etiquetas), 4),
        "comparacion_por_semestre": resumen,
    }


def priorizar_casos(modelo, filas):
    entradas = matriz(filas)
    riesgos = modelo.predict_proba(entradas)[:, 1]
    estandarizadas = modelo[0].transform(entradas)
    coeficientes = modelo[1].coef_[0]
    ranking = sorted(range(len(filas)), key=lambda i: (-riesgos[i], filas[i]["id"]))
    casos = []
    for i in ranking[:CAPACIDAD]:
        aportes = sorted(
            zip(VARIABLES, entradas[i], estandarizadas[i] * coeficientes),
            key=lambda aporte: abs(aporte[2]),
            reverse=True,
        )
        casos.append({
            "id": filas[i]["id"],
            "semestre": filas[i]["semestre"],
            "riesgo_estimado": round(float(riesgos[i]), 4),
            "senales": [
                {"variable": variable, "valor": valor, "aporte_log_odds": round(float(aporte), 4)}
                for variable, valor, aporte in aportes[:2]
            ],
        })
    return casos


def exportar_demo(modelo, filas):
    casos = priorizar_casos(modelo, filas)
    (SALIDA / "alertas_demo.json").write_text(json.dumps(casos, indent=2), encoding="utf-8")


def main():
    SALIDA.mkdir(exist_ok=True)
    historial = generar_historial()
    etiquetadas = [fila for fila in historial if fila["desercion"] != ""]
    # El último desenlace de entrenamiento ya se conoce al iniciar la validación.
    entrenamiento = [fila for fila in etiquetadas if fila["semestre"] <= 7]
    validacion = [fila for fila in etiquetadas if fila["semestre"] == 10]
    prueba = [fila for fila in etiquetadas if fila["semestre"] == 13]
    modelo = make_pipeline(StandardScaler(), LogisticRegression(max_iter=1000))
    modelo.fit(matriz(entrenamiento), [fila["desercion"] for fila in entrenamiento])
    reporte = {
        "datos": "sinteticos",
        "capacidad_por_semestre": CAPACIDAD,
        "filas_entrenamiento": len(entrenamiento),
        "validacion": evaluar(modelo, validacion),
        "prueba": evaluar(modelo, prueba),
    }
    escribir_csv("historial_sintetico.csv", historial)
    joblib.dump({"modelo": modelo, "variables": VARIABLES}, SALIDA / "modelo.joblib")
    (SALIDA / "evaluacion.json").write_text(json.dumps(reporte, indent=2), encoding="utf-8")
    exportar_demo(modelo, [fila for fila in historial if fila["semestre"] == 15])
    print(json.dumps(reporte["prueba"], indent=2))


if __name__ == "__main__":
    main()
