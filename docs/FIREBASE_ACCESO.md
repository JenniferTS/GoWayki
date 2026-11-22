# Firebase gowayki-lab — acceso desde el servidor

## Hecho con la service account (Drive)

- Clave guardada fuera de Git: `~/.config/firebase-tokens/gowayki-lab-sa.json`
- `projects:list` y `apps:list` OK
- SHA1/SHA256 del keystore **debug de este servidor** registrados en la app Android
  - SHA1: `DB:89:F5:84:21:E0:DE:80:40:83:E4:18:5A:06:B5:0B:35:7B:6D:0E`
- Ruleset nuevo creado (`…/rulesets/e8a53b06-…`) con tipos `heavy_traffic` y `road_blocked`

## Bloqueo de deploy

La SA **Firebase Admin SDK** no tiene `firebaserules.releases.create/update`.
Las rules **en producción siguen sin** `heavy_traffic` / `road_blocked`.

### Opción 1 (rápida): deploy con tu usuario

```powershell
cd ruta\al\repo\gowayki-kmp   # o JenniferTS/GoWayki branch feat/kmp-migration
firebase login
firebase deploy --project gowayki-lab --only firestore:rules,firestore:indexes
```

O `.\tool\deploy-firestore.ps1`

### Opción 2: dar permiso a la SA

En Google Cloud IAM del proyecto `gowayki-lab`, a  
`firebase-adminsdk-fbsvc@gowayki-lab.iam.gserviceaccount.com`  
añadir rol **Firebase Rules Admin** (`roles/firebaserules.admin`) o **Firebase Admin**.
Luego el agente puede publicar el release.
