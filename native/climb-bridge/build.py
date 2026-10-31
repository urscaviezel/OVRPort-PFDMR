"""Build the Climb-only PFDMR grip-pose trial from pinned FramePort bridge sources."""
import argparse,pathlib,subprocess,urllib.request,hashlib,json,shutil
root=pathlib.Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser();p.add_argument('--ndk',required=True);args=p.parse_args()
base=root/'native/climb-bridge';build=base/'build';build.mkdir(exist_ok=True)
headers=base/'include/openxr';headers.mkdir(parents=True,exist_ok=True)
commit='7f9285bce1ce8b69bb75554bf788666579d0c35e'
for name in ['openxr.h','openxr_platform.h','openxr_platform_defines.h']:
 target=headers/name
 if not target.exists():target.write_bytes(urllib.request.urlopen('https://raw.githubusercontent.com/KhronosGroup/OpenXR-SDK/'+commit+'/include/openxr/'+name,timeout=30).read())
source=root/'third-party/frameport-1.4.0/native/vrapi-bridge'
text=(source/'input.cpp').read_text(encoding='utf-8')
old="""        // VrApi expects the legacy Touch pose, not OpenXR grip. Invert MetaXR's
        // OculusPoseToGrip transform: +60 degrees about X and (0, -0.03, 0.04)m
        // in OpenXR coordinates. Both hands use the same local rigid transform.
        spaceInfo.poseInActionSpace = {
            {-0.5f, 0.0f, 0.0f, 0.8660254038f},
            {0.0f, -0.0196410162f, -0.0459807621f},
        };"""
new="""        // PFDMR Climb trial: use the runtime grip pose directly, without the
        // legacy Meta Touch conversion. Both hands use the identity transform.
        spaceInfo.poseInActionSpace = {{0, 0, 0, 1}, {0, 0, 0}};"""
assert text.count(old)==1,'Pinned input pose block changed'
(build/'input.cpp').write_text(text.replace(old,new),encoding='utf-8')
compiler=pathlib.Path(args.ndk)/'toolchains/llvm/prebuilt/windows-x86_64/bin/clang++.exe'
out=root/'overportcli/src/main/resources/pfdmr/community/libpfdmr_climb_vrapi.so'
cmd=[str(compiler),'--target=aarch64-linux-android29','-std=c++17','-O2','-g','-fPIC','-fvisibility=hidden','-fvisibility-inlines-hidden','-Wall','-Wextra','-Wno-missing-field-initializers','-Werror=return-type','-DXR_NO_PROTOTYPES','-DXR_USE_PLATFORM_ANDROID','-DXR_USE_GRAPHICS_API_VULKAN','-DXR_USE_GRAPHICS_API_OPENGL_ES','-DXR_USE_TIMESPEC','-I'+str(base/'include'),'-I'+str(source),'-shared','-static-libstdc++',str(source/'runtime.cpp'),str(source/'graphics.cpp'),str(build/'input.cpp'),'-Wl,--no-undefined','-Wl,-z,max-page-size=16384','-Wl,--version-script='+str(source/'exports.map'),'-llog','-landroid','-lvulkan','-lEGL','-lGLESv3','-ldl','-o',str(out)]
subprocess.run(cmd,check=True)
manifest=out.parent/'hashes.json';hashes=json.loads(manifest.read_text(encoding='utf-8'));hashes[out.name]=hashlib.sha256(out.read_bytes()).hexdigest();manifest.write_text(json.dumps(hashes,indent=2)+'\n',encoding='utf-8')
print(out)
