$ErrorActionPreference = 'Stop'
$nodeBin = Join-Path $PSScriptRoot '.n8n-runtime/node24/node_modules/node/bin'
$n8n = Join-Path $PSScriptRoot '.n8n-runtime/node_modules/.bin/n8n.cmd'
$env:Path = "$nodeBin;$env:Path"
$env:N8N_USER_FOLDER = Join-Path $PSScriptRoot '.n8n-data'
$env:N8N_LISTEN_ADDRESS = '127.0.0.1'
$env:N8N_ENFORCE_SETTINGS_FILE_PERMISSIONS = 'false'
& $n8n start
exit $LASTEXITCODE
