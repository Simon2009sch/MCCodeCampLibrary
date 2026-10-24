# MCCodeCamp IntelliJ-Template

Lokales Repository für Minecraft Development (Format 3).

In IntelliJ: Settings > Languages & Frameworks > Minecraft Development > Creator/Template Repositories: Repository `MCCodeCamp`, Provider `local`, Pfad `C:/Users/simon/hermes-tools/mccodecamp-template/templates` hinzufügen. Die Registrierung ist außerdem in `options/minecraft_dev.xml` vorbereitet und wird beim nächsten IntelliJ-Start geladen.

Danach File > New > Project > Minecraft > Repository MCCodeCamp > MCCodeCamp - Paper + Calagopus Upload.

Für eine Installation aus dem Git-Repository denselben Local-Provider auf den absoluten Ordner `<checkout>/intellij-template/templates` zeigen lassen. Die aktive Installation auf Simons Rechner bleibt unter `hermes-tools/mccodecamp-template/templates`.

Verifikation: `python create_template.py` prüft die Struktur. `python verify_paper_preset.py` rendert mit den installierten IDE/Plugin-APIs und baut beide Manifest-Varianten. Die Windows-Pfade können mit `PRESET_JAVA_HOME`, `PRESET_IDEA_HOME` und `PRESET_MCDEV_LIB` überschrieben werden.

Der Plugin-Code und dessen installierte JARs bleiben unverändert. Die Vorlage liegt außerhalb der Installation und überlebt Plugin-Updates. Details und ENV-Namen stehen in der generierten README.
