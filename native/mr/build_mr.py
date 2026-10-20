"""Build the optional ARM64 MR compatibility layers and isolated Android tests."""
import argparse, os, pathlib, subprocess
root = pathlib.Path(__file__).resolve().parent
p = argparse.ArgumentParser()
p.add_argument('--ndk', required=True)
p.add_argument('--tests', action='store_true')
a = p.parse_args()
host = 'windows-x86_64' if os.name == 'nt' else 'linux-x86_64'
cc = pathlib.Path(a.ndk)/'toolchains/llvm/prebuilt'/host/'bin'/('clang.exe' if os.name == 'nt' else 'clang')
out = root.parent.parent/'overportcli/src/main/resources/pfdmr/arm64-v8a'
out.mkdir(parents=True, exist_ok=True)
base = [str(cc), '--target=aarch64-linux-android29', '-O2', '-Wall', '-Wextra', '-Werror', '-I'+str(root.parent/'include')]
for source, name in [('xr_probe.c', 'libpfdmr_mr_yvr.so'), ('xr_dispatch.c', 'libpfdmr_mr_dispatch.so')]:
    subprocess.run(base+['-shared', '-fPIC', str(root/source), *([str(root/'forward.c')] if source=='xr_dispatch.c' else [str(root/'yvr_forward.c')]), '-ldl', '-llog',
        '-Wl,-Bsymbolic', '-Wl,-z,max-page-size=16384', '-Wl,-soname,'+name, '-o', str(out/name)], check=True)
if a.tests:
    dest = root/'build'; dest.mkdir(exist_ok=True)
    for name in ['test_pt_gate', 'test_scene_buffers', 'test_room_filter', 'test_scene_components']:
        subprocess.run(base+['-fPIE', '-pie', str(root/(name+'.c')), '-ldl', '-llog', '-o', str(dest/name)], check=True)
print('MR native layers built.')
