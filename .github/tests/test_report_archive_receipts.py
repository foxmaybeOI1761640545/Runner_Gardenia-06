"""Accepted evidence must exist in Git, not only beneath an ignored local build path."""
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
sys.path.insert(0,str(Path(__file__).parents[1]/'scripts'))
from verify_report_archive import verify_stage_receipt

class ReceiptTests(unittest.TestCase):
    def fixture(self, root):
        folder=root/'reports';folder.mkdir();p=folder/'native.xml';raw=b'<testsuite tests="1"/>';p.write_bytes(raw)
        receipt=folder/'stage4-verification.json';data={'status':'PASSED','files':[{'path':p.name,'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest()}]}
        receipt.write_text(json.dumps(data));return receipt,data,{'reports/native.xml'}
    def test_exact_tracked_original_is_accepted(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);p,_,tracked=self.fixture(root);verify_stage_receipt(p,root,tracked)
    def test_local_ignored_or_untracked_evidence_never_counts_as_archived(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);p,_,_=self.fixture(root)
            with self.assertRaisesRegex(ValueError,'untracked/ignored'):verify_stage_receipt(p,root,set())
    def test_missing_or_changed_bytes_fail_closed(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);p,_,tracked=self.fixture(root);item=p.parent/'native.xml';item.write_bytes(b'changed')
            with self.assertRaises(ValueError):verify_stage_receipt(p,root,tracked)
            item.unlink()
            with self.assertRaises(ValueError):verify_stage_receipt(p,root,tracked)
    def test_path_escape_is_rejected_even_if_its_bytes_and_git_identity_match(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);p,data,tracked=self.fixture(root);outside=root/'outside.xml';outside.write_bytes((p.parent/'native.xml').read_bytes())
            data['files'][0]['path']='../outside.xml';p.write_text(json.dumps(data))
            with self.assertRaises(ValueError):verify_stage_receipt(p,root,tracked|{'outside.xml'})
    def test_incomplete_or_unaccepted_receipt_cannot_claim_pass(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);p,data,tracked=self.fixture(root)
            for value in (dict(data,status='PENDING'),dict(data,files=[])):
                p.write_text(json.dumps(value))
                with self.assertRaises(ValueError):verify_stage_receipt(p,root,tracked)
