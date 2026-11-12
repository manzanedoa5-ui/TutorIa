# Modelo de alerta temprana

Prototipo didáctico del caso UNMSM. Entrena una regresión logística con datos sintéticos y produce una lista de hasta 20 casos ficticios para revisión.

Desde PowerShell, en esta carpeta:

```powershell
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe modelo.py
```

Si el entorno ya está instalado, ejecuta solo la última línea.

| Archivo en `resultados/` | Contenido |
| --- | --- |
| `historial_sintetico.csv` | Filas por estudiante ficticio y semestre, a partir de su segundo semestre. |
| `modelo.joblib` | Modelo entrenado y orden de las variables de entrada. |
| `evaluacion.json` | Métricas y comparación con la regla de promedio previo menor que 11, con un máximo de 20 casos para cada método. |
| `alertas_demo.json` | Los 20 casos con mayor riesgo en el semestre 15 y sus dos contribuciones principales al puntaje. |

La etiqueta se construye observando la ausencia de matrícula en ambos semestres siguientes. Queda vacía si el estudiante egresa ese semestre o si faltan datos de seguimiento. El entrenamiento usa los semestres 1 a 7, la validación el 10 y la prueba el 13. Las separaciones dejan tiempo para conocer el desenlace de cada conjunto antes de pasar al siguiente.

La simulación incorpora relaciones entre rendimiento y abandono. Las métricas sirven para comprobar el flujo sobre esa simulación. Una evaluación institucional requiere datos reales autorizados y una definición de deserción acordada con la universidad.

## FastAPI y n8n en Windows

Abre dos terminales PowerShell en la carpeta del proyecto. Para iniciar FastAPI:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn api:app --host 127.0.0.1 --port 8000
```

La documentación de la API está en http://127.0.0.1:8000/docs. Prueba `GET /cases` para obtener los casos priorizados del último semestre o `POST /events` con `{}` para solicitar un cálculo desde n8n.

En la otra terminal, instala n8n una vez e inícialo:

```powershell
npm.cmd install --prefix .n8n-runtime/node24 --no-audit --no-fund node@24
$env:Path = "$PWD\.n8n-runtime\node24\node_modules\node\bin;$env:Path"
& node 'C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js' install --prefix .n8n-runtime --no-audit --no-fund n8n@2.47.5
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\iniciar-n8n.ps1
```

Abre http://127.0.0.1:5678 y crea la cuenta local si se solicita. Importa `n8n/prueba_tutoria.json` desde el editor y ejecuta el flujo manual. El nodo de FastAPI devuelve los casos generados por el modelo. Este flujo verifica la conexión con datos sintéticos. La aprobación del tutor y las notificaciones corresponden a una etapa posterior.

Si ejecutas n8n en Docker Desktop, importa `n8n/tutoria_revision.json` para usar el flujo ampliado. Reinicia FastAPI para cargar la ruta `POST /review-queue`. El flujo separa las alertas, registra los casos pendientes en `resultados/revision.sqlite3` y resume los casos nuevos y existentes. Ejecutarlo dos veces con el mismo semestre conserva un registro por alumno y semestre.
