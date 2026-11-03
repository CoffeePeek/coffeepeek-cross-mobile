#!/usr/bin/env python3
"""Fails when a module's values-be does not cover every key of its values (or has different array lengths).

Russian (values) is the base and the fallback; every module that has strings must also ship values-be.
Run from anywhere inside the repository: python3 tools/l10n/check_parity.py
"""
import re,subprocess,sys,collections,os
ROOT=subprocess.run(['git','rev-parse','--show-toplevel'],capture_output=True,text=True).stdout.strip()
files=subprocess.run(['git','ls-files','*/composeResources/values*/*.xml'],capture_output=True,text=True,cwd=ROOT).stdout.split()
mods=collections.defaultdict(lambda: collections.defaultdict(dict))
for f in files:
    m=re.match(r'(.*)/src/[^/]+/composeResources/(values[^/]*)/',f)
    mod,loc=m.group(1),m.group(2)
    s=open(os.path.join(ROOT,f)).read()
    for kind,name in re.findall(r'<(string|plurals|string-array)\s+name="([^"]+)"',s):
        n=0
        if kind=='string-array':
            mm=re.search(r'<string-array\s+name="'+re.escape(name)+r'".*?</string-array>',s,re.S); n=len(re.findall(r'<item',mm.group(0)))
        mods[mod][loc][(kind,name)]=n
bad=False
for mod,locs in sorted(mods.items()):
    ru=locs.get('values',{}); be=locs.get('values-be',{})
    if not be: print(f"{mod}: NO values-be ({len(ru)} keys)"); bad=True; continue
    miss=[k for k in ru if k not in be]; extra=[k for k in be if k not in ru]
    arr=[k for k in ru if k in be and k[0]=='string-array' and ru[k]!=be[k]]
    if miss or extra or arr:
        bad=True; print(f"{mod}: missing-be={len(miss)} extra-be={len(extra)} array-len={len(arr)}",[k[1] for k in miss][:6])
print("OK" if not bad else "ISSUES")
sys.exit(1 if bad else 0)
