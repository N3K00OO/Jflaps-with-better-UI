param(
  [string]$FlatLafVersion = "3.7",
  [string]$AsmVersion = "9.9.1",
  [string]$BatikVersion = "1.17",
  [string]$XmlApisVersion = "1.3.04",
  [string]$InputJar = "JFLAP7.1.jar",
  [string]$OutputJar = "JFLAP7.1.5.1-better-ui.jar",
  [switch]$Fat
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
& (Join-Path $repoRoot "build-modern.ps1") `
  -FlatLafVersion $FlatLafVersion `
  -AsmVersion $AsmVersion `
  -BatikVersion $BatikVersion `
  -XmlApisVersion $XmlApisVersion `
  -InputJar $InputJar `
  -OutputJar $OutputJar `
  -Fat:$Fat
