"""Fail closed on runtime network/backup capability or non-P0 Release entry points."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET

A='{http://schemas.android.com/apk/res/android}'
PREFIX='de.dennisguse.opentracks.'
LAUNCHER=PREFIX+'introduction.IntroductionActivity'
COMPONENTS={
    'activity':{LAUNCHER,PREFIX+'p0.app.P0Activity',PREFIX+'p0.app.P0SensorsActivity',PREFIX+'p0.app.P0HistoryActivity',PREFIX+'p0.app.P0SettingsActivity'},
    'service':{PREFIX+'services.TrackRecordingService','androidx.appcompat.app.AppLocalesMetadataHolderService'},
    'provider':{PREFIX+'data.ShareContentProvider','androidx.core.content.FileProvider'},
}
PERMISSIONS={
    'FOREGROUND_SERVICE','FOREGROUND_SERVICE_LOCATION','FOREGROUND_SERVICE_CONNECTED_DEVICE',
    'ACCESS_FINE_LOCATION','ACCESS_COARSE_LOCATION','WAKE_LOCK','VIBRATE','POST_NOTIFICATIONS',
    'BLUETOOTH','BLUETOOTH_ADMIN','BLUETOOTH_SCAN','BLUETOOTH_CONNECT',
}


def inspect(root,release):
    identity='com.fox.runner' if release else 'com.fox.runner.debug'
    if root.get('package')!=identity:raise ValueError('Offline audit application identity changed')
    permissions={node.get(A+'name') for tag in ('uses-permission','uses-permission-sdk-23') for node in root.findall(tag)}
    if 'android.permission.INTERNET' in permissions:raise ValueError('Runtime INTERNET permission is forbidden')
    app=root.find('application')
    if app is None or app.get(A+'allowBackup')!='false':raise ValueError('System backup must be disabled')
    components={}
    if release:
        allowed={'android.permission.'+value for value in PERMISSIONS}|{identity+'.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'}
        if permissions-allowed:raise ValueError('Unexpected Release permissions: '+str(sorted(permissions-allowed)))
        for tag in ('activity','activity-alias','service','provider','receiver'):
            nodes=app.findall(tag);names={node.get(A+'name') for node in nodes}
            if names!=COMPONENTS.get(tag,set()):raise ValueError('Unexpected or missing Release '+tag+': '+str(names))
            for node in nodes:
                exported=node.get(A+'exported','false')=='true'
                if exported!=(node.get(A+'name')==LAUNCHER):raise ValueError('Unexpected exported Release component')
            components[tag]=sorted(names)
        authorities={node.get(A+'authorities') for node in app.findall('provider')}
        if authorities!={identity+'.content',identity+'.fileprovider'}:raise ValueError('Provider authority identity changed')
        if root.find('queries') is not None and list(root.find('queries')):raise ValueError('P0 Release must not discover health/TTS integrations')
    return {'applicationId':identity,'permissions':sorted(permissions),'allowBackup':False,'components':components}


def verify():
    analyzer=shutil.which('apkanalyzer')
    if not analyzer:raise ValueError('Installed Android APK analyzer required')
    folder=Path('build/outputs/apk/reproducible/debug');metadata=json.loads((folder/'output-metadata.json').read_text())
    if len(metadata['elements'])!=1:raise ValueError('Ambiguous Debug APK')
    apk=(folder/metadata['elements'][0]['outputFile']).resolve()
    if apk.parent!=folder.resolve() or not apk.is_file():raise ValueError('Missing actual Debug APK')
    debugXml=subprocess.check_output([analyzer,'manifest','print',str(apk)],text=True)
    debug=inspect(ET.fromstring(debugXml),False)
    manifests=list(Path('build/intermediates/merged_manifests/reproducibleRelease').glob('**/AndroidManifest.xml'))
    if len(manifests)!=1:raise ValueError('Missing or ambiguous actual merged Release manifest: '+str(manifests))
    releaseXml=manifests[0].read_text();release=inspect(ET.fromstring(releaseXml),True)
    output=Path('build/reports/p0-offline');output.mkdir(parents=True,exist_ok=True)
    (output/'debug-manifest.xml').write_text(debugXml);(output/'release-manifest.xml').write_text(releaseXml)
    proof={'passed':True,'commit':os.environ['GITHUB_SHA'],'debug':debug,'release':release}
    (output/'result.json').write_text(json.dumps(proof,indent=2)+'\n');print(json.dumps(proof,indent=2))


if __name__=='__main__':verify()
