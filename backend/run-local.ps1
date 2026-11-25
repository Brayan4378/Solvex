$ErrorActionPreference = 'Stop'

$firebaseKey = 'C:\Projects\firebase\solvex-75281-firebase-adminsdk-fbsvc-b0ea2fdd72.json'
$jwtSecretFile = Join-Path $PSScriptRoot '.jwt-secret'
$javaHomePath = 'C:\Program Files\Java\jdk-17.0.12'

if (-not (Test-Path -LiteralPath $firebaseKey)) {
    throw "No se encontró la clave de Firebase: $firebaseKey"
}

if (-not (Test-Path -LiteralPath $jwtSecretFile)) {
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $secretBytes = New-Object byte[] 32
    $rng.GetBytes($secretBytes)
    $rng.Dispose()
    [Convert]::ToBase64String($secretBytes) | Set-Content -LiteralPath $jwtSecretFile -NoNewline -Encoding ASCII
}

$env:FIREBASE_PROJECT_ID = 'solvex-75281'
$env:GOOGLE_APPLICATION_CREDENTIALS = $firebaseKey
$env:GOOGLE_WEB_CLIENT_ID = '170158622353-af3ls0k64c7a1v9nvqkjeiolokj797rl.apps.googleusercontent.com'
$env:SOLVEX_JWT_SECRET = (Get-Content -LiteralPath $jwtSecretFile -Raw).Trim()

if (Test-Path -LiteralPath $javaHomePath) {
    $env:JAVA_HOME = $javaHomePath
    $env:Path = "$javaHomePath\bin;$env:Path"
}

Set-Location $PSScriptRoot
$settingsFile = Join-Path $env:TEMP 'solvex-maven-settings.xml'
if (Test-Path -LiteralPath $settingsFile) {
    mvn -s $settingsFile spring-boot:run
} else {
    mvn spring-boot:run
}
