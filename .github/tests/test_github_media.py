import io
import os
from pathlib import Path
import sys
import unittest
from unittest.mock import patch,Mock
sys.path.insert(0,str(Path(__file__).parents[1]/'scripts'))
from publish_release import GitHub

class MediaTests(unittest.TestCase):
    def request(self,**args):
        with patch.dict(os.environ,{'GH_TOKEN':'unit-only-placeholder','GITHUB_REPOSITORY':'unit/fixture'}):
            api=GitHub();api.opener=Mock();api.opener.open.return_value=io.BytesIO(b'actual-style job log')
            raw=api.request(api.base+'/actions/jobs/1/logs',binary=True,**args)
            self.assertEqual(b'actual-style job log',raw)
            return api.opener.open.call_args.args[0].get_header('Accept')
    def test_job_logs_keep_api_media_type_while_returning_raw_bytes(self):
        self.assertEqual('application/vnd.github+json',self.request(api_binary=True))
    def test_release_asset_download_still_uses_octet_stream(self):
        self.assertEqual('application/octet-stream',self.request())
