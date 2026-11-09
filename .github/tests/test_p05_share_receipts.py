"""Host JSON contract fixtures only; never substitutes for native receiver execution."""
import copy,sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
from collect_p05_history import validate_receipt

class ShareReceiptContractTests(unittest.TestCase):
    def fixture(self,kind='multiple',negative=False):
        count=3 if kind=='multiple' else 1
        def entry(i):return {'workoutId':str(i),'rawSamples':3,'sha256':str(i)*64}
        files=[]
        for i in range(1,count+1):
            r={'uri':f'content://com.fox.runner.debug.fileprovider/p0_gpx/batch/Workout-{i}.gpx','read':not negative,'permission':-1 if negative else 0}
            if negative:r['errorClass']='java.lang.SecurityException'
            else:
                r.update(bytes=1024,sha256='a'*64)
                if kind=='zip':r['entries']=[entry(n) for n in (1,2,3)]
                else:r['gpx']=entry(i)
            files.append(r)
        n=1 if kind=='single' else 3
        return dict(sourceCommit='tested',api=35,permissionContractPassed=True,uid=10153,targetUid=10152,authenticatedBinderUid=10153,pid=2000,targetPid=1000,negativeControl=negative,
                    action='de.dennisguse.opentracks.p0.TEST_WITHOUT_GRANT' if negative else ('android.intent.action.SEND_MULTIPLE' if kind=='multiple' else 'android.intent.action.SEND'),
                    mime='application/zip' if kind=='zip' else 'application/gpx+xml',flags=0x10000000+(0 if negative else 1),files=files,clip=[] if negative else [f['uri'] for f in files],
                    contentContractPassed=True,expectedWorkoutIds=list(range(1,n+1)),expectedGpxSha256={str(i):str(i)*64 for i in range(1,n+1)})
    def test_all_three_real_intent_contract_shapes_require_positive_and_negative_receipts(self):
        for kind in ('single','multiple','zip'):
            for negative in (False,True):validate_receipt('tested',kind,negative,self.fixture(kind,negative))
    def test_sender_uid_or_forged_binder_uid_cannot_prove_cross_app_access(self):
        for field,value in [('uid',10152),('authenticatedBinderUid',10152),('pid',1000),('uid',0)]:
            r=self.fixture();r[field]=value
            with self.subTest(field=field),self.assertRaises(ValueError):validate_receipt('tested','multiple',False,r)
    def test_wrong_source_api_action_or_mime_fails_closed(self):
        for field,value in [('sourceCommit','other'),('api',34),('action','android.intent.action.SEND'),('mime','text/plain'),('permissionContractPassed',False)]:
            r=self.fixture();r[field]=value
            with self.subTest(field=field),self.assertRaises(ValueError):validate_receipt('tested','multiple',False,r)
    def test_missing_reordered_or_duplicate_uri_is_not_a_complete_share(self):
        for mode in ('missing','reordered','duplicate','private'):
            r=self.fixture()
            if mode=='missing':r['files'].pop()
            elif mode=='reordered':r['clip'].reverse()
            elif mode=='duplicate':r['files'][1]['uri']=r['files'][0]['uri']
            else:r['files'][0]['uri']='file:///data/private.gpx';r['clip'][0]=r['files'][0]['uri']
            with self.subTest(mode=mode),self.assertRaises(ValueError):validate_receipt('tested','multiple',False,r)
    def test_missing_read_or_extra_write_persistent_prefix_grants_are_rejected(self):
        for flags in (0,3,65,129):
            r=self.fixture();r['flags']=flags
            with self.subTest(flags=flags),self.assertRaises(ValueError):validate_receipt('tested','multiple',False,r)
    def test_negative_control_needs_actual_security_exception_and_no_implicit_grant(self):
        for mode in ('read','class','grant'):
            r=self.fixture(negative=True)
            if mode=='read':r['files'][0]['read']=True
            elif mode=='class':r['files'][0]['errorClass']='java.io.FileNotFoundException'
            else:r['flags']|=1
            with self.subTest(mode=mode),self.assertRaises(ValueError):validate_receipt('tested','multiple',True,r)
    def test_zip_cannot_omit_duplicate_or_alter_selected_gpx_or_raw_samples(self):
        for mode in ('missing','duplicate','hash','raw','expected','unproved'):
            r=self.fixture('zip');entries=r['files'][0]['entries']
            if mode=='missing':entries.pop()
            elif mode=='duplicate':entries[1]['workoutId']=entries[0]['workoutId']
            elif mode=='hash':entries[0]['sha256']='b'*64
            elif mode=='raw':entries[0]['rawSamples']=2
            elif mode=='expected':r['expectedWorkoutIds']=[1,2,4]
            else:r['contentContractPassed']=False
            with self.subTest(mode=mode),self.assertRaises(ValueError):validate_receipt('tested','zip',False,r)

if __name__=='__main__':unittest.main()
