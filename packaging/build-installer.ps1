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

$launch4jcPath = $env:HEALTOUCH_LAUNCH4JC
if ([string]::IsNullOrWhiteSpace($launch4jcPath)) {
  $launch4jc = Get-Command launch4jc -ErrorAction SilentlyContinue
  if ($null -ne $launch4jc) { $launch4jcPath = $launch4jc.Source }
}
if ([string]::IsNullOrWhiteSpace($launch4jcPath) -or -not (Test-Path -LiteralPath $launch4jcPath -PathType Leaf)) {
  throw 'Launch4j executable was not found. Set HEALTOUCH_LAUNCH4JC or add launch4jc.exe to PATH.'
}
& $launch4jcPath $launch4jConfig
if ($LASTEXITCODE -ne 0) { throw 'Launch4j packaging failed.' }
& iscc "/DAppVersion=$Version" "/DArch=$Architecture" "/DRuntimeDir=$RuntimeDirectory" "$PSScriptRoot\HealTouch.iss"
if ($LASTEXITCODE -ne 0) { throw 'Inno Setup packaging failed.' }
