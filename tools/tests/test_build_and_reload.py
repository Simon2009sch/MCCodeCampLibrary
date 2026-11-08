"""Offline tests for the project-local build/upload/reload helper."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('build_reload', ROOT / 'tools/build-and-reload.py')
helper = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(helper)

class BuildReloadTests(unittest.TestCase):
    def test_reload_skips_nonrunning_server(self):
        for state in ('offline', 'starting', 'stopping'):
            panel = Mock()
            panel.resources.return_value = {'state': state, 'uptime': 10}
            self.assertEqual(helper.reload_plugin(panel), 'skipped')
            panel.command.assert_not_called()

    def test_reload_verifies_new_success_not_old_console_lines(self):
        panel = Mock()
        panel.resources.return_value = {'state': 'running', 'uptime': 100}
        old = '[10:00:00 INFO]: [PlugMan] MCCodeCampLibraryTestPlugin has been reloaded.'
        new = '[10:00:01 INFO]: [PlugMan] MCCodeCampLibraryTestPlugin has been reloaded.'
        panel.logs.side_effect = [[old], [old], [old, new]]
        with patch.object(helper.time, 'sleep'):
            self.assertEqual(helper.reload_plugin(panel, timeout=2), 'reloaded')
        panel.command.assert_called_once_with('plugman reload MCCodeCampLibraryTestPlugin')

    def test_restart_before_command_skips_reload(self):
        panel = Mock()
        panel.resources.side_effect = [{'state':'running','uptime':100}, {'state':'starting','uptime':1}]
        panel.logs.return_value = []
        self.assertEqual(helper.reload_plugin(panel), 'skipped')
        panel.command.assert_not_called()

    def test_restart_after_command_does_not_retry(self):
        panel = Mock()
        panel.resources.side_effect = [{'state':'running','uptime':100}, {'state':'running','uptime':101}, {'state':'starting','uptime':1}]
        panel.logs.return_value = []
        with self.assertRaisesRegex(RuntimeError, 'restart'):
            helper.reload_plugin(panel)
        panel.command.assert_called_once()

    def test_new_lines_handles_console_tail_and_duplicates(self):
        self.assertEqual(helper.new_lines(['a','b'], ['a','b']), [])
        self.assertEqual(helper.new_lines(['a','b'], ['b','c']), ['c'])
        self.assertEqual(helper.new_lines(['a','b'], ['a','b','b']), ['b'])

    def test_config_rejects_short_uuid_without_repairing_it(self):
        env = {'CALAGOPUS_PANEL_URL':'https://panel.example', 'CALAGOPUS_API_TOKEN':'dummy-secret', 'CALAGOPUS_SERVER_ID':'798320a9'}
        with self.assertRaisesRegex(ValueError, 'full'):
            helper.deployment_environment(env, ROOT, None)

    def test_explicit_config_is_environment_only_and_remote_only(self):
        env = {'CALAGOPUS_PANEL_URL':'https://panel.example', 'CALAGOPUS_API_TOKEN':'dummy-secret', 'CALAGOPUS_SERVER_ID':'798320a9-6e00-40ff-9b37-6b9062531212', 'PLUGINSFOLDER':'do-not-copy'}
        loaded = helper.deployment_environment(env, ROOT, None)
        self.assertEqual(loaded['CALAGOPUS_API_TOKEN'], 'dummy-secret')
        self.assertNotIn('PLUGINSFOLDER', loaded)
        self.assertEqual(loaded['CALAGOPUS_DEPLOY_DRY_RUN'], 'false')

    def test_fingerprint_mismatch_fails(self):
        panel = Mock(); panel.fingerprint.return_value = '0'*64
        with tempfile.TemporaryDirectory(prefix='hermes-verify-build-reload-', dir='C:/Users/simon/AppData/Local/hermes/cache/scratch') as directory:
            jar = Path(directory)/'test.jar'; jar.write_bytes(b'test artifact')
            with self.assertRaisesRegex(RuntimeError, 'SHA-256'):
                helper.verify_artifact(panel, jar, '/plugins/test.jar')

if __name__ == '__main__':
    unittest.main()
