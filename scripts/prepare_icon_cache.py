"""Stage the optional GitHub icon-cache branch. Never uploads or modifies source images."""
import argparse,pathlib,re,hashlib,json,shutil
p=argparse.ArgumentParser();p.add_argument('--source',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args()
a.output.mkdir(parents=True,exist_ok=True);images=a.output/'images';images.mkdir(exist_ok=True);entries={};ignored=0
for f in sorted(a.source.iterdir()):
 if not f.is_file() or f.suffix.lower() not in ('.webp','.png','.jpg','.jpeg','.gif') or not re.fullmatch(r'[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+',f.stem):ignored+=1;continue
 data=f.read_bytes()
 valid=len(data)>=12 and (data.startswith(b'\x89PNG\r\n\x1a\n') or data.startswith(b'\xff\xd8') or data.startswith(b'GIF8') or (data.startswith(b'RIFF') and data[8:12]==b'WEBP'))
 if not valid or len(data)>16*1024*1024:ignored+=1;continue
 if f.stem in entries:raise ValueError('Duplicate package '+f.stem)
 entries[f.stem]={'file':f.name,'size':len(data),'sha256':hashlib.sha256(data).hexdigest()}
 shutil.copy2(f,images/f.name)
(a.output/'icons-index.json').write_text(json.dumps({'version':1,'icons':entries},separators=(',',':')),encoding='utf-8')
(a.output/'README.md').write_text('OVRPort PFDMR optional icon cache. Exact package-name mappings; no APKs or application code. The app downloads only an individual missing image and retains it locally. Source: owner-supplied website cache, staged 2026-10-31.\n',encoding='utf-8')
print(f'Staged {len(entries)} icons; ignored {ignored} files. No upload performed.')
