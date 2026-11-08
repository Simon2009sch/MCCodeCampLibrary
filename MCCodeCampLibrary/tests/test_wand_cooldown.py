"""Focused source-level wand wiring regression checks (not an in-game event test)."""
from pathlib import Path
import unittest

root = Path(__file__).resolve().parents[2]
wand = root / 'MCCodeCampLibraryTestPlugin/src/main/java/me/simoncrafter/mCCodeCampLibraryTestPlugin/yourCode/a3_wand/Course.java'

class WandCooldownWiring(unittest.TestCase):
    def test_gate_before_both_explosions(self):
        text = wand.read_text()
        gate = text.index('itemCooldown.tryUse(event.getItem(),')
        self.assertLess(text.index('event.getHand() == EquipmentSlot.HAND'), gate)
        self.assertLess(gate, text.index('isSneaking()'))
        self.assertIn('getItemData(event.getItem()).node("cooldown").getLong()', text)
        self.assertNotIn('getLastUse', text)
        self.assertEqual(text.count('createExplosion('), 2)

if __name__ == '__main__':
    unittest.main()
