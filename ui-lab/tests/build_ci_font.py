"""Regenerate the licensed CI-only CJK subset; requires local fontTools.
Usage: python ui-lab/tests/build_ci_font.py /path/to/NotoSansCJK-Regular.ttc
No webfont is installed into the website. OFL notices remain in fonts/OFL.txt.
"""
import argparse
from pathlib import Path
from fontTools import subset
from fontTools.ttLib import TTFont

args=argparse.ArgumentParser()
args.add_argument('source',type=Path)
args.add_argument('--font-number',type=int,default=2)
options=args.parse_args()
root=Path(__file__).resolve().parents[1]
font=TTFont(options.source,fontNumber=options.font_number,recalcTimestamp=False)
assert font['name'].getDebugName(1)=='Noto Sans CJK SC','Use the documented OFL-licensed source family'
text=''.join((root/name).read_text() for name in ['index.html','app.js','styles.css'])
source_map=font.getBestCmap()
codepoints=sorted({ord(c) for c in text if ord(c)>127 and ord(c) in source_map})
settings=subset.Options();settings.name_IDs=['*'];settings.name_legacy=True;settings.name_languages=['*']
reducer=subset.Subsetter(options=settings);reducer.populate(unicodes=codepoints);reducer.subset(font)
names={1:'Gardenia UI Lab CJK',2:'Regular',3:'GardeniaUILabCJK-Subset-20261107',4:'Gardenia UI Lab CJK Regular',6:'GardeniaUILabCJK-Regular',16:'Gardenia UI Lab CJK',17:'Regular'}
for name in font['name'].names:
    if name.nameID in names:name.string=names[name.nameID].encode(name.getEncoding())
cff=font['CFF '].cff;cff.fontNames=['GardeniaUILabCJK-Regular']
cff.topDictIndex[0].FamilyName='Gardenia UI Lab CJK';cff.topDictIndex[0].FullName='Gardenia UI Lab CJK Regular'
font.save(root/'tests/fonts/GardeniaLabCJK-Subset.otf')
(root/'tests/fonts/codepoints.txt').write_text('\n'.join(f'U+{point:04X}' for point in codepoints)+'\n')
print('Updated CI fallback:',len(codepoints),'glyphs')
