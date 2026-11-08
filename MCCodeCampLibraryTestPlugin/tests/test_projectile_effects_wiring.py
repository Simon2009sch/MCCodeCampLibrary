"""Source-level lifecycle/command wiring checks; not a live Paper integration test."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/me/simoncrafter/mCCodeCampLibraryTestPlugin'

class ProjectileEffectsWiring(unittest.TestCase):
    def test_console_only_command(self):
        source = (JAVA / 'testing/ProjectileEffectsCommand.java').read_text()
        self.assertIn('sender instanceof ConsoleCommandSender', source)
        gate = source.index('sender instanceof ConsoleCommandSender')
        for action in ['effects.liveMeteor()', 'effects.liveRay()', 'effects.restore(null)', 'effects.disarmLiveTest()']:
            self.assertGreater(source.index(action), gate)
        self.assertNotIn('getLocation()', source)

    def test_single_coordinator_lifecycle(self):
        source = (JAVA / 'MCCodeCampLibraryTestPlugin.java').read_text()
        self.assertEqual(source.count('new ProjectileEffects(this)'), 1)
        self.assertIn('new ProjectileTestSupport(this, MCCodeCampLib.getItemsManager(), projectileEffects)', source)
        self.assertLess(source.index('projectileEffects.close()'), source.index('MCCodeCampLib.onDisable()'))
        manifest = (ROOT / 'src/main/resources/plugin.yml').read_text()
        self.assertIn('FastAsyncWorldEdit', manifest)
        self.assertIn('projectileeffects:', manifest)

if __name__ == '__main__':
    unittest.main()
