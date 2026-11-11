"""Editor integration source-contract checks, separate from Java behavior harnesses."""
from pathlib import Path
import unittest

src = Path(__file__).resolve().parents[1] / 'src/main/java/me/simoncrafter/mCCodeCampLibrary'
class DoorEditorWiring(unittest.TestCase):
    def test_reusable_editor_has_private_cyan_inclusive_outlines_and_explicit_save(self):
        path = src / 'internal/editor/area/CuboidAreaEditor.java'
        self.assertTrue(path.exists(), 'reusable multi-area editor missing')
        text = path.read_text()
        for contract in ['Color.fromARGB(100, 0, 255, 255)', 'CuboidAreaSelection.center(area)',
                         'CuboidAreaSelection.size(area)', 'hideByDefault(true', 'showForPlayer(player, true)',
                         'protected void leave(Player player)', '"Save"', 'getClickedBlock()', 'getWorld().equals(world)',
                         'LocationInputAction.create', 'getEyeLocation()', 'selectCornerAt(']:
            self.assertIn(contract, text)
    def test_door_uses_movement_dialog_and_scroll(self):
        path = src / 'input/editor/DoorEditor.java'
        self.assertTrue(path.exists(), 'door editor missing')
        text = path.read_text()
        for contract in ['extends CuboidAreaEditor', 'new PivotEditor(', 'getPivotPosition()', 'setPivotPosition(']:
            self.assertIn(contract, text)
        pivot = src / 'internal/editor/area/PivotEditor.java'
        self.assertTrue(pivot.exists(), 'reusable pivot editor missing')
        pivot_text = pivot.read_text()
        for contract in ['MovementQuestion.create()', 'addScrollAction', 'setHeldItemSlotQuietly', 'isSneaking()', 'Consumer<Vector>']:
            self.assertIn(contract, pivot_text)
        self.assertNotIn('AOpenableObject', pivot_text)
        base = (src / 'obstical/AOpenableObject.java').read_text()
        self.assertIn('new DoorEditor(', base)
        self.assertIn('registerEditor(', base)
        self.assertIn('manager.put(player,', base)
    def test_animation_uses_absolute_selected_blocks(self):
        text = (src / 'obstical/AAnimatedOpenableObject.java').read_text()
        self.assertNotIn('getLocation().clone().add(v)', text)
        self.assertIn('public Vector getPivotPosition()', text)
        self.assertIn('public void setPivotPosition(Vector', text)

if __name__ == '__main__': unittest.main()
