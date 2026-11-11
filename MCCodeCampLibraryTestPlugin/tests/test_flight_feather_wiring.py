"""Course integration contract; movement and display behavior have Java harnesses."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1] / 'src/main/java/me/simoncrafter/mCCodeCampLibraryTestPlugin'

class FlightFeatherWiring(unittest.TestCase):
    def test_course_has_button_sneak_and_tick_routes(self):
        text = (ROOT / 'courseInternal/a4_flightFeather/FlightFeatherCourseSegment.java').read_text()
        for route in ['"get_flight_feather"', 'student.onSneak(event)', 'wind.tick()', 'registerCustomItem(student)', 'unregisterCustomItem(student)', 'wind.close()', 'student.close()']:
            self.assertIn(route, text)

    def test_plugin_registers_and_unregisters_segment(self):
        text = (ROOT / 'MCCodeCampLibraryTestPlugin.java').read_text()
        self.assertIn('registerSegment(flightFeather)', text)
        self.assertIn('unregisterSegment(flightFeather)', text)

if __name__ == '__main__':
    unittest.main()
