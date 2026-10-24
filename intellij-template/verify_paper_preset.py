"""Real installed Velocity/plugin API rendering and deployment-free Maven builds."""
from pathlib import Path
import os, subprocess, json, zipfile, urllib.request, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parent
SCRATCH=Path('C:/Users/simon/AppData/Local/hermes/cache/scratch')/'mccodecamp-preset-verification'
JAVA=Path(os.environ.get('PRESET_JAVA_HOME','C:/Users/simon/.jdks/openjdk-26.0.1'))
IDE=Path(os.environ.get('PRESET_IDEA_HOME','C:/Program Files/JetBrains/IntelliJ IDEA 2026.1.4'))
PLUGIN=Path(os.environ.get('PRESET_MCDEV_LIB','C:/Users/simon/AppData/Roaming/JetBrains/IntelliJIdea2026.1/plugins/Minecraft Development/lib'))
CP=';'.join(str(p) for p in [IDE/'lib/*',IDE/'plugins/java/lib/*',PLUGIN/'*'])
def run(cmd,cwd,log,env):
    result=subprocess.run(cmd,cwd=cwd,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace')
    log.write_text(result.stdout,encoding='utf-8')
    print(log, 'exit',result.returncode)
    if result.returncode: print(result.stdout[-6000:])
    assert result.returncode==0
    return result.stdout
if __name__=='__main__':
    SCRATCH.mkdir(parents=True,exist_ok=True)
    env={k:v for k,v in os.environ.items() if not k.upper().startswith(('PELICAN_','CALAGOPUS_')) and k.upper()!='PLUGINSFOLDER'}
    env['JAVA_HOME']=str(JAVA)
    run(['python',str(ROOT/'test_preset.py')],ROOT,SCRATCH/'descriptor.log',env)
    import hashlib
    before={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'templates').rglob('*') if p.is_file()}
    run(['python',str(ROOT/'create_template.py')],ROOT,SCRATCH/'generator.log',env)
    after={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'templates').rglob('*') if p.is_file()}
    assert before==after, 'Generator must never overwrite canonical templates'
    req=urllib.request.Request('https://fill.papermc.io/v3/projects/paper',headers={'User-Agent':'MCCodeCampTemplateVerification/1.0 (local template test)'})
    data=json.load(urllib.request.urlopen(req,timeout=30))
    (SCRATCH/'paper-selector-response.json').write_text(json.dumps(data,indent=2))
    versions=[v for vs in data['versions'].values() for v in vs]
    assert '1.21.11' in versions
    print('Live paper_versions upstream endpoint:',len(versions),'versions; 1.21.11 present')
    summary=[]
    for paper in [False,True]:
        name='paper-loader' if paper else 'bukkit'
        out=SCRATCH/name
        text=run([str(JAVA/'bin/java'),'-cp',CP,str(ROOT/'VerifyPaperPreset.java'),str(ROOT/'templates/mccodecamp'),str(out),str(paper).lower(),str(paper).lower()],ROOT,SCRATCH/(name+'-render.log'),env)
        assert 'Java=21 dependency=1.21.11-R0.1-SNAPSHOT' in text
        resources=out/'src/main/resources'; manifest='paper-plugin.yml' if paper else 'plugin.yml'
        import ast
        ast.parse((out/'deploy.py').read_text(encoding='utf-8'))
        contents=(resources/manifest).read_text()
        assert not (resources/('plugin.yml' if paper else 'paper-plugin.yml')).exists()
        for field in ['description:','authors:','website:','prefix:']:
            assert (field in contents)==paper,(field,contents)
        if paper:
            assert "description: 'A plugin: Simon''s test'" in contents
            assert "authors: ['Simon', 'Alex: Dev', 'O''Brien']" in contents
            assert 'load: STARTUP' in contents and 'loader: me.simoncrafter.templateprobe.TemplateProbeLoader' in contents
        else: assert 'load: POSTWORLD' in contents and 'loader:' not in contents
        ns={'m':'http://maven.apache.org/POM/4.0.0'}
        pom=ET.parse(out/'pom.xml')
        assert {p.text for p in pom.findall('m:profiles/m:profile/m:id',ns)}=={'deploy-local-jar','deploy-remote-python-windows','deploy-remote-python-unix','mccodecamp-library'}
        build=run([str(IDE/'plugins/maven/lib/maven3/bin/mvn.cmd'),'-B','-Dstyle.color=never','-Dexec.skip=true','clean','verify'],out,SCRATCH/(name+'-build.log'),env)
        assert 'BUILD SUCCESS' in build and 'shade:3.5.3:shade' in build
        jar=out/'target/template-probe-1.0-SNAPSHOT.jar'
        with zipfile.ZipFile(jar) as z:
            assert 'me/simoncrafter/templateprobe/TemplateProbe.class' in z.namelist()
            assert ('me/simoncrafter/templateprobe/TemplateProbeLoader.class' in z.namelist())==paper
            packaged=z.read(manifest).decode()
            assert "version: '1.0-SNAPSHOT'" in packaged
            assert '${' not in packaged
        summary.append({'variant':name,'jar':str(jar),'bytes':jar.stat().st_size,'build':'BUILD SUCCESS','manifest':packaged})
    for name,mc,paper,loader,expected_java in [('legacy-java17','1.18.2',False,False,17),('bukkit-loader','1.21.11',False,True,21),('paper-no-loader','1.21.11',True,False,21)]:
        out=SCRATCH/name
        text=run([str(JAVA/'bin/java'),'-cp',CP,str(ROOT/'VerifyPaperPreset.java'),str(ROOT/'templates/mccodecamp'),str(out),str(paper).lower(),'false',mc,str(loader).lower()],ROOT,SCRATCH/(name+'-render.log'),env)
        assert f'Java={expected_java}' in text
        manifest=(out/'src/main/resources'/('paper-plugin.yml' if paper else 'plugin.yml')).read_text()
        assert ('plugin-loader:' in manifest)==(loader and not paper)
        assert ('loader:' in manifest)==loader
        assert (out/'src/main/java/me/simoncrafter/templateprobe/TemplateProbeLoader.java').exists()==loader
        if mc=='1.18.2':
            assert "api-version: '1.18'" in manifest
            assert '<version>1.18.2-R0.1-SNAPSHOT</version>' in (out/'pom.xml').read_text()
    (SCRATCH/'results.json').write_text(json.dumps(summary,indent=2))
    print(json.dumps(summary,indent=2))
