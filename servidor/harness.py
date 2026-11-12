"""Controles y evidencia determinista para los casos sintéticos de TutorIA."""

import math
import re

from agente_evidencia import preparar_evidencia
from modelo import VARIABLES, priorizar_casos


def procesar_casos(modelo, filas):
    registros = {}
    for fila in filas:
        alumno, periodo = fila["id"], fila["semestre"]
        clave = alumno, periodo
        if not re.fullmatch(r"SIM-\d+", alumno) or periodo < 0 or clave in registros:
            raise ValueError("Código, periodo o combinación duplicada inválidos")
        for campo in VARIABLES:
            valor = fila[campo]
            if not math.isfinite(valor) or valor < 0:
                raise ValueError(f"Valor inválido en {campo}")
            if campo == "promedio_previo" and valor > 20:
                raise ValueError("Promedio fuera de la escala de 0 a 20")
        registros[clave] = fila

    casos = priorizar_casos(modelo, filas)
    for caso in casos:
        fila = registros[caso["id"], caso["semestre"]]
        caso["evidencia"] = {
            **preparar_evidencia(caso, fila),
            "revision_normativa": "pendiente_de_revision_humana",
            "decision": "REQUIRE_APPROVAL",
            "contacto_autorizado": False,
        }
    return casos
