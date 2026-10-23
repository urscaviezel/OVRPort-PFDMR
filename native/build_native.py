"""Build the optional ARM64 OpenXR wrapper from checked-in source and headers."""
import argparse, os, pathlib, subprocess
root = pathlib.Path(__file__).resolve().parent
p = argparse.ArgumentParser()
p.add_argument('--ndk', default=os.environ.get('ANDROID_NDK_HOME'), required=not os.environ.get('ANDROID_NDK_HOME'))
p.add_argument('--tests', action='store_true')
a = p.parse_args()
host = 'windows-x86_64' if os.name == 'nt' else 'linux-x86_64'
exe = '.exe' if os.name == 'nt' else ''
compiler = pathlib.Path(a.ndk)/'toolchains/llvm/prebuilt'/host/'bin'/('clang++'+exe)
out = root.parent/'overportcli/src/main/resources/pfdmr/arm64-v8a/libopenxr_loader.so'
out.parent.mkdir(parents=True, exist_ok=True)
base = [str(compiler), '--target=aarch64-linux-android29', '-O2', '-std=c++17', '-I'+str(root/'include')]
subprocess.run(base+['-shared','-fPIC',str(root/'stereo.cpp'),str(root/'forward.cpp'),'-ldl','-llog',
    '-Wl,-Bsymbolic','-Wl,-soname,libopenxr_loader.so','-Wl,-z,max-page-size=16384','-static-libstdc++','-o',str(out)],check=True)
refresh = out.parent/'libpfdmr_refresh.so'
subprocess.run(base+['-DPFDMR_CUSTOM_CREATE_INSTANCE','-shared','-fPIC',str(root/'refresh.cpp'),str(root/'forward.cpp'),'-ldl','-llog',
    '-Wl,-Bsymbolic','-Wl,-soname,libopenxr_loader.so','-Wl,-z,max-page-size=16384','-static-libstdc++','-o',str(refresh)],check=True)
print(out)
if a.tests:
    test = root/'build'; test.mkdir(exist_ok=True)
    subprocess.run(base+['-shared','-fPIC',str(root/'mock_runtime.cpp'),'-Wl,-Bsymbolic','-o',str(test/'libpfdmr_original_openxr.so')],check=True)
    subprocess.run(base+['-fPIE','-pie',str(root/'native_tests.cpp'),'-ldl','-static-libstdc++','-o',str(test/'native-tests')],check=True)

    refresh_test = test/'refresh'; refresh_test.mkdir(exist_ok=True)
    subprocess.run(base+['-shared','-fPIC',str(root/'mock_refresh.cpp'),'-Wl,-Bsymbolic','-o',str(refresh_test/'libpfdmr_refresh_original.so')],check=True)
    subprocess.run(base+['-fPIE','-pie',str(root/'refresh_tests.cpp'),'-ldl','-static-libstdc++','-o',str(refresh_test/'refresh-tests')],check=True)
