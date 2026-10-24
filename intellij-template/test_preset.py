import json, unittest
from pathlib import Path
ROOT = Path(__file__).resolve().parent
class PresetTests(unittest.TestCase):
    def test_only_paper_controls(self):
        d=json.loads((ROOT/'templates/mccodecamp/mccodecamp.mcdev.template.json').read_text())
        visible=[]
        for p in d['properties']:
            for q in p.get('groupProperties',[p]):
                if q.get('visible') is not False: visible.append(q['name'])
        self.assertEqual(set(visible),set('MC_VERSION PLUGIN_NAME MAIN_CLASS USE_PAPER_MANIFEST INCLUDE_PLUGIN_LOADER DESCRIPTION AUTHORS WEBSITE LOG_PREFIX LOAD_AT'.split()))
        props={p['name']:p for p in d['properties'] if 'name' in p}
        self.assertEqual(props['MC_VERSION']['type'],'paper_versions')
        self.assertEqual(props['JAVA_VERSION']['derives']['method'],'recommendJavaVersionForMcVersion')
        self.assertEqual(props['DEPENDENCY_VERSION']['derives']['method'],'fetchPaperDependencyVersionForMcVersion')
if __name__=='__main__': unittest.main()
