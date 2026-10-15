param([string]$OutputDirectory='submissions/assignment-3',[string]$ManualPdf,[string]$ArchiveName)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$privateManifest=Join-Path $root 'private/coordination/assignment-3/implementation/manual/MANUAL-MANIFEST.json'
if((!$ManualPdf -or !$ArchiveName) -and (Test-Path -LiteralPath $privateManifest)){
 $manualInput=Get-Content -LiteralPath $privateManifest -Raw | ConvertFrom-Json
 if(!$ManualPdf){$ManualPdf=$manualInput.manual};if(!$ArchiveName){$ArchiveName=$manualInput.archiveName}
}
if(!$ManualPdf -or !$ArchiveName){throw 'Provide the verified private ManualPdf and student-ID archive filename.'}
if([IO.Path]::GetFileName($ArchiveName) -ne $ArchiveName -or !$ArchiveName.EndsWith('.zip')){throw 'ArchiveName must be a plain ZIP filename.'}
$manual=[IO.Path]::GetFullPath($ManualPdf)
if(!(Test-Path -LiteralPath $manual)){throw 'Manual PDF missing'}
$header=[IO.File]::ReadAllBytes($manual)
if($header.Length -lt 5 -or [Text.Encoding]::ASCII.GetString($header,0,5) -ne '%PDF-'){throw 'ManualPdf is not a PDF'}
$out=if([IO.Path]::IsPathRooted($OutputDirectory)){[IO.Path]::GetFullPath($OutputDirectory)}else{[IO.Path]::GetFullPath((Join-Path $root $OutputDirectory))}
if(!$out.StartsWith([IO.Path]::GetFullPath($root)+'\')){throw 'Package output must be inside this checkout.'}
$build=Join-Path $root 'build/exercise3'
$manifest=Get-Content -LiteralPath (Join-Path $build 'build-manifest.json') -Raw | ConvertFrom-Json
foreach($input in $manifest.inputs){$path=Join-Path $root $input.path;if(!(Test-Path -LiteralPath $path) -or (Get-FileHash -LiteralPath $path).Hash -ne $input.sha256){throw ('Build input changed: '+$input.path)}}
$report=[xml](Get-Content -LiteralPath (Join-Path $manifest.reports 'TEST-junit-jupiter.xml') -Raw)
if([int]$report.testsuite.tests -lt 1 -or [int]$report.testsuite.failures -ne 0 -or [int]$report.testsuite.errors -ne 0 -or [int]$report.testsuite.skipped -ne 0){throw 'A passing nonempty complete core test report is required.'}
if($manifest.target -in @('dto','client-native')){throw 'Use a full core/all build before packaging.'}
$war=Join-Path $build 'dev/guessmarket.war'
if((Get-FileHash -LiteralPath $war).Hash -ne $manifest.war){throw 'Built WAR identity changed.'}
$staging=Join-Path $out ('candidate-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
$zip=Join-Path $out $ArchiveName
if((Test-Path -LiteralPath $zip) -or (Test-Path -LiteralPath $staging)){throw 'Existing candidate preserved. Choose a new output directory.'}
New-Item -ItemType Directory -Force $staging | Out-Null
Copy-Item -LiteralPath $war -Destination (Join-Path $staging 'guessmarket.war')
Copy-Item -LiteralPath (Join-Path $build 'dev/client') -Destination (Join-Path $staging 'client') -Recurse
Copy-Item -LiteralPath $manual -Destination (Join-Path $staging 'README.pdf')
Copy-Item -LiteralPath (Join-Path $root 'README-EXERCISE3.md') -Destination (Join-Path $staging 'README.txt')
New-Item -ItemType Directory -Force (Join-Path $staging 'licenses') | Out-Null
Copy-Item -LiteralPath (Join-Path $root 'tools/exercise3/licenses') -Destination (Join-Path $staging 'licenses/exercise3') -Recurse
Copy-Item -LiteralPath (Join-Path $root 'tools/jaxb-ri-4.0.5/LICENSE.txt') -Destination (Join-Path $staging 'licenses/JAXB-LICENSE.txt')
Copy-Item -LiteralPath (Join-Path $root 'dependencies/exercise3-dependencies.json') -Destination (Join-Path $staging 'licenses/dependency-manifest.json')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive=[IO.Compression.ZipFile]::OpenRead((Join-Path $staging 'guessmarket.war'))
try{
 $libs=@($archive.Entries | Where-Object FullName -Match '^WEB-INF/lib/[^/]+\.jar$' | ForEach-Object {[IO.Path]::GetFileName($_.FullName)})
 $expected=@('guessmarket-dto.jar','guessmarket-engine.jar','gson.jar','errorprone.jar','jakarta.activation-api.jar','angus-activation.jar','jakarta.xml.bind-api.jar','jaxb-core.jar','jaxb-impl.jar')
 if((Compare-Object ($libs|Sort-Object) ($expected|Sort-Object))){throw 'WAR library membership mismatch'}
 if(@($archive.Entries | Where-Object FullName -Match 'javafx|okhttp|servlet-api').Count){throw 'Forbidden WAR dependency'}
}finally{$archive.Dispose()}
$clientLibs=@(Get-ChildItem (Join-Path $staging 'client/lib') -Filter '*.jar' | ForEach-Object Name)
$expectedClient=@('guessmarket-dto.jar','guessmarket-client.jar','gson.jar','errorprone.jar','okhttp.jar','okio.jar','kotlin-stdlib.jar','kotlin-common.jar','kotlin-jdk7.jar','kotlin-jdk8.jar','annotations.jar')
if((Compare-Object ($clientLibs|Sort-Object) ($expectedClient|Sort-Object))){throw 'Client library membership mismatch'}
if(@(Get-ChildItem (Join-Path $staging 'client/javafx/lib') -Filter '*.jar').Count -ne 4){throw 'Expected four JavaFX modules'}
$files=@(Get-ChildItem -LiteralPath $staging -Recurse -File)
if(@($files | Where-Object {$_.Extension -in @('.java','.class','.log','.ser') -or $_.Name -match 'junit|Test\.class'}).Count){throw 'Private/source/test artifact in package'}
[IO.Compression.ZipFile]::CreateFromDirectory($staging,$zip,[IO.Compression.CompressionLevel]::Optimal,$false)
if((Get-Item -LiteralPath $zip).Length -gt 100000000){throw 'Archive exceeds 100 MB'}
$members=@($files | ForEach-Object {@{path=$_.FullName.Substring($staging.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_.FullName).Hash;bytes=$_.Length}})
$result=@{archive=$zip;sha256=(Get-FileHash -LiteralPath $zip).Hash;bytes=(Get-Item -LiteralPath $zip).Length;staging=$staging;manualSHA256=(Get-FileHash -LiteralPath (Join-Path $staging 'README.pdf')).Hash;sourceBuildManifestSHA256=(Get-FileHash -LiteralPath (Join-Path $build 'build-manifest.json')).Hash;testCount=[int]$report.testsuite.tests;members=$members;status='Local candidate, independent/native/Windows10/publication gates pending'}
$result | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $out 'PACKAGE-MANIFEST.json')
Write-Output ('PACKAGED '+$zip+' SHA256='+$result.sha256+' bytes='+$result.bytes+' files='+$members.Count)
