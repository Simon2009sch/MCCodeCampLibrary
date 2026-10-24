"""Credential-free deployment wire-protocol tests; never contacts a real panel."""
import contextlib
import importlib.util
import io
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(os.environ.get('DEPLOY_SCRIPT_UNDER_TEST', str(Path(__file__).resolve().parents[1] / 'deploy-local.py')))
spec = importlib.util.spec_from_file_location('deploy_under_test', SCRIPT)
deploy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deploy)


class DeploymentTests(unittest.TestCase):
    def test_calagopus_dry_run_uses_new_environment_and_no_network_or_copy(self):
        with tempfile.TemporaryDirectory(prefix='hermes-verify-', dir='C:/Users/simon/AppData/Local/hermes/cache/scratch' if os.name == 'nt' else None) as directory:
            root=Path(directory)
            jar=root/'plugin.jar'; jar.write_bytes(b'PK\x03\x04binary\x00\xff')
            env={'CALAGOPUS_PANEL_URL':'https://panel.invalid','CALAGOPUS_API_TOKEN':'dummy-secret','CALAGOPUS_SERVER_ID':'123e4567-e89b-12d3-a456-426614174000','CALAGOPUS_DEPLOY_DRY_RUN':'true','PLUGINSFOLDER':str(root/'plugins')}
            output=io.StringIO()
            with patch.dict(os.environ,env,clear=True), patch.object(sys,'argv',['deploy-local.py',str(jar)]), contextlib.redirect_stdout(output):
                result=deploy.main()
            self.assertEqual(result,0)
            self.assertIn('Dry run',output.getvalue())
            self.assertNotIn('dummy-secret',output.getvalue())
            self.assertFalse((root/'plugins').exists(),'Dry run must not copy locally either')

    def test_signed_multipart_upload_and_remote_sha256_verification(self):
        import hashlib, json
        from email.parser import BytesParser
        from email.policy import default
        from urllib.parse import urlsplit, parse_qs
        payload=b'PK\x03\x04binary\x00\xff'
        requests=[]
        class Panel:
            def open(self, request, timeout):
                requests.append(request)
                path=urlsplit(request.full_url).path
                if path.endswith('/files/fingerprint'):
                    digest=('0'*64 if len(requests)==1 else hashlib.sha256(payload).hexdigest())
                    return io.BytesIO(json.dumps({'fingerprint':digest}).encode())
                if path.endswith('/files/upload'):
                    return io.BytesIO(b'{"url":"https://node.invalid/upload/file?token=signed-secret"}')
                if path=='/upload/file':
                    self_outer.assertNotIn('Authorization',request.headers)
                    self_outer.assertEqual(parse_qs(urlsplit(request.full_url).query)['directory'],['/plugins'])
                    msg=BytesParser(policy=default).parsebytes(('Content-Type: '+request.get_header('Content-type')+'\r\n\r\n').encode()+request.data)
                    part=list(msg.iter_parts())[0]
                    self_outer.assertEqual(part.get_filename(),'Custom Plugin.jar')
                    self_outer.assertEqual(part.get_param('name',header='content-disposition'),'files')
                    self_outer.assertEqual(part.get_payload(decode=True),payload)
                    return io.BytesIO(b'{}')
                raise AssertionError('Unexpected endpoint '+path)
        self_outer=self
        with tempfile.TemporaryDirectory(prefix='hermes-verify-',dir='C:/Users/simon/AppData/Local/hermes/cache/scratch' if os.name=='nt' else None) as directory:
            artifact=Path(directory)/'plugin.jar'; artifact.write_bytes(payload)
            env={'CALAGOPUS_PANEL_URL':'https://panel.invalid','CALAGOPUS_API_TOKEN':'dummy-secret','CALAGOPUS_SERVER_ID':'123e4567-e89b-12d3-a456-426614174000','CALAGOPUS_PLUGIN_PATH':'/plugins/Custom Plugin.jar'}
            output=io.StringIO()
            with patch.dict(os.environ,env,clear=True),patch.object(sys,'argv',['deploy-local.py',str(artifact)]),patch.object(deploy,'build_opener',return_value=Panel(),create=True),contextlib.redirect_stdout(output):
                self.assertEqual(deploy.main(),0)
            self.assertEqual([r.get_method() for r in requests],['GET','GET','POST','GET'])
            self.assertIn('verified',output.getvalue().lower())
            self.assertNotIn('dummy-secret',output.getvalue())
            self.assertNotIn('signed-secret',output.getvalue())

    def test_invalid_config_never_attempts_network_or_local_copy(self):
        baseline={'CALAGOPUS_PANEL_URL':'https://panel.invalid','CALAGOPUS_API_TOKEN':'dummy-secret','CALAGOPUS_SERVER_ID':'123e4567-e89b-12d3-a456-426614174000'}
        cases=[{'CALAGOPUS_PANEL_URL':'http://panel.invalid'}, {'CALAGOPUS_SERVER_ID':'798320a9'}, {'CALAGOPUS_PLUGIN_PATH':'/plugins/../evil.jar'}, {'CALAGOPUS_API_TOKEN':''}, {'CALAGOPUS_PLUGIN_PATH':'/plugins/evil\r\n.jar'}]
        with tempfile.TemporaryDirectory(prefix='hermes-verify-',dir='C:/Users/simon/AppData/Local/hermes/cache/scratch' if os.name=='nt' else None) as directory:
            artifact=Path(directory)/'plugin.jar'; artifact.write_bytes(b'PK-test')
            for override in cases:
                with self.subTest(override=override),patch.dict(os.environ,baseline|override|{'PLUGINSFOLDER':directory+'/plugins'},clear=True),patch.object(sys,'argv',['deploy-local.py',str(artifact)]),patch.object(deploy,'build_opener',side_effect=AssertionError('Network attempted')),contextlib.redirect_stderr(io.StringIO()):
                    self.assertEqual(deploy.main(),1)
                self.assertFalse((Path(directory)/'plugins').exists())

    def test_remote_mismatch_is_failure_and_never_logs_tokens(self):
        from urllib.error import HTTPError, URLError
        import json
        with tempfile.TemporaryDirectory(prefix='hermes-verify-',dir='C:/Users/simon/AppData/Local/hermes/cache/scratch' if os.name=='nt' else None) as directory:
            artifact=Path(directory)/'plugin.jar'; artifact.write_bytes(b'PK-test')
            env={'CALAGOPUS_PANEL_URL':'https://panel.invalid','CALAGOPUS_API_TOKEN':'dummy-secret','CALAGOPUS_SERVER_ID':'123e4567-e89b-12d3-a456-426614174000'}
            for failure in ['mismatch','http','network','permission','new-file','insecure-node']:
                requests=[]
                class Panel:
                    def open(self,request,timeout):
                        requests.append(request)
                        if len(requests)==1:
                            if failure=='permission': raise HTTPError(request.full_url,403,'denied',{},io.BytesIO(b'dummy-secret'))
                            if failure=='new-file': raise HTTPError(request.full_url,404,'absent',{},io.BytesIO())
                            return io.BytesIO(b'{"fingerprint":"old"}')
                        if len(requests)==2:
                            scheme='http' if failure=='insecure-node' else 'https'
                            return io.BytesIO(json.dumps({'url':scheme+'://node.invalid/upload/file?token=signed-secret'}).encode())
                        if len(requests)==3:
                            if failure=='http': raise HTTPError(request.full_url,500,'signed-secret',{},io.BytesIO(b'dummy-secret'))
                            if failure=='network': raise URLError('https://node.invalid/?token=signed-secret')
                            return io.BytesIO(b'{}')
                        return io.BytesIO(b'{"fingerprint":"mismatch"}')
                output=io.StringIO()
                with self.subTest(failure=failure),patch.dict(os.environ,env,clear=True),patch.object(sys,'argv',['deploy-local.py',str(artifact)]),patch.object(deploy,'build_opener',return_value=Panel()),contextlib.redirect_stdout(output),contextlib.redirect_stderr(output):
                    self.assertEqual(deploy.main(),1)
                self.assertNotIn('signed-secret',output.getvalue())
                self.assertNotIn('dummy-secret',output.getvalue())
                if failure=='permission': self.assertEqual(len(requests),1)
                if failure=='insecure-node': self.assertEqual(len(requests),2)
                if failure=='new-file': self.assertEqual(len(requests),4)

    def test_legacy_environment_aliases_and_new_names_take_precedence(self):
        with patch.dict(os.environ,{'PELICAN_API_TOKEN':'old'},clear=True):
            self.assertEqual(deploy.setting('API_TOKEN'),'old')
        with patch.dict(os.environ,{'PELICAN_API_TOKEN':'old','CALAGOPUS_API_TOKEN':'new'},clear=True):
            self.assertEqual(deploy.setting('API_TOKEN'),'new')
        with patch.dict(os.environ,{'PELICAN_API_TOKEN':'old','CALAGOPUS_API_TOKEN':''},clear=True):
            self.assertEqual(deploy.setting('API_TOKEN'),'')
        self.assertIsNone(deploy.NoRedirect().redirect_request(None,None,302,'',{},'https://other.invalid'))

    def test_local_only_copy_and_no_configuration(self):
        with tempfile.TemporaryDirectory(prefix='hermes-verify-',dir='C:/Users/simon/AppData/Local/hermes/cache/scratch' if os.name=='nt' else None) as directory:
            artifact=Path(directory)/'plugin.jar'; artifact.write_bytes(b'PK-binary\x00\xff')
            for env in ({},{'PLUGINSFOLDER':directory+'/plugins'}):
                with patch.dict(os.environ,env,clear=True),patch.object(sys,'argv',['deploy-local.py',str(artifact)]),patch.object(deploy,'build_opener',side_effect=AssertionError('Network attempted')),contextlib.redirect_stdout(io.StringIO()):
                    self.assertEqual(deploy.main(),0)
            self.assertEqual((Path(directory)/'plugins/plugin.jar').read_bytes(),artifact.read_bytes())


if __name__=='__main__':
    unittest.main()
