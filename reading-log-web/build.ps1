param(
    [string]$TomcatHome = "C:\xampp\tomcat"
)

$ErrorActionPreference = "Stop"
$projectDir = $PSScriptRoot
$buildDir = Join-Path $projectDir "build"
$classesDir = Join-Path $buildDir "WEB-INF\classes"
$libDir = Join-Path $buildDir "WEB-INF\lib"
$driver = Join-Path $projectDir "lib\postgresql.jar"
$jarCommand = Get-Command jar -ErrorAction SilentlyContinue
$jarExecutable = if ($null -ne $jarCommand) { $jarCommand.Source } else { "C:\Program Files\Java\jdk-21\bin\jar.exe" }

if (-not (Test-Path $driver)) {
    throw "PostgreSQL JDBC driver is missing: $driver"
}
if (-not (Test-Path $jarExecutable)) { throw "JDK jar command was not found." }

if (Test-Path $buildDir) { Remove-Item -LiteralPath $buildDir -Recurse -Force }
New-Item -ItemType Directory -Path $classesDir,$libDir | Out-Null
Copy-Item -Path (Join-Path $projectDir "src\main\webapp\*") -Destination $buildDir -Recurse -Force
Copy-Item -LiteralPath $driver -Destination $libDir

$sources = Get-ChildItem (Join-Path $projectDir "src\main\java") -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
& javac -encoding UTF-8 --release 17 -cp (Join-Path $TomcatHome "lib\servlet-api.jar") -d $classesDir $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$war = Join-Path $projectDir "reading-log.war"
if (Test-Path $war) { Remove-Item -LiteralPath $war -Force }
Push-Location $buildDir
try { & $jarExecutable --create --file $war * } finally { Pop-Location }
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "WAR created: $war"
