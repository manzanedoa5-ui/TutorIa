---
name: risk-scoring
description: Obtener el riesgo y los factores del modelo entrenado de TutorIA al evaluar un semestre.
---

# Uso en TutorIA

Usa `POST /events` con el semestre interno para obtener los casos priorizados. Conserva `id`, `semestre`, `riesgo_estimado` y `senales` de la respuesta. Presenta el riesgo como porcentaje si la interfaz lo requiere. Si un caso no aparece en la selección, conserva el estado sin alerta.

Correcto: mostrar 85,45 % cuando la API devuelve 0.8545. Incorrecto: convertir la ausencia de alerta en riesgo cero.
