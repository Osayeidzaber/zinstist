"""Inventory source without executing reference code. Run from any directory with Python 3."""
from pathlib import Path
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "development"
MAIN = ROOT / "src/main/java"
UPSTREAM = ROOT.parent / "analysis/liquidbounce-upstream/src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules"
ANUBIS = ROOT / "reference/anubis-cfr-0.9.9.3/net/anubis/client/module/impl"


def relative(path):
    return Path(__import__('os').path.relpath(path, ROOT)).as_posix()


def settings(text):
    found = []
    for match in re.finditer(r'new (Boolean|Number|Choice|Text)Setting\s*\(', text):
        start = match.end()
        depth = 1
        end = start
        quoted = False
        escaped = False
        while end < len(text) and depth:
            char = text[end]
            if char == '"' and not escaped:
                quoted = not quoted
            if not quoted:
                depth += (char == '(') - (char == ')')
            escaped = char == '\\' and not escaped
            end += 1
        args = text[start:end - 1]
        if not re.match(r'\s*"', args):
            continue  # Parameterized helper declarations are expanded separately.
        strings = re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', args)
        if len(strings) >= 2:
            found.append(dict(type=match.group(1), id=strings[0], label=strings[1],
                              declaration=args.strip(), modes=strings[2:] if match.group(1) == "Choice" else []))
    return found


def hand_settings(text):
    """Expand ViewModel's parameterized HandOptions groups without executing Java."""
    result = []
    for prefix, label in re.findall(r'new HandOptions\("([^"]+)", "([^"]+)"\)', text):
        result.append(dict(type="Boolean", id=prefix + "_enabled", label=label + " transform",
                           declaration="true", modes=[]))
        for suffix, title, numeric in re.findall(r'number\(id \+ "([^"]+)", label \+ "([^"]+)", ([^)]+)\)', text):
            result.append(dict(type="Number", id=prefix + suffix, label=label + title,
                               declaration=numeric, modes=[]))
    return result


def main():
    OUT.mkdir(exist_ok=True)
    registry = (MAIN / "dev/jilaton/client/module/ModuleManager.java").read_text(encoding="utf-8")
    registered = set(re.findall(r'register\(new (?:[\w]+\.)*(\w+)\(', registry))
    test_files = [(p, p.read_text(encoding="utf-8")) for p in (ROOT / "src/test/java").rglob("*.java")]
    modules = []
    for path in sorted((MAIN / "dev/jilaton/client/module/impl").glob("*Module.java")):
        text = path.read_text(encoding="utf-8")
        match = re.search(r'super\("([^"]+)",\s*"([^"]+)",\s*"[^"]*",\s*ModuleCategory\.(\w+)\)', text)
        if not match or path.stem not in registered:
            continue
        imported = re.findall(r'import (dev\.jilaton\.[\w.]+);', text)
        dependencies = [relative(MAIN / (name.replace('.', '/') + ".java")) for name in imported]
        related_tests = [relative(p) for p, body in test_files if path.stem in body or '"' + match[1] + '"' in body]
        modules.append(dict(id=match[1], name=match[2], category=match[3], state="IMPLEMENTED",
                            source_path=relative(path), target_path=relative(path),
                            source_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                            provenance="See ATTRIBUTION.md and IMPLEMENTATION_INVENTORY.md; implementations may be partial",
                            settings=settings(text) + hand_settings(text) + (settings((MAIN / "dev/jilaton/client/render/LivingOverlayOptions.java").read_text(encoding="utf-8")) if "new LivingOverlayOptions(" in text else []), dependencies=dependencies,
                            dependency_coverage="Direct project imports only; dependency closure is not yet audited",
                            settings_coverage="Declared constructor settings; GUI/config use the shared Setting framework",
                            supported_minecraft="1.21.11", tests=related_tests,
                            runtime_verification="UNVERIFIED; see checkpoint for specific smoke checks",
                            compatibility_issues="Source-client parity and server compatibility are not established"))
    assert len(modules) == len(registered), (len(modules), len(registered))
    references = []
    for origin, base, pattern in [("Anubis recovered", ANUBIS, "*Module.java"), ("LiquidBounce public source", UPSTREAM, "Module*.kt")]:
        for path in sorted(base.rglob(pattern)):
            text = path.read_text(encoding="utf-8", errors="replace")
            category = path.relative_to(base).parts[0].upper()
            source_category = category
            if category == "MISC": category = "MISCELLANEOUS"
            if category == "EXPLOIT": category = "MISCELLANEOUS"
            if category in ("FUN", "DONUTMACROS"): category = "MISCELLANEOUS"
            references.append(dict(origin=origin, name=path.stem, category=category,
                                   source_category=source_category,
                                   state="EXCLUDED" if source_category == "DONUTMACROS" else "DISCOVERED",
                                   exclusion_reason="Server-specific Donut macros are outside the master specification" if source_category == "DONUTMACROS" else None,
                                   source_path=relative(path), target_path=None,
                                   source_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                                   dependencies=[], modes=[], settings_coverage="Pending behavior audit",
                                   compatibility_issues="Not imported automatically; review behavior, license and dependencies",
                                   evidence="Existing markdown inventories retain partial coverage mappings; name discovery is not a port"))
    report = dict(schema_version=1, supported_states=["DISCOVERED", "IN_SCOPE", "IN_PROGRESS", "IMPLEMENTED", "VERIFIED", "BLOCKED", "DUPLICATE", "EXCLUDED"],
                  scope="All registered Jilaton modules and matching module source filenames in the two local reference trees",
                  limitations=["Lexical source inventory, not complete behavioral analysis", "No module is marked VERIFIED by compilation alone",
                               "Reference filenames may include abstract/shared module entry points; no missing source is fabricated",
                               "Detailed reference-to-target behavior mappings remain in the markdown inventories"],
                  counts=dict(registered=len(modules), reference_sources=len(references), verified=0),
                  modules=modules, references=references)
    ports_file = OUT / "render-ports.json"
    if ports_file.exists():
        ports = json.loads(ports_file.read_text(encoding="utf-8"))["ports"]
        for port in ports:
            matches = [ref for ref in references if ref["source_path"].endswith(port["source_suffix"])]
            assert len(matches) == 1, (port["source_suffix"], len(matches))
            ref = matches[0]
            for key in ("state", "modes", "settings_coverage", "dependencies", "evidence", "compatibility_issues"):
                ref[key] = port[key]
            ref["target_paths"] = [module["target_path"] for module in modules if module["id"] in port["targets"]]
            ref["target_path"] = ref["target_paths"][0]
            for module in modules:
                if module["id"] in port["targets"]:
                    module.setdefault("source_ports", []).append(ref["source_path"])
                    module["compatibility_issues"] = port["compatibility_issues"]
    (OUT / "integration-manifest.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report["counts"]))


if __name__ == "__main__":
    main()
