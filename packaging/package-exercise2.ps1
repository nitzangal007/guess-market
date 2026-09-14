param([Parameter(Mandatory=$true)][string]$RuntimeDirectory,[Parameter(Mandatory=$true)][string]$JavaFxLegalDirectory,[Parameter(Mandatory=$true)][string]$ManualPdf,[Parameter(Mandatory=$true)][string]$ArchiveName)
$ErrorActionPreference='Stop'
$packageRepo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$packageBuild=Join-Path $packageRepo 'build/javafx'
$packageRuntime=[IO.Path]::GetFullPath($RuntimeDirectory)
$packageStage=Join-Path $packageBuild 'submission-final'
$packageDistributions=Join-Path $packageBuild 'distributions'
if([IO.Path]::GetFileName($ArchiveName) -ne $ArchiveName -or $ArchiveName -notmatch '^[^<>:"/\\|?*]+\.zip$'){throw 'ArchiveName must be a plain ZIP filename supplied for this submission.'}
if(![IO.Path]::IsPathRooted($ManualPdf) -or !(Test-Path -LiteralPath $ManualPdf -PathType Leaf)){throw 'Supply the verified private manual PDF as an absolute file path.'}
$packagePdfHeader=[IO.File]::ReadAllBytes($ManualPdf)
if($packagePdfHeader.Length -lt 5 -or [Text.Encoding]::ASCII.GetString($packagePdfHeader,0,5) -ne '%PDF-'){throw 'ManualPdf must be a verified PDF file.'}
$packageZip=Join-Path $packageDistributions $ArchiveName
if((Test-Path -LiteralPath $packageStage) -or (Test-Path -LiteralPath $packageZip)){throw 'Package output already exists. Preserve it or run a fresh build before packaging.'}
$packageReport=Join-Path $packageBuild 'reports/TEST-junit-jupiter.xml'
if(!(Test-Path -LiteralPath $packageReport)){throw 'Run build-javafx.bat successfully first.'}
$packageTests=[xml](Get-Content -Raw -LiteralPath $packageReport)
if([int]$packageTests.testsuite.tests -ne 236 -or [int]$packageTests.testsuite.failures -ne 0 -or [int]$packageTests.testsuite.errors -ne 0 -or [int]$packageTests.testsuite.skipped -ne 0){throw 'The complete 236-test build gate has not passed.'}
foreach($packageInput in @('bin/java.exe','lib/modules','legal/java.base/LICENSE','release')){
    if(!(Test-Path -LiteralPath (Join-Path $packageRuntime $packageInput))){throw ('Missing portable runtime input: '+$packageInput)}
}
$packageRelease=Get-Content -LiteralPath (Join-Path $packageRuntime 'release')
if($packageRelease -notcontains 'JAVA_VERSION="25.0.4"'){throw 'Use the verified Java 25.0.4 linked runtime.'}
$packageExe=[IO.File]::ReadAllBytes((Join-Path $packageRuntime 'bin/java.exe'))
$packagePe=[BitConverter]::ToInt32($packageExe,60)
if([BitConverter]::ToUInt16($packageExe,$packagePe+4) -ne 0x8664){throw 'Use a Windows x64 runtime.'}
$packageFxModules=@(& (Join-Path $packageRuntime 'bin/java.exe') --list-modules)
if($LASTEXITCODE -ne 0 -or $packageFxModules -notcontains 'javafx.graphics@25.0.4' -or $packageFxModules -notcontains 'javafx.fxml@25.0.4'){throw 'Use the verified JavaFX 25.0.4 linked runtime.'}
if(!(Test-Path -LiteralPath (Join-Path $JavaFxLegalDirectory 'javafx.graphics/LICENSE'))){throw 'Supply the JavaFX SDK legal directory.'}
New-Item -ItemType Directory -Force -Path $packageStage,$packageDistributions,(Join-Path $packageStage 'lib') | Out-Null
$packageJarNames=@('guessmarket-dto.jar','guessmarket-engine.jar','guessmarket-javafx-ui.jar','jakarta.activation-api.jar','angus-activation.jar','jakarta.xml.bind-api.jar','jaxb-core.jar','jaxb-impl.jar')
foreach($packageName in $packageJarNames){Copy-Item -LiteralPath (Join-Path $packageBuild ('dev/lib/'+$packageName)) -Destination (Join-Path $packageStage 'lib')}
Copy-Item -LiteralPath $packageRuntime -Destination (Join-Path $packageStage 'runtime') -Recurse
Get-ChildItem -LiteralPath $JavaFxLegalDirectory -Directory | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $packageStage 'runtime/legal') -Recurse}
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'run-exercise2.bat') -Destination (Join-Path $packageStage 'run.bat')
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'README-exercise2.md') -Destination (Join-Path $packageStage 'README.md')
Copy-Item -LiteralPath $ManualPdf -Destination (Join-Path $packageStage 'README.pdf')
Copy-Item -LiteralPath (Join-Path $packageRepo 'tools/jaxb-ri-4.0.5/LICENSE.txt') -Destination (Join-Path $packageStage 'JAXB-LICENSE.txt')
[IO.File]::WriteAllText((Join-Path $packageStage 'THIRD-PARTY-NOTICES.txt'),"Runtime: Oracle Java 25.0.4 and JavaFX 25.0.4 Windows x64. Notices and licenses are retained under runtime/legal.`r`nJAXB RI 4.0.5 and its activation dependencies: licenses are retained in the dependency JARs and JAXB-LICENSE.txt.`r`nApplication code: no open-source license granted.`r`n",[Text.UTF8Encoding]::new($false))
$packageInventory=@(Get-ChildItem -LiteralPath $packageStage -Recurse -File | Sort-Object FullName | ForEach-Object {
    [ordered]@{path=$_.FullName.Substring($packageStage.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash;bytes=$_.Length}
})
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::CreateFromDirectory($packageStage,$packageZip,[IO.Compression.CompressionLevel]::Optimal,$false)
$packageArchive=[IO.Compression.ZipFile]::OpenRead($packageZip)
try{$packageMembers=@($packageArchive.Entries | Where-Object {!$_.FullName.EndsWith('/')} | ForEach-Object FullName)}finally{$packageArchive.Dispose()}
if(@(Compare-Object @($packageInventory.path) $packageMembers).Count -ne 0){throw 'ZIP membership differs from staging.'}
$packageManifest=[ordered]@{archive=('distributions/'+$ArchiveName);sha256=(Get-FileHash -LiteralPath $packageZip -Algorithm SHA256).Hash;bytes=(Get-Item -LiteralPath $packageZip).Length;expectedTests=236;inventory=$packageInventory;zipMembers=$packageMembers}
$packageManifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $packageBuild 'package-manifest.json') -Encoding UTF8
Write-Output ('ZIP='+$packageZip)
Write-Output ('SHA256='+$packageManifest.sha256)
Write-Output ('BYTES='+$packageManifest.bytes)
