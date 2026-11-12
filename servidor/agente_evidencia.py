"""Agente local de evidencia con acciones deterministas."""

from modelo import VARIABLES

PREGUNTAS = {
    "promedio_previo": "¿Qué dificultades afectaron el rendimiento en el periodo anterior?",
    "creditos_aprobados_previos": "¿Qué dificultó aprobar los créditos del periodo anterior?",
    "cursos_repetidos": "¿En qué cursos repetidos sería útil recibir apoyo?",
    "carga_actual": "¿La carga académica actual resulta manejable?",
}


def preparar_evidencia(caso, registro):
    """Aplica academic-insight y evidence-report a un caso ya validado."""
    factores = []
    pregunta = "¿Qué apoyo académico le sería útil en este periodo?"
    for senal in caso["senales"]:
        variable = senal["variable"]
        aporte = senal["aporte_log_odds"]
        efecto = "neutro"
        if aporte > 0:
            efecto = "aumenta"
        elif aporte < 0:
            efecto = "reduce"
        factores.append({
            "variable": variable,
            "valor": registro[variable],
            "efecto_en_estimacion": efecto,
        })

    factor_prioritario = next(
        (factor["variable"] for factor in factores if factor["efecto_en_estimacion"] == "aumenta"),
        None,
    )
    if factor_prioritario is not None:
        pregunta = PREGUNTAS[factor_prioritario]

    return {
        "indicadores": {campo: registro[campo] for campo in VARIABLES},
        "factores": factores,
        "propuesta": "Revisar los indicadores académicos con el tutor.",
        "pregunta_sugerida": pregunta,
    }
