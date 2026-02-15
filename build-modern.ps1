param(
  [string]$FlatLafVersion = "3.7",
  [string]$AsmVersion = "9.9.1",
  [string]$BatikVersion = "1.17",
  [string]$XmlApisVersion = "1.3.04",
  [string]$InputJar = "JFLAP7.1.jar",
  [string]$OutputJar = "JFLAP7.1.5.1-modern.jar"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repoRoot

$flatlafJar = Join-Path $repoRoot "deps/flatlaf-$FlatLafVersion.jar"
if (!(Test-Path $flatlafJar)) {
  New-Item -ItemType Directory -Force -Path (Split-Path -Parent $flatlafJar) | Out-Null
  Invoke-WebRequest `
    -Uri "https://repo1.maven.org/maven2/com/formdev/flatlaf/$FlatLafVersion/flatlaf-$FlatLafVersion.jar" `
    -OutFile $flatlafJar
}

$asmJar = Join-Path $repoRoot "deps/asm-$AsmVersion.jar"
if (!(Test-Path $asmJar)) {
  New-Item -ItemType Directory -Force -Path (Split-Path -Parent $asmJar) | Out-Null
  Invoke-WebRequest `
    -Uri "https://repo1.maven.org/maven2/org/ow2/asm/asm/$AsmVersion/asm-$AsmVersion.jar" `
    -OutFile $asmJar
}

$batikJar = Join-Path $repoRoot "deps/batik-all-$BatikVersion.jar"
if (!(Test-Path $batikJar)) {
  New-Item -ItemType Directory -Force -Path (Split-Path -Parent $batikJar) | Out-Null
  Invoke-WebRequest `
    -Uri "https://repo1.maven.org/maven2/org/apache/xmlgraphics/batik-all/$BatikVersion/batik-all-$BatikVersion.jar" `
    -OutFile $batikJar
}

$xmlApisJar = Join-Path $repoRoot "deps/xml-apis-$XmlApisVersion.jar"
if (!(Test-Path $xmlApisJar)) {
  New-Item -ItemType Directory -Force -Path (Split-Path -Parent $xmlApisJar) | Out-Null
  Invoke-WebRequest `
    -Uri "https://repo1.maven.org/maven2/xml-apis/xml-apis/$XmlApisVersion/xml-apis-$XmlApisVersion.jar" `
    -OutFile $xmlApisJar
}

if (!(Test-Path (Join-Path $repoRoot $InputJar))) {
  throw "Input jar not found: $InputJar"
}

$tempDir = Join-Path $env:TEMP ("jflap-modern-" + [Guid]::NewGuid().ToString("n"))
$classesDir = Join-Path $tempDir "classes"
$workDir = Join-Path $tempDir "jarwork"
$manifest = Join-Path $tempDir "MANIFEST.MF"

New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
New-Item -ItemType Directory -Force -Path $workDir | Out-Null

$javaFiles = Get-ChildItem -Path (Join-Path $repoRoot "launcher-src") -Recurse -Filter *.java -File
if ($javaFiles.Count -eq 0) {
  throw "No Java sources found under launcher-src/"
}

$classpath = (Join-Path $repoRoot $InputJar) + ";" + $flatlafJar + ";" + $batikJar + ";" + $xmlApisJar
javac --release 8 -d $classesDir -classpath $classpath $javaFiles.FullName
if ($LASTEXITCODE -ne 0) {
  throw "javac failed with exit code $LASTEXITCODE"
}

Push-Location $workDir
try {
  jar xf (Join-Path $repoRoot $InputJar)
  jar xf $flatlafJar
  jar xf $batikJar

  $xmlApisDir = Join-Path $tempDir "xml-apis"
  New-Item -ItemType Directory -Force -Path $xmlApisDir | Out-Null
  Push-Location $xmlApisDir
  try {
    jar xf $xmlApisJar
  } finally {
    Pop-Location
  }

  Get-ChildItem -Path $xmlApisDir | Where-Object { $_.Name -ne "license" } | ForEach-Object {
    Copy-Item -Recurse -Force -Path $_.FullName -Destination $workDir
  }

  $xmlApisLicense = Join-Path $xmlApisDir "license"
  if (Test-Path $xmlApisLicense) {
    $licenseDest = Join-Path $workDir "licenses/xml-apis"
    New-Item -ItemType Directory -Force -Path $licenseDest | Out-Null
    Copy-Item -Recurse -Force -Path (Join-Path $xmlApisLicense "*") -Destination $licenseDest
  }

  $patcherSources = Get-ChildItem -Path (Join-Path $repoRoot "tools-src/patch") -Filter *.java -File -ErrorAction SilentlyContinue
  if ($patcherSources -and $patcherSources.Count -gt 0) {
    $patcherTemp = Join-Path $tempDir "patcher"
    $patcherClasses = Join-Path $patcherTemp "classes"
    New-Item -ItemType Directory -Force -Path $patcherClasses | Out-Null

    javac --release 8 -d $patcherClasses -classpath $asmJar $patcherSources.FullName
    if ($LASTEXITCODE -ne 0) {
      throw "javac (patcher) failed with exit code $LASTEXITCODE"
    }

    $patcherCp = "$patcherClasses;$asmJar"

    $patchTargets = @(
      @{ Class = "patch.AutomatonPanePatcher"; Target = "gui/viewer/AutomatonPane.class" },
      @{ Class = "patch.CurvedArrowPatcher"; Target = "gui/viewer/CurvedArrow.class" },
      @{ Class = "patch.AutomatonDrawerPatcher"; Target = "gui/viewer/AutomatonDrawer.class" },
      @{ Class = "patch.SelectionDrawerPatcher"; Target = "gui/viewer/SelectionDrawer.class" },
      @{ Class = "patch.StateDrawerPatcher"; Target = "gui/viewer/StateDrawer.class" }
    )

    foreach ($t in $patchTargets) {
      $targetPath = Join-Path $workDir $t.Target
      if (!(Test-Path $targetPath)) {
        continue
      }

      java -classpath $patcherCp $t.Class $targetPath
      if ($LASTEXITCODE -ne 0) {
        throw "Patcher failed: $($t.Class)"
      }
    }
  }

  if (Test-Path (Join-Path $workDir "META-INF/MANIFEST.MF")) {
    Remove-Item -Force (Join-Path $workDir "META-INF/MANIFEST.MF")
  }

  Copy-Item -Recurse -Force -Path (Join-Path $classesDir "*") -Destination $workDir

  $modsFile = Join-Path $repoRoot "MODIFICATIONS.txt"
  if (Test-Path $modsFile) {
    Copy-Item -Force -Path $modsFile -Destination (Join-Path $workDir "MODIFICATIONS.txt")
  }

  $launcherSrc = Join-Path $repoRoot "launcher-src"
  if (Test-Path $launcherSrc) {
    Copy-Item -Recurse -Force -Path $launcherSrc -Destination $workDir
  }

  $toolsSrc = Join-Path $repoRoot "tools-src"
  if (Test-Path $toolsSrc) {
    Copy-Item -Recurse -Force -Path $toolsSrc -Destination $workDir
  }

  @"
Manifest-Version: 1.0
Main-Class: launcher.ModernMain

"@ | Set-Content -NoNewline -Encoding ASCII $manifest

  $outPath = Join-Path $repoRoot $OutputJar
  if (Test-Path $outPath) {
    Remove-Item -Force $outPath
  }

  jar cfm $outPath $manifest -C $workDir .
  if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE"
  }
} finally {
  Pop-Location
  Remove-Item -Recurse -Force $tempDir
}

Write-Host "Built $OutputJar"
