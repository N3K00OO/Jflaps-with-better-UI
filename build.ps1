param(
  [string]$FlatLafVersion = "3.7",
  [string]$AsmVersion = "9.9.1",
  [string]$InputJar = "JFLAP7.1-modern-reference.jar",
  [string]$OutputJar = "JFLAP7.1.1-better-ui.jar",
  [switch]$Fat
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
& (Join-Path $repoRoot "build-modern.ps1") `
  -FlatLafVersion $FlatLafVersion `
  -AsmVersion $AsmVersion `
  -InputJar $InputJar `
  -OutputJar $OutputJar `
  -Fat:$Fat
