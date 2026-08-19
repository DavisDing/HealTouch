param(
  [Parameter(Mandatory=$true)][ValidateSet('x86','x64')][string]$Architecture,
  [Parameter(Mandatory=$true)][string]$Version,
  [Parameter(Mandatory=$true)][string]$RuntimeDirectory
)
$ErrorActionPreference = 'Stop'

$runtimeJava = Join-Path $RuntimeDirectory 'bin\java.exe'
if (-not (Test-Path $runtimeJava)) { throw "未找到 $Architecture JavaFX 8 Runtime：$runtimeJava" }
$javaFx = Get-ChildItem -Path $RuntimeDirectory -Filter jfxrt.jar -File -Recurse | Select-Object -First 1
if ($null -eq $javaFx) { throw "$Architecture Runtime 不包含 JavaFX 8（jfxrt.jar）。" }

$jars = @(Get-ChildItem "$PSScriptRoot\..\target\healtouch-*-shaded.jar" -File)
if ($jars.Count -ne 1) { throw "预期找到 1 个 shaded jar，实际找到 $($jars.Count) 个；请先执行 mvn clean package。" }
$jar = $jars[0]

$launch4jConfig = "$PSScriptRoot\..\target\launch4j.xml"
Copy-Item "$PSScriptRoot\launch4j.xml" $launch4jConfig -Force
(Get-Content $launch4jConfig) -replace 'healtouch-1.0.0-SNAPSHOT-shaded.jar', $jar.Name |
  Set-Content $launch4jConfig -Encoding UTF8

# Ensure PATH includes Chocolatey and standard installation paths for launch4j and iscc
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User") + ";" + $env:Path

$launch4jcCmd = Get-Command launch4jc -ErrorAction SilentlyContinue
if ($null -eq $launch4jcCmd) {
  $candidate = Get-ChildItem -Path "C:\Program Files*", "C:\tools" -Filter launch4jc.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($candidate) { $launch4jcExe = $candidate.FullName } else { $launch4jcExe = 'launch4jc' }
} else {
  $launch4jcExe = 'launch4jc'
}

$isccCmd = Get-Command iscc -ErrorAction SilentlyContinue
if ($null -eq $isccCmd) {
  $candidate = Get-ChildItem -Path "C:\Program Files*", "C:\tools" -Filter ISCC.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($candidate) { $isccExe = $candidate.FullName } else { $isccExe = 'iscc' }
} else {
  $isccExe = 'iscc'
}

& $launch4jcExe $launch4jConfig
if ($LASTEXITCODE -ne 0) { throw 'Launch4j 打包失败' }
& $isccExe "/DAppVersion=$Version" "/DArch=$Architecture" "/DRuntimeDir=$RuntimeDirectory" "$PSScriptRoot\HealTouch.iss"
if ($LASTEXITCODE -ne 0) { throw 'Inno Setup 打包失败' }
