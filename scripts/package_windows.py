"""Package a source-built desktop JAR plus a separately supplied Temurin JRE."""
import argparse, hashlib, json, os, pathlib, shutil, subprocess, zipfile
root=pathlib.Path(__file__).resolve().parent.parent
p=argparse.ArgumentParser()
p.add_argument('--jar',type=pathlib.Path,required=True)
p.add_argument('--jre',type=pathlib.Path,required=True)
p.add_argument('--output',type=pathlib.Path,default=root/'dist')
a=p.parse_args()
version=next(line.split('=',1)[1].strip() for line in (root/'gradle.properties').read_text().splitlines() if line.startswith('releaseVersion='))
folder=a.output/f'OVRPort-PFDMR-Edition-{version}-Windows'; folder.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(a.jar) as z:
    if 'pfdmr/arm64-v8a/libopenxr_loader.so' not in z.namelist(): raise SystemExit('Missing native wrapper. Build native first, then rebuild the desktop JAR.')
    if 'pfdmr/audio/Audio48.smali' not in z.namelist(): raise SystemExit('Missing audio helper. Build audio-helper first, then rebuild the desktop JAR.')
shutil.copy2(a.jar,folder/'OVRPort-PFDMR.jar')
shutil.copytree(a.jre,folder/'jre',dirs_exist_ok=True)
csc=pathlib.Path(os.environ['WINDIR'])/'Microsoft.NET/Framework64/v4.0.30319/csc.exe'
subprocess.run([str(csc),'/nologo','/target:winexe','/optimize+','/reference:System.Windows.Forms.dll',
    '/out:'+str(folder/'OVRPort-PFDMR.exe'),str(root/'launcher/Launcher.cs')],check=True)
(folder/'Start-OVRPort-PFDMR.cmd').write_text('@echo off\ncd /d "%~dp0"\nstart "" "%~dp0jre\\bin\\javaw.exe" -jar "%~dp0OVRPort-PFDMR.jar" %*\n')
for name in ['README.md','README.de.md','LICENSE','THIRD-PARTY-NOTICES.md','CHANGELOG.md']:
    shutil.copy2(root/name,folder/name)
shutil.copytree(root/'licenses',folder/'licenses',dirs_exist_ok=True)
try: commit=subprocess.check_output(['git','-C',str(root),'rev-parse','HEAD'],text=True).strip()
except Exception: commit='unknown'
info={'version':version,'upstream':'https://github.com/ovrport/app','upstream_commit':'1c7cb335c81369fbfd2957ad0edcb238956a8e3c','source_commit_at_packaging':commit,
      'jar_sha256':hashlib.sha256(a.jar.read_bytes()).hexdigest(),'jre_release':(a.jre/'release').read_text()}
(folder/'BUILD-INFO.json').write_text(json.dumps(info,indent=2))
archive=folder.parent/(folder.name+'.zip')
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for f in sorted(folder.rglob('*')):
        if f.is_file():
            if f.suffix.lower() in ('.apk','.keystore','.idsig','.p12'): raise SystemExit('Unexpected private/test file: '+str(f))
            z.write(f,pathlib.Path(folder.name)/f.relative_to(folder))
with zipfile.ZipFile(archive) as z:
    if z.testzip() is not None: raise SystemExit('Archive integrity check failed')
print(archive)
