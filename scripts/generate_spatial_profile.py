"""Derive a small, hash-guarded recipe from owned reference APKs (UnityPy, build-time only).
No game assets are copied: recipes contain changed bytes, offsets and digests.
"""
import argparse,sys,zipfile,hashlib,json,struct,difflib
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--game-root',required=True);a=p.parse_args()
game=Path(a.game_root);sys.path.insert(0,str(game/'analysis/python-deps'))
import UnityPy
sha=lambda b:hashlib.sha256(b).hexdigest()
member='assets/bin/Data/data.unity3d'
with zipfile.ZipFile(game/'com.mrf.tomfoerster.zombiesmr.PFDMR.apk') as z:
 raw=z.read(member);before=UnityPy.load(raw);bundle_hash=sha(raw)
 metadata=z.read('assets/bin/Data/Managed/Metadata/global-metadata.dat')
with zipfile.ZipFile(game/'com.mrf.tomfoerster.zombiesmr.PFDMR.settings-resume.apk') as z:
 after=UnityPy.load(z.read(member));metadata_after=z.read('assets/bin/Data/Managed/Metadata/global-metadata.dat')
changes=[]
for filename,ids in [('globalgamemanagers.assets',[7058,7063,7092]),('sharedassets0.assets',[163,164])]:
 f=before.file.files[filename];f.reader.Position=0;header=f.reader.read_bytes(f.header.data_offset)
 for id in ids:
  obj=f.objects[id];old=obj.get_raw_data();new=after.file.files[filename].objects[id].get_raw_data()
  pattern=struct.pack('<qQI',id,obj.byte_start-f.header.data_offset,obj.byte_size)
  table=header.find(pattern);assert table>=0 and header.count(pattern)==1
  edits=[{'offset':i,'remove':j-i,'data':new[k:l].hex()} for tag,i,j,k,l in difflib.SequenceMatcher(None,old,new,autojunk=False).get_opcodes() if tag!='equal']
  changes.append(dict(file=filename,id=id,start=obj.byte_start,size=obj.byte_size,table=table,dataOffset=f.header.data_offset,fileSize=f.header.file_size,before=sha(old),after=sha(new),edits=edits))
result=dict(bundle=bundle_hash,metadata=sha(metadata),metadataAfter=sha(metadata_after),objects=changes)
out=Path(__file__).resolve().parents[1]/'overportcli/src/main/resources/pfdmr/spatial-zombies/profile.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8');print(out)
