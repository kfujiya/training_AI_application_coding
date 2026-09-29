$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$maven = Join-Path $projectRoot '.tools\apache-maven-3.9.10\bin\mvn.cmd'
$tomcatHome = Join-Path $projectRoot '.tools\apache-tomcat-10.1.59'
$war = Join-Path $projectRoot 'target\reading-log.war'
$deployedWar = Join-Path $tomcatHome 'webapps\reading-log.war'

if (-not (Test-Path -LiteralPath $maven)) {
    throw "Maven was not found: $maven"
}
if (-not (Test-Path -LiteralPath $tomcatHome)) {
    throw "Tomcat was not found: $tomcatHome"
}
if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
    throw 'Port 8080 is already in use. Stop the process using it before starting Tomcat.'
}

& $maven package
if ($LASTEXITCODE -ne 0) {
    throw 'Maven build failed.'
}

Copy-Item -LiteralPath $war -Destination $deployedWar -Force

$env:CATALINA_HOME = $tomcatHome
$env:CATALINA_BASE = $tomcatHome

Write-Host 'Tomcat is starting. Open http://localhost:8080/reading-log/ after the startup message appears.'
& (Join-Path $tomcatHome 'bin\catalina.bat') run
