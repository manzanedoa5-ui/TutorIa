# Prompts y resultados de TutorIA

Este registro resume las solicitudes usadas para construir la demo con datos sintéticos. Los prompts están condensados.

| Etapa | Prompt resumido | Resultado |
| --- | --- | --- |
| Modelo | Entrena en Python un modelo de riesgo de deserción con historial sintético. Separa entrenamiento y prueba por semestre y compara con la regla de promedio menor que 11. | Regresión logística y 20 alertas priorizadas. En el semestre de prueba, el modelo identificó 13 abandonos simulados entre sus 20 alertas y la regla identificó 12. ROC-AUC de 0,877. |
| API | Levanta FastAPI y permite que n8n solicite el cálculo de alertas y que Android consulte la cola. | `POST /events` devuelve los casos priorizados. `POST /review-queue` los registra y `GET /review-queue` consulta los casos guardados por semestre. |
| n8n | Amplía el flujo para separar los casos, registrar los pendientes y resumir la ejecución. | El flujo procesa 20 alertas. El servidor conserva un registro por alumno y semestre al repetir la ejecución. |
| Android | Construye una app Kotlin/XML con buscador, selector de periodos, ficha del alumno y filtro de alertas. Mejora el diseño y conecta la consulta a FastAPI. | La app contiene los datos académicos sintéticos y consulta la cola de la API. La compilación instalada en el celular se probó por USB y la API respondió 200. |
| Registro Android | Añade un formulario para registrar y editar alumnos con sus indicadores académicos. | El código incluye el formulario y almacenamiento local en SQLite. |
| Skills | Crea seis guías para validar datos, interpretar riesgo, explicar señales, revisar normas, proteger datos y preparar la ficha. | Se añadieron seis archivos `SKILL.md` y un índice. Se aplicó manualmente la guía de evidencia al caso `SIM-01857`. |
| Harness y agente | Integra controles en FastAPI y añade un agente que sugiera una pregunta a partir de los factores del modelo. | En la PC de desarrollo, el harness rechazó un puntaje alterado. Desde el contenedor de n8n se verificaron 20 casos con evidencia. Para `SIM-01857`, el agente sugirió preguntar por las dificultades del periodo anterior y el caso requirió aprobación. |

Las métricas del modelo corresponden a la simulación.

- [Código de entrenamiento](../servidor/modelo.py)
- [Flujo de revisión de n8n](../servidor/n8n/tutoria_revision.json)
- [Índice de skills](../servidor/SKILLS_INDEX.md)
