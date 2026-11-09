import copy
import hashlib
import json
from pathlib import Path
import struct
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from collect_p05_ui import collect, FAMILIES, VIEWPORTS


class P05VisualGateTest(unittest.TestCase):
    def fixture(self, root):
        # Host gate unit fixture, never Android screenshot acceptance evidence.
        rows = []
        for family in FAMILIES:
            for appearance in ('light', 'dark'):
                for w, h in VIEWPORTS:
                    for scale in (1.0, 1.3):
                        name = f'{family}-{appearance}-{w}-{scale}.png'
                        raw = b'\x89PNG\r\n\x1a\n' + b'\x00\x00\x00\rIHDR' + struct.pack('>II', w * 2, h * 2)
                        (root / name).write_bytes(raw)
                        rows.append(dict(file=name, family=family, appearance=appearance,
                                         widthDp=w, heightDp=h, fontScale=scale,
                                         sha256=hashlib.sha256(raw).hexdigest(), syntheticUiFixture=True))
        proof = dict(passed=True, sourceCommit='tested-source', api=35, native=True, productionFixtureEntry=False,
                     policy='FOLLOW_SYSTEM', screenshots=rows)
        (root / 'p05-layout-matrix.json').write_text(json.dumps(proof))
        return proof

    def test_complete_exact_source_matrix_and_bytes_required(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / 'input'; root.mkdir()
            output = Path(directory) / 'output'
            proof = self.fixture(root)
            self.assertEqual(48, len(collect('tested-source', root, output)['screenshots']))
            for mutation in ('missing', 'duplicate', 'wrong-font', 'wrong-source', 'wrong-api', 'not-native'):
                changed = copy.deepcopy(proof)
                if mutation == 'missing': changed['screenshots'].pop()
                if mutation == 'duplicate': changed['screenshots'][-1] = changed['screenshots'][0]
                if mutation == 'wrong-font': changed['screenshots'][0]['fontScale'] = 1.1
                if mutation == 'wrong-source': changed['sourceCommit'] = 'other'
                if mutation == 'wrong-api': changed['api'] = 34
                if mutation == 'not-native': changed['native'] = False
                (root / 'p05-layout-matrix.json').write_text(json.dumps(changed))
                with self.subTest(mutation=mutation), self.assertRaises(ValueError):
                    collect('tested-source', root, output)
            (root / 'p05-layout-matrix.json').write_text(json.dumps(proof))
            (root / proof['screenshots'][0]['file']).write_bytes(b'broken')
            with self.assertRaises(ValueError): collect('tested-source', root, output)

    def test_wrong_viewport_even_with_matching_hash_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            proof = self.fixture(root)
            row = proof['screenshots'][0]
            raw = b'\x89PNG\r\n\x1a\n' + b'\x00\x00\x00\rIHDR' + struct.pack('>II', 10, 10)
            (root / row['file']).write_bytes(raw)
            row['sha256'] = hashlib.sha256(raw).hexdigest()
            (root / 'p05-layout-matrix.json').write_text(json.dumps(proof))
            with self.assertRaises(ValueError): collect('tested-source', root, root / 'out')
