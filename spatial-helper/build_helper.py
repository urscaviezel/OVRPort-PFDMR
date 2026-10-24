"""Compile the checked-in Android helper and embed its smali in the desktop patcher."""
import argparse, pathlib, subprocess
root=pathlib.Path(__file__).resolve().parent
p=argparse.ArgumentParser();p.add_argument('--jdk',required=True);p.add_argument('--sdk',required=True);a=p.parse_args()
jdk=pathlib.Path(a.jdk)/'bin';sdk=pathlib.Path(a.sdk);build=root/'build';build.mkdir(exist_ok=True)
android=sdk/'platforms/android-34/android.jar'
subprocess.run([str(jdk/'javac.exe'),'--release','8','-cp',str(android),'-d',str(build),str(root/'src/moe/crx/pfdmr/SpatialSettings.java')],check=True)
subprocess.run([str(jdk/'java.exe'),'-cp',str(sdk/'build-tools/34.0.0/lib/d8.jar'),'com.android.tools.r8.D8','--min-api','26','--lib',str(android),'--output',str(build),*[str(p) for p in (build/'moe/crx/pfdmr').glob('SpatialSettings*.class')]],check=True)
subprocess.run([str(jdk/'java.exe'),'-cp',str(root.parent/'overportcli/libs/smali.jar'),'org.jf.baksmali.Main','disassemble',str(build/'classes.dex'),'-o',str(build/'smali')],check=True)
out=root.parent/'overportcli/src/main/resources/pfdmr/spatial-zombies';out.mkdir(parents=True,exist_ok=True)
for p in (build/'smali/moe/crx/pfdmr').glob('SpatialSettings*.smali'):
    (out/p.name).write_bytes(p.read_bytes())
print(out/'SpatialSettings.smali')
