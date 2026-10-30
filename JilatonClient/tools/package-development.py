"""Package the already-tested build with corresponding project source; never invokes reference code."""
from pathlib import Path
import hashlib
import json
import re
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def main():
    jar = ROOT / "build/libs/jilaton-client-0.1.0.jar"
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    reports = list((ROOT / "build/test-results/test").glob("TEST-*.xml"))
    assert reports, "Run the tests before packaging"
    for path in reports:
        suite = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(suite.get(key, 0))
    assert totals["failures"] == totals["errors"] == totals["skipped"] == 0
    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()
        assert not any(name.startswith(("net/anubis/", "net/ccbluex/", "imgui/")) for name in names)
        assert "dev/jilaton/client/module/impl/ZoomModule.class" in names
        mappings = json.loads(archive.read("jilaton.refmap.json"))["mappings"]
        for mixin in ("ZoomFovMixin", "ZoomMouseMixin", "HudMouseInputMixin"):
            assert any(mixin in key for key in mappings)
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    summary = dict(**totals, artifact=jar.relative_to(ROOT).as_posix(), size_bytes=jar.stat().st_size,
                   sha256=digest, packaging_checks="Zoom and new remapped Mixins present; no reference client classes bundled")
    (ROOT / "development/build-verification.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    inventory = ROOT / "IMPLEMENTATION_INVENTORY.md"
    text = inventory.read_text(encoding="utf-8")
    text = re.sub(r'Checkpoint: [^\n]+',
                  f'Checkpoint: 2026-10-30. The master specification remains incomplete. Latest offline test/build passed {totals["tests"]} tests (zero failures/errors/skips). Client SHA-256: `{digest}`. Specific live checks and open work: [development/CHECKPOINT.md](development/CHECKPOINT.md).', text, count=1)
    text = re.sub(r'Latest artifact: [^\n]+',
                  f'Latest artifact: `build/libs/jilaton-client-0.1.0.jar` ({jar.stat().st_size:,} bytes), SHA-256 `{digest}`. All {totals["tests"]} automated tests pass. The source client loaded in a newly created local Creative world, and specific HUD controls were smoke-tested. This is not feature parity, multiplayer verification or release readiness; see `development/CHECKPOINT.md`.', text, count=1)
    hud = '| CLIENT | `hud` | `src/main/java/dev/jilaton/client/module/impl/HudModule.java` | Saved drag/scale/panel-opacity/accent/alignment/visibility, armor/effects/CPS/notifications and target health interpolation implemented; 259-test build passes. Armor drag, scaling, recoloring, hide behavior and saved properties checked in a local world. Full widget behavior remains unverified; see checkpoint | Existing settings/config, HudLayout, HudStyle, mouse-event Mixin, bounded input/notification histories |'
    text = re.sub(r'^\| CLIENT \| `hud` .*$', hud, text, flags=re.M)
    if '| RENDER | `zoom` |' not in text:
        text = text.replace(hud, '| RENDER | `zoom` | `src/main/java/dev/jilaton/client/module/impl/ZoomModule.java` | Configurable hold/toggle FOV zoom, scroll adjustment, float transition and sensitivity scaling; settings/profile/ratio tests pass and Mixins load. Detailed live controls unverified | Public LiquidBounce Zoom behavior, existing settings/config/keybinds, GameRenderer/MouseHandler Mixins |\n' + hud)
    text = text.replace('build passes, visual/input behavior not yet checked in Minecraft.', 'build passes; title navigation, ClickGUI and compact button fitting were observed in the dev client. Full screen navigation/focus testing remains open.')
    inventory.write_text(text, encoding="utf-8")
    dist = ROOT / "build/distributions"
    dist.mkdir(exist_ok=True)
    source = dist / "jilaton-client-0.1.0-source.zip"
    documents = ["build.gradle", "settings.gradle", "gradle.properties", "LICENSE", "ATTRIBUTION.md", "README.md", "BUILDING.md",
                 "IMPLEMENTATION_INVENTORY.md", "LIQUIDBOUNCE_MODULE_INVENTORY.md"]
    with zipfile.ZipFile(source, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in sorted((ROOT / "src").rglob("*")):
            if path.is_file(): archive.write(path, path.relative_to(ROOT).as_posix())
        for name in documents: archive.write(ROOT / name, name)
        for directory in ("development", "tools"):
            for path in sorted((ROOT / directory).glob("*")):
                if path.is_file(): archive.write(path, path.relative_to(ROOT).as_posix())
    bundle = dist / "jilaton-client-0.1.0-development.zip"
    with zipfile.ZipFile(bundle, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.write(jar, jar.name)
        archive.write(source, source.name)
        for name in ("LICENSE", "ATTRIBUTION.md", "README.md", "BUILDING.md", "development/CHECKPOINT.md"):
            archive.write(ROOT / name, name)
    print(json.dumps(dict(**summary, development_bundle=bundle.relative_to(ROOT).as_posix(), corresponding_source=source.relative_to(ROOT).as_posix())))


if __name__ == "__main__":
    main()
