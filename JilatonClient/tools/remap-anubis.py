"""Rewrite intermediary names (class_/method_/field_/comp_) in recovered Anubis sources to Mojang names.

Reads Loom's cached tiny v2 mappings (official -> intermediary -> named) and writes remapped copies of the
requested source files. It only reads text; nothing from the reference client is executed or compiled.

usage: python -I tools/remap-anubis.py <mappings.tiny> <out-dir> <source.java>...
"""
import os
import re
import sys


def load(path):
    classes, members = {}, {}
    with open(path, encoding='utf-8') as handle:
        handle.readline()
        for line in handle:
            parts = line.rstrip('\n').split('\t')
            if parts[0] == 'c':
                classes[parts[2]] = parts[3]
            elif len(parts) >= 6 and parts[1] in ('f', 'm'):
                # "\tm\t<desc>\t<official>\t<intermediary>\t<named>"
                inter, named = parts[4], parts[5]
                if inter != named:
                    members.setdefault(inter, named)
    return classes, members


def remap(text, classes, members):
    simple = {}
    for inter, named in classes.items():
        if not inter.startswith('net/minecraft/class_'):
            continue
        key = inter[len('net/minecraft/'):].replace('$', '.')
        simple[key] = named.split('/')[-1].replace('$', '.')
    imports = set()

    def import_sub(match):
        inter = 'net/minecraft/' + match.group(1)
        named = classes.get(inter)
        if named is None:
            return match.group(0)
        outer = named.split('$')[0].replace('/', '.')
        imports.add(outer)
        return ''

    text = re.sub(r'^import net\.minecraft\.(class_\d+);\n', import_sub, text, flags=re.M)
    text = re.sub(r'^ \*  net\.minecraft\.class_\d+\n', '', text, flags=re.M)

    def class_sub(match):
        chain = match.group(0)
        return simple.get(chain, chain)

    # Longest chains first so inner classes (class_A.class_B) resolve before their outer class.
    text = re.sub(r'(?:net\.minecraft\.)?class_\d+(?:\.class_\d+)*',
                  lambda m: class_sub(type('M', (), {'group': lambda self, i=0: m.group(0).replace('net.minecraft.', '')})()),
                  text)
    # Inner classes of non-net.minecraft types (e.g. com.mojang VertexFormat$class_5596 -> Mode) keep
    # intermediary simple names; resolve them by their last path segment.
    inner = {}
    for inter, named in classes.items():
        last = inter.split('$')[-1]
        if last.startswith('class_') and '$' in inter:
            inner.setdefault(last, named.split('$')[-1])
    text = re.sub(r'\bclass_\d+\b', lambda m: inner.get(m.group(0), m.group(0)), text)
    text = re.sub(r'\b(method|field|comp)_\d+\b', lambda m: members.get(m.group(0), m.group(0)), text)
    if imports:
        block = ''.join('import %s;\n' % name for name in sorted(imports))
        text = re.sub(r'^(package [^\n]+;\n)', r'\1\n' + block.replace('\\', '\\\\'), text, count=1, flags=re.M)
    return text


def main():
    mappings, out_dir, sources = sys.argv[1], sys.argv[2], sys.argv[3:]
    classes, members = load(mappings)
    os.makedirs(out_dir, exist_ok=True)
    for source in sources:
        with open(source, encoding='utf-8') as handle:
            text = handle.read()
        result = remap(text, classes, members)
        left = len(re.findall(r'\b(?:class|method|field|comp)_\d+\b', result))
        target = os.path.join(out_dir, os.path.basename(source))
        with open(target, 'w', encoding='utf-8') as handle:
            handle.write(result)
        print('%s: %d unresolved' % (os.path.basename(source), left))


if __name__ == '__main__':
    main()
