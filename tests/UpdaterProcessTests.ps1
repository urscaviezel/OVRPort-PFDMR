param([Parameter(Mandatory=$true)][string]$Package,[Parameter(Mandatory=$true)][string]$TestRoot)
$ErrorActionPreference='Stop'
$TestRoot=[IO.Path]::GetFullPath($TestRoot)
if(Test-Path -LiteralPath $TestRoot){throw 'Use a fresh test directory.'}
[IO.Directory]::CreateDirectory($TestRoot) | Out-Null
$install=Join-Path $TestRoot 'portable app with spaces'
$stage=Join-Path $install '.pfdmr-update-process-test'
$payload=Join-Path $stage 'payload'
[IO.Directory]::CreateDirectory($payload) | Out-Null
$names=@('OVRPort-PFDMR.jar','OVRPort-PFDMR.exe','PFDMR-Updater.exe','Start-OVRPort-PFDMR.cmd','README.md','README.de.md','LICENSE','THIRD-PARTY-NOTICES.md','CHANGELOG.md','BUILD-INFO.json')
foreach($name in $names){[IO.File]::WriteAllText((Join-Path $install $name),'old');[IO.File]::WriteAllText((Join-Path $payload $name),'new')}
foreach($root in @($install,$payload)) {
    [IO.Directory]::CreateDirectory((Join-Path $root 'jre/bin')) | Out-Null
    [IO.Directory]::CreateDirectory((Join-Path $root 'licenses')) | Out-Null
    [IO.File]::WriteAllText((Join-Path $root 'jre/release'),'fixture runtime')
    [IO.File]::WriteAllText((Join-Path $root 'licenses/test'),'fixture license')
}
foreach($name in @('OVRPort-PFDMR.exe','PFDMR-Updater.exe')){Copy-Item -LiteralPath (Join-Path $Package $name) -Destination (Join-Path $payload $name)}
Copy-Item -LiteralPath (Join-Path $Package 'PFDMR-Updater.exe') -Destination (Join-Path $stage 'installer.exe')
$stub=Join-Path $TestRoot 'Stub.cs'
@'
using System; using System.IO; using System.Threading;
internal static class Stub {
 static void Main(string[] args) {
  if(args.Length==1 && args[0]=="--parent") {Thread.Sleep(5000);return;}
  File.WriteAllText(Path.Combine(Environment.CurrentDirectory,"restart-marker"),String.Join("|",args));
 }
}
'@ | Set-Content -LiteralPath $stub -Encoding UTF8
$stubExe=Join-Path $TestRoot 'Stub.exe'
& "$env:WINDIR/Microsoft.NET/Framework64/v4.0.30319/csc.exe" /nologo /target:winexe "/out:$stubExe" $stub
if($LASTEXITCODE -ne 0){throw 'Stub compilation failed.'}
Copy-Item -LiteralPath $stubExe -Destination (Join-Path $payload 'jre/bin/javaw.exe')
[IO.Directory]::CreateDirectory((Join-Path $install 'workspace')) | Out-Null
[IO.File]::WriteAllText((Join-Path $install 'workspace/keys'),'preserved')
$parent=Start-Process -FilePath $stubExe -ArgumentList '--parent' -WindowStyle Hidden -PassThru
$updater=Start-Process -FilePath (Join-Path $stage 'installer.exe') -ArgumentList @(('"'+$install+'"'),('"'+$stage+'"'),$parent.Id) -WindowStyle Hidden -PassThru
$deadline=(Get-Date).AddSeconds(25)
while((Get-Date) -lt $deadline -and !(Test-Path -LiteralPath (Join-Path $install 'restart-marker'))){Start-Sleep -Milliseconds 200}
if(!(Test-Path -LiteralPath (Join-Path $install 'restart-marker'))){throw "Automatic restart failed; inspect $stage"}
if(Test-Path -LiteralPath $stage){throw 'Successful update staging/old files were not cleaned.'}
if([IO.File]::ReadAllText((Join-Path $install 'workspace/keys')) -ne 'preserved'){throw 'User workspace changed.'}
if([IO.File]::ReadAllText((Join-Path $install 'OVRPort-PFDMR.jar')) -ne 'new'){throw 'Updated JAR not installed.'}
if(!([IO.File]::ReadAllText((Join-Path $install 'restart-marker'))).StartsWith('-jar|')){throw 'Launcher arguments incorrect.'}
Write-Output 'PASS helper waits for app exit, replaces files, restarts through the real launcher, cleans old files, preserves workspace; paths with spaces.'
