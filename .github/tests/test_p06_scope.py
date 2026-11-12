import importlib.util,json,sys,tempfile,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'scripts'))
import check_p06_source as scope
import p06_pipeline
import p06_offline

class MapScopeTest(unittest.TestCase):
    def test_map_and_audited_integration_are_allowed(self):
        scope.verify_changes([('A',scope.JAVA+'p0/map/P06MapActivity.java'),('M',scope.JAVA+'services/TrackRecordingService.java')])
    def test_ble_database_gpx_and_old_phase_workflows_stay_frozen(self):
        for path in [scope.JAVA+'sensors/HeartRateHandler.java',scope.JAVA+'data/TrackPointsColumns.java',scope.JAVA+'p0/export/P0GpxFiles.java','.github/workflows/p051-gps-motion-quality.yml']:
            with self.assertRaises(ValueError):scope.verify_changes([('M',path)])
    def test_deletion_and_empty_diff_fail_closed(self):
        for rows in [[],[('D',scope.JAVA+'p0/map/P06MapActivity.java')]]:
            with self.assertRaises(ValueError):scope.verify_changes(rows)
    def test_exact_p06_pr_route(self):
        env={'GITHUB_REPOSITORY':p06_pipeline.REPO,'GITHUB_EVENT_NAME':'pull_request','GITHUB_HEAD_REF':p06_pipeline.BRANCH,'GITHUB_BASE_REF':'feature/p0-5-1-gps-motion-quality'}
        p06_pipeline.trusted(env)
        with self.assertRaises(ValueError):p06_pipeline.trusted({**env,'GITHUB_BASE_REF':'main'})
    def test_final_stage_requires_every_regression(self):
        with self.assertRaises(ValueError):p06_pipeline.configuration({'stage':5,'fullRegression':False,'classes':['test.Map'],'signed':False})
    def test_phase_specific_offline_allowlist_keeps_original_private(self):
        original=p06_offline.original.COMPONENTS
        try:
            p06_offline.enable_map_activity()
            added=p06_offline.original.COMPONENTS['activity']-original['activity']
            self.assertEqual(added,{'de.dennisguse.opentracks.p0.map.P06MapActivity'})
            self.assertEqual(p06_offline.original.COMPONENTS['service'],original['service'])
        finally:p06_offline.original.COMPONENTS=original
    def test_map_runtime_keeps_original_dependencies_and_only_adds_pinned_engine(self):
        old="implementation 'existing:library:1.0'\n"
        added="".join("implementation '"+d+"'\n" for d in sorted(p06_offline.MAP_DEPENDENCIES))
        p06_offline.audit_runtime(old+added,old,{'Map.java':'new TileRendererLayer()'})
        for change in [added.replace('0.25.0','+'),added+"implementation 'second:map:1.0'\n",'']:
            with self.assertRaises(ValueError):p06_offline.audit_runtime(old+change,old,{'Map.java':'class Map {}'})
    def test_online_map_layer_or_client_cannot_pass_phase_offline_audit(self):
        build="".join("implementation '"+d+"'\n" for d in sorted(p06_offline.MAP_DEPENDENCIES))
        for source in ['new TileDownloadLayer()','import java.net.URL;','new WebView()']:
            with self.assertRaises(ValueError):p06_offline.audit_runtime(build,'',{'Map.java':source})

    def history_baseline(self):
        return '@Test void retained() { assertNotNull(record); '+ '; '.join([
            'onView(withId(android.R.id.button1))','onView(withId(android.R.id.button2))',
            'onView(withText(R.string.p05_direct_gpx))',
            'onView(withText(context.getString(R.string.p05_delete_confirm,3)))'])+'; }'
    def test_history_regression_only_allows_exact_dialog_root_patch(self):
        original=self.history_baseline();scope.verify_changes([('M',scope.HISTORY_TEST)])
        scope.verify_history_test(original,scope.focused_history_test(original))
    def test_history_assertions_and_test_methods_cannot_be_removed(self):
        original=self.history_baseline();patched=scope.focused_history_test(original)
        for changed in [patched.replace('assertNotNull(record);',''),patched.replace('@Test',''),patched+' unrelated();']:
            with self.assertRaises(ValueError):scope.verify_history_test(original,changed)
    def test_missing_original_history_selector_cannot_be_accepted(self):
        with self.assertRaises(ValueError):scope.focused_history_test(self.history_baseline().replace('onView(withId(android.R.id.button1))',''))
