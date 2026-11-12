---
name: student-data-validation
description: Validar registros académicos sintéticos de TutorIA antes de calcular el riesgo.
---

# Uso en TutorIA

Comprueba el ID interno `SIM-…`, el semestre y las cuatro variables de `modelo.VARIABLES`. Verifica que los números sean finitos, que el promedio esté entre 0 y 20 y que los conteos no sean negativos. Rechaza las filas incompletas y reporta el campo inválido. Usa solo variables disponibles al momento de la evaluación. El campo `desercion` pertenece al entrenamiento.

Correcto: reportar `promedio_previo` inválido si vale 23. Incorrecto: rellenar el promedio faltante con 0.
