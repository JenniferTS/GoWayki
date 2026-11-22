# Firebase gowayki-lab — acceso desde el servidor

## Hecho con la service account (Drive)

- Clave guardada fuera de Git: `~/.config/firebase-tokens/gowayki-lab-sa.json`
- `projects:list` y `apps:list` OK
- SHA1/SHA256 del keystore **debug de este servidor** registrados en la app Android
  - SHA1: `DB:89:F5:84:21:E0:DE:80:40:83:E4:18:5A:06:B5:0B:35:7B:6D:0E`
- Ruleset nuevo creado (`…/rulesets/e8a53b06-…`) con tipos `heavy_traffic` y `road_blocked`

## Deploy de rules (hecho 2026-11-22)

Sharon desplegó con su usuario desde `Downloads\GoWayki` (`feat/kmp-migration`).
Release activo: `…/rulesets/f913b7df-…` (`updateTime` ~21:53Z).
Incluye `heavy_traffic` y `road_blocked`.

La SA sigue sin poder publicar releases; futuros deploys de rules: usuario Owner o rol Rules Admin a la SA.
