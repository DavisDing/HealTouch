param(
  [Parameter(Mandatory=$true)][ValidateSet('x86','x64')][string]$Architecture,
  [Parameter(Mandatory=$true)][string]$Version,
  [Parameter(Mandatory=$true)][string]$RuntimeDirectory
)
$ErrorActionPreference = 'Stop'

$runtimeJava = Join-Path $RuntimeDirectory 'bin\java.exe'
if (-not (Test-Path $runtimeJava)) { throw "The $Architecture JavaFX 8 runtime was not found: $runtimeJava" }
$javaFx = Get-ChildItem -Path $RuntimeDirectory -Filter jfxrt.jar -File -Recurse | Select-Object -First 1
if ($null -eq $javaFx) { throw "$Architecture runtime does not include JavaFX 8 (jfxrt.jar)." }

$jars = @(Get-ChildItem "$PSScriptRoot\..\target\healtouch-*-shaded.jar" -File)
if ($jars.Count -ne 1) { throw "Expected exactly one shaded JAR, but found $($jars.Count). Run mvn clean package first." }
$jar = $jars[0]

$launch4jConfig = "$PSScriptRoot\..\target\launch4j.xml"
Copy-Item "$PSScriptRoot\launch4j.xml" $launch4jConfig -Force
(Get-Content $launch4jConfig) -replace 'healtouch-1.0.0-SNAPSHOT-shaded.jar', $jar.Name |
  Set-Content $launch4jConfig -Encoding UTF8

# Launch4j 3.14's native launch4jc.exe depends on legacy Windows JRE discovery
# (such as registry entries). actions/setup-java configures JAVA_HOME without necessarily
# registering a JRE, so run the packaged Launch4j JAR with the workflow's known Java 8.
$launch4jJar = $env:HEALTOUCH_LAUNCH4J_JAR
if ([string]::IsNullOrWhiteSpace($launch4jJar)) {
  $launch4jcPath = $env:HEALTOUCH_LAUNCH4JC
  if ([string]::IsNullOrWhiteSpace($launch4jcPath)) {
    $launch4jc = Get-Command launch4jc -ErrorAction SilentlyContinue
    if ($null -ne $launch4jc) { $launch4jcPath = $launch4jc.Source }
  }
  if (-not [string]::IsNullOrWhiteSpace($launch4jcPath)) {
    $launch4jJar = Join-Path (Split-Path -Parent $launch4jcPath) 'launch4j.jar'
  }
}
if ([string]::IsNullOrWhiteSpace($launch4jJar) -or -not (Test-Path -LiteralPath $launch4jJar -PathType Leaf)) {
  throw 'Launch4j JAR was not found. Set HEALTOUCH_LAUNCH4J_JAR to the installed launch4j.jar path.'
}

$launch4jJava = $null
$javaHomeCandidates = @($env:HEALTOUCH_JDK_X64, $env:JAVA_HOME) |
  Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
foreach ($javaHome in $javaHomeCandidates) {
  $candidate = Join-Path $javaHome 'bin\java.exe'
  if (Test-Path -LiteralPath $candidate -PathType Leaf) {
    $launch4jJava = $candidate
    break
  }
}
if ($null -eq $launch4jJava) {
  throw 'A Java executable for Launch4j was not found. Set HEALTOUCH_JDK_X64 or JAVA_HOME to a Java 8 JDK.'
}

Write-Host "Running Launch4j JAR with $launch4jJava"
& $launch4jJava -jar $launch4jJar $launch4jConfig
$launch4jExitCode = $LASTEXITCODE
if ($launch4jExitCode -ne 0) { throw "Launch4j packaging failed with exit code $launch4jExitCode." }
& iscc "/DAppVersion=$Version" "/DArch=$Architecture" "/DRuntimeDir=$RuntimeDirectory" "$PSScriptRoot\HealTouch.iss"
if ($LASTEXITCODE -ne 0) { throw 'Inno Setup packaging failed.' }
