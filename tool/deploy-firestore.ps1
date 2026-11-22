# Ejecutar en Windows con tu usuario Firebase (Owner/Editor), no con la SA.
# Desde la carpeta del repo clonado:
#   firebase login
#   .\tool\deploy-firestore.ps1

$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)
firebase deploy --project gowayki-lab --only firestore:rules,firestore:indexes
Write-Host "Listo. Verifica que las rules incluyan heavy_traffic y road_blocked."
