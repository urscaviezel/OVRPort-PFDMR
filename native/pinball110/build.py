"""Build the version-gated PinballFX 1.10 ARM64 profile and optional mock tests."""
import argparse,pathlib,subprocess,os
root=pathlib.Path(__file__).resolve().parent
p=argparse.ArgumentParser();p.add_argument('--ndk',required=True);p.add_argument('--tests',action='store_true');a=p.parse_args()
cc=pathlib.Path(a.ndk)/'toolchains/llvm/prebuilt/windows-x86_64/bin/clang++.exe'
base=[str(cc),'--target=aarch64-linux-android29','-std=c++17','-O2','-Wall','-Wextra','-Werror','-Wno-missing-field-initializers','-I'+str(root.parent/'include')]
out=root.parent.parent/'overportcli/src/main/resources/pfdmr/arm64-v8a/libpfdmr_pinball110.so'
subprocess.run(base+['-shared','-fPIC','-DPFDMR_PINBALL110',str(root/'pinball.cpp'),str(root.parent/'forward.cpp'),'-ldl','-llog','-static-libstdc++','-Wl,-Bsymbolic','-Wl,-soname,libopenxr_loader.so','-Wl,-z,max-page-size=16384','-o',str(out)],check=True)
if a.tests:
 dest=root.parent/'build/pinball110';dest.mkdir(parents=True,exist_ok=True)
 subprocess.run(base+['-shared','-fPIC',str(root/'mock.cpp'),'-Wl,-Bsymbolic','-static-libstdc++','-o',str(dest/'libpfdmr_pinball110_original.so')],check=True)
 subprocess.run(base+['-fPIE','-pie',str(root/'tests.cpp'),'-ldl','-llog','-static-libstdc++','-o',str(dest/'pinball-tests')],check=True)
print(out)
