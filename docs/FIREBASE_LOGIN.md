# Autenticación Firebase en este servidor

El login del MCP de Cursor (`auth.firebase.tools`) **pierde la sesión PKCE entre mensajes**,
así que el código `4/0A…` llega cuando el agente ya no puede canjearlo.

## Método estable: token CI

En tu PC (PowerShell), con Firebase CLI:

```powershell
firebase login:ci --no-localhost
```

1. Abre el enlace que imprime.
2. Inicia sesión con la cuenta dueña de `gowayki-lab`.
3. Copia el token largo (`1//…`) y pégalo en el chat.

El agente lo guardará en `~/.config/firebase-tokens/gowayki-lab` (chmod 600, fuera de Git)
y podrá desplegar rules, listar apps y usar emuladores autenticados.

## Alternativa: cuenta de servicio

En Firebase Console → Project settings → Service accounts → Generate new private key.
Pega el JSON en el chat o súbelo; el agente lo guarda fuera de Git y usa
`GOOGLE_APPLICATION_CREDENTIALS` (no sirve para todo lo de Auth de usuarios, sí para Admin/Firestore deploy según permisos).
