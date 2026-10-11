"""Preserve dependency notices separately, avoiding shaded-JAR name collisions."""
import hashlib, pathlib, re, zipfile
root=pathlib.Path(__file__).resolve().parent.parent
inventory=root/'composeApp/build/dependency-inventory.tsv'
output=root/'licenses/dependencies'; output.mkdir(parents=True,exist_ok=True)
rows=[]
artifacts=[line.split('\t',1) for line in inventory.read_text().splitlines() if line]
artifacts += [('vendored:'+p.stem,str(p)) for p in (root/'overportcli/libs').glob('*.jar')]
for coordinate,path in artifacts:
    jar=pathlib.Path(path)
    if jar.suffix!='.jar': continue
    ident=jar.stem[:55]+'_'+hashlib.sha256(coordinate.encode()).hexdigest()[:8]
    names=[]
    with zipfile.ZipFile(jar) as archive:
        for member in archive.infolist():
            base=pathlib.PurePosixPath(member.filename).name
            if not member.is_dir() and not base.endswith('.class') and re.match(r'(?i)^(license|notice|copying|copyright)([._-].*)?$',base):
                target=output/ident/(hashlib.sha256(member.filename.encode()).hexdigest()[:8]+'_'+base)
                target.parent.mkdir(parents=True,exist_ok=True)
                target.write_bytes(archive.read(member)); names.append(member.filename)
    rows.append('| '+coordinate+' | '+jar.name+' | `'+hashlib.sha256(jar.read_bytes()).hexdigest()+'` | '+('; '.join(names) or 'See upstream project license')+' |')
(root/'licenses/DEPENDENCIES.md').write_text('# Resolved desktop runtime dependencies\n\nGenerated from the release build; project modules and vendored JARs are included.\n\n| Coordinate | File | SHA-256 | Embedded notices |\n|---|---|---|---|\n'+'\n'.join(rows)+'\n',encoding='utf-8')
print('Recorded',len(rows),'artifacts')
