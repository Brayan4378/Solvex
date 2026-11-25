# Solvex API

Backend REST en Spring Boot, organizado por capas y conectado al Firestore existente mediante Firebase Admin SDK.

## Capas

- `api`: controladores HTTP y rutas.
- `application`: autenticación y casos de uso.
- `domain`: modelos de negocio.
- `infrastructure.firestore`: acceso remoto a Firestore.
- `config`: Firebase y seguridad JWT.
- `dto`: contratos de entrada y salida.

## Requisitos

- Java 17 o superior.
- Maven 3.6.3 o superior.
- Una cuenta de servicio de Firebase con acceso a Firestore. No guardes el JSON de la cuenta en el repositorio.

El servicio usa el proyecto Firebase actual `solvex-75281`. Configura credenciales mediante Application Default Credentials o `GOOGLE_APPLICATION_CREDENTIALS`.

### Ejecutar localmente en PowerShell

El botón Run de Android Studio inicia la app Android, no este servidor. Mantén el backend ejecutándose en una terminal aparte:

```powershell
Set-Location C:\Projects\Solvex\backend
.\run-local.ps1
```

El script configura Firebase y Google Sign-In para este equipo. Crea una clave JWT local en `.jwt-secret` la primera vez y la reutiliza para que los tokens de sesión sigan sirviendo tras reiniciar el backend. El archivo está ignorado por Git. El JSON de cuenta de servicio está fuera del repositorio y no es `app/google-services.json`.

El backend escucha en `http://localhost:8080`. Comprueba que responde abriendo `http://localhost:8080/actuator/health` antes de probar el registro. En Android Emulator la app usa `http://10.0.2.2:8080/`. En un teléfono físico, configura `SOLVEX_API_BASE_URL` en `gradle.properties` con la IP local del computador, por ejemplo `http://192.168.1.20:8080/`, y recompila la app.

## Rutas

Públicas:

- `POST /api/auth/register` — `{ "nombre", "email", "contrasena" }`
- `POST /api/auth/login` — `{ "email", "contrasena" }`
- `POST /api/auth/google` — `{ "idToken" }`

Las rutas restantes requieren `Authorization: Bearer <accessToken>`:

- `GET /api/usuarios/me`, `PATCH /api/usuarios/me`
- `GET /api/movimientos`, `POST /api/movimientos`, `POST /api/movimientos/importar`
- `PUT /api/movimientos/{id}`, `DELETE /api/movimientos/{id}`
- `GET /api/cuenta`, `PUT /api/cuenta`
- `GET /api/extractos`, `POST /api/extractos`

Las contraseñas nuevas se guardan con BCrypt. En el primer inicio de sesión, las contraseñas heredadas en texto plano se verifican y migran a hash.

## Integración Android

La app Android consume esta API mediante Retrofit; las credenciales de servicio de Firebase viven solo en el backend. En depuración, la app apunta al backend local del emulador (`10.0.2.2:8080`). Para desplegarla, define `SOLVEX_API_BASE_URL` como propiedad Gradle con la URL HTTPS real del servidor. Las sesiones guardadas antes de la integración deberán autenticarse una vez para obtener un JWT.
