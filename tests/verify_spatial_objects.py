"""Independent UnityPy comparison of owned baseline, reference, and patcher output."""
import sys,zipfile,hashlib,gc,json
sys.path.insert(0,sys.argv[1])
import UnityPy
sha=lambda b:hashlib.sha256(b).hexdigest()
def inventory(path):
 with zipfile.ZipFile(path) as z: env=UnityPy.load(z.read('assets/bin/Data/data.unity3d'))
 objects={(o.assets_file.name,o.path_id):sha(o.get_raw_data()) for o in env.objects}
 other={}
 for name,f in env.file.files.items():
  if not hasattr(f,'objects'):
   f.Position=0;other[name]=sha(f.read_bytes(f.Length))
 del env;gc.collect();return objects,other
base,resources=inventory(sys.argv[2]);reference,reference_resources=inventory(sys.argv[3]);actual,actual_resources=inventory(sys.argv[4])
assert base.keys()==reference.keys()==actual.keys()
changed={key for key in base if base[key]!=actual[key]}
expected={('globalgamemanagers.assets',7058),('globalgamemanagers.assets',7063),('globalgamemanagers.assets',7092),('sharedassets0.assets',163),('sharedassets0.assets',164)}
assert changed==expected,changed
assert actual==reference, [key for key in actual if actual[key]!=reference[key]][:20]
assert resources==reference_resources==actual_resources
print('PASS all',len(actual),'Unity objects match the working reference; exactly five differ from baseline; all',len(resources),'resource streams unchanged')
