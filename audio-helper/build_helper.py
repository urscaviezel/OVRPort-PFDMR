"""Compile the checked-in Android helper and embed its smali in the desktop patcher."""
import argparse, pathlib, subprocess
root=pathlib.Path(__file__).resolve().parent
p=argparse.ArgumentParser();p.add_argument('--jdk',required=True);p.add_argument('--sdk',required=True);a=p.parse_args()
jdk=pathlib.Path(a.jdk)/'bin';sdk=pathlib.Path(a.sdk);build=root/'build';build.mkdir(exist_ok=True)
android=sdk/'platforms/android-34/android.jar'
subprocess.run([str(jdk/'javac.exe'),'--release','8','-cp',str(android),'-d',str(build),str(root/'src/moe/crx/pfdmr/Audio48.java')],check=True)
subprocess.run([str(jdk/'java.exe'),'-cp',str(sdk/'build-tools/34.0.0/lib/d8.jar'),'com.android.tools.r8.D8','--min-api','26','--lib',str(android),'--output',str(build),str(build/'moe/crx/pfdmr/Audio48.class')],check=True)
subprocess.run([str(jdk/'java.exe'),'-cp',str(root.parent/'overportcli/libs/smali.jar'),'org.jf.baksmali.Main','disassemble',str(build/'classes.dex'),'-o',str(build/'smali')],check=True)
out=root.parent/'overportcli/src/main/resources/pfdmr/audio';out.mkdir(parents=True,exist_ok=True)
(out/'Audio48.smali').write_bytes((build/'smali/moe/crx/pfdmr/Audio48.smali').read_bytes())
print(out/'Audio48.smali')
