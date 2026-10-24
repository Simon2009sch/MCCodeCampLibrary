"""Validate canonical templates; never regenerate from a personal project.
Edit templates/mccodecamp directly. Retains the old automation entry point safely.
"""
from pathlib import Path
import json
import unittest
from test_preset import PresetTests
ROOT = Path(__file__).resolve().parent
if __name__ == '__main__':
    result = unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(PresetTests))
    if not result.wasSuccessful():
        raise SystemExit(1)
    d = json.loads((ROOT/'templates/mccodecamp/mccodecamp.mcdev.template.json').read_text())
    for f in d['files']:
        assert (ROOT/'templates/mccodecamp'/f['template']).is_file(), f
    print('Validated canonical template:', ROOT/'templates/mccodecamp')
