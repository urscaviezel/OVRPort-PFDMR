"""Publish only the audited icon inventory on an independent root branch; never master."""
import argparse,pathlib,json,hashlib,subprocess,os,tempfile
p=argparse.ArgumentParser();p.add_argument('--source',type=pathlib.Path,required=True);p.add_argument('--publish',action='store_true');a=p.parse_args()
repo=pathlib.Path(__file__).resolve().parents[1];source=a.source.resolve()
doc=json.loads((source/'icons-index.json').read_text(encoding='utf-8'));assert doc['version']==1
expected={'README.md','icons-index.json'};total=0
for pkg,entry in doc['icons'].items():
 name=entry['file'];assert '/' not in name and '\\' not in name and name in [pkg+ext for ext in ('.webp','.png','.jpg','.jpeg','.gif')]
 path=source/'images'/name;assert path.is_file() and not path.is_symlink();data=path.read_bytes();assert len(data)==entry['size'] and hashlib.sha256(data).hexdigest()==entry['sha256'],name
 expected.add('images/'+name);total+=len(data)
actual={f.relative_to(source).as_posix() for f in source.rglob('*') if f.is_file()};assert actual==expected,(actual-expected,expected-actual)
print(f'AUDITED {len(doc["icons"])} icons, {total} image bytes; no unexpected files',flush=True)
if not a.publish:raise SystemExit(0)
record=pathlib.Path(tempfile.mkdtemp(prefix='icon-publish-',dir=repo/'dist'));env=os.environ.copy();env['GIT_INDEX_FILE']=str(record/'index')
git=['git','--git-dir='+str(repo/'.git'),'--work-tree='+str(source)]
def run(args,capture=True):return subprocess.check_output(git+args,env=env,text=True).strip() if capture else subprocess.run(git+args,env=env,check=True)
print('Creating independent Git tree with a temporary index',flush=True)
run(['read-tree','--empty']);run(['add','--all','--','.'],False);tree=run(['write-tree']);names=set(run(['ls-tree','-r','--name-only',tree]).splitlines());assert names==expected
commit=run(['commit-tree',tree,'-m','Add optional package icon inventory for OVRPort PFDMR'])
run(['update-ref','refs/heads/icon-cache',commit,'0'*40]);(record/'result.json').write_text(json.dumps({'icons':len(doc['icons']),'bytes':total,'commit':commit,'tree':tree},indent=2),encoding='utf-8')
print('Uploading icon-cache root commit '+commit,flush=True)
run(['push','origin',commit+':refs/heads/icon-cache'],False)
print('PUBLISHED icon-cache '+commit,flush=True)
