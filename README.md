# TutorIA

Demo de seguimiento académico con datos sintéticos. Incluye la app Android, FastAPI, el modelo de riesgo de deserción, el flujo n8n, el harness y el agente de evidencia.

- [Servidor y ejecución local](servidor/README.md)
- [Prompts y resultados](docs/PROMPTS_Y_RESULTADOS.md)
- [Índice de skills](servidor/SKILLS_INDEX.md)

Abre `android/` en Android Studio. El emulador usa `10.0.2.2:8000`. Para la prueba en un celular físico conectado por USB, usa `adb reverse tcp:8000 tcp:8000` con FastAPI activo en la PC.

Importa `servidor/n8n/tutoria_revision.json` en n8n. El flujo solicita las alertas y las registra en la cola de revisión. Las predicciones, el historial y los códigos de alumno son sintéticos.
