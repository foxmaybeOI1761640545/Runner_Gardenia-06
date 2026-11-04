import sys
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
sys.path.insert(0,str(Path(__file__).parents[1]/'scripts'))
import verify_p0_offline as audit


class OfflineGateTests(unittest.TestCase):
    def fixture(self,release=True):
        root=ET.Element('manifest',{'package':'com.fox.runner' if release else 'com.fox.runner.debug'})
        app=ET.SubElement(root,'application',{audit.A+'allowBackup':'false'})
        for tag,names in audit.COMPONENTS.items():
            for name in names:
                attrs={audit.A+'name':name,audit.A+'exported':str(name==audit.LAUNCHER).lower()}
                if tag=='provider':attrs[audit.A+'authorities']='com.fox.runner.'+('fileprovider' if name.endswith('FileProvider') else 'content')
                ET.SubElement(app,tag,attrs)
        return root
    def test_minimal_release_manifest_is_accepted(self):
        self.assertEqual(audit.inspect(self.fixture(),True)['applicationId'],'com.fox.runner')
    def test_internet_is_rejected_in_debug_as_well_as_release(self):
        for release in (True,False):
            root=self.fixture(release);ET.SubElement(root,'uses-permission',{audit.A+'name':'android.permission.INTERNET'})
            with self.assertRaises(ValueError):audit.inspect(root,release)
    def test_extra_permission_or_external_entry_point_blocks_release(self):
        root=self.fixture();ET.SubElement(root,'uses-permission',{audit.A+'name':'android.permission.health.WRITE_EXERCISE'})
        with self.assertRaises(ValueError):audit.inspect(root,True)
        root=self.fixture();ET.SubElement(root.find('application'),'receiver',{audit.A+'name':'Unexpected'})
        with self.assertRaises(ValueError):audit.inspect(root,True)
    def test_cloud_backup_or_exported_private_activity_blocks_release(self):
        root=self.fixture();root.find('application').set(audit.A+'allowBackup','true')
        with self.assertRaises(ValueError):audit.inspect(root,True)
        root=self.fixture();private=next(node for node in root.find('application').findall('activity') if node.get(audit.A+'name')!=audit.LAUNCHER);private.set(audit.A+'exported','true')
        with self.assertRaises(ValueError):audit.inspect(root,True)
