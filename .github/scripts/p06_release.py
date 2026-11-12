"""P06 validation routing. Signing remains inactive until exact-source review is implemented."""
import json,os
from pathlib import Path
import p06_pipeline as validation

def scope():
    validation.trusted();validation.protected_refs()
    source=validation.git('rev-parse','HEAD')
    if source!=os.environ['GITHUB_SHA']:raise ValueError('Wrong source checkout')
    values={'mode':'checks','source':source,'test-run':os.environ['GITHUB_RUN_ID']}
    with open(os.environ['GITHUB_OUTPUT'],'a') as out:
        for key,value in values.items():out.write(key+'='+value+'\n')
    print(json.dumps(values,indent=2))
if __name__=='__main__':scope()
