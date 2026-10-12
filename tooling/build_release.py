from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import json, shutil, hashlib, tempfile

ROOT = Path(__file__).resolve().parents[1]
VENDOR = ROOT / 'vendor/open-trade-1.4.2.jar'
LEGACY_UI = ROOT / 'VMTrade-1.0.26-ui-deselect-back-alignment.jar'
VERSION = next((line.split('=', 1)[1].strip() for line in (ROOT / 'gradle.properties').read_text(encoding='utf-8').splitlines() if line.startswith('version=')), None)
if not VERSION:
    raise SystemExit('version= missing from gradle.properties')
OUT = ROOT / f'build/libs/vm-trade-{VERSION}.jar'
CLASSES = ROOT / 'build/release_compile'
RES = ROOT / 'src/main/resources'

EXACT_EXCLUDES = {
    'fabric.mod.json',
    'opentrade-fabric.mixins.json',
    'assets/opentrade/lang/en_us.json',
    'net/onlyjugs/fabric/mixin/AbstractContainerScreenInitMixin.class',
    # The current mixins are isolated under com.puzzlechannel.vmtrade.mixin.
    # Never ship old/repackaged copies under the vendor namespace.
    'net/onlyjugs/fabric/mixin/CancelListingReturnMixin.class',
    'net/onlyjugs/fabric/mixin/MarketReturnClientMixin.class',
    'net/onlyjugs/fabric/mixin/MarketScreenBackMixin.class',
    'net/onlyjugs/fabric/mixin/MarketScreenLayoutMixin.class',
    'net/onlyjugs/fabric/mixin/ServerTransactionGuardMixin.class',
    'net/onlyjugs/fabric/mixin/UiTranslationClientMixin.class',
    'net/onlyjugs/fabric/mixin/UiTranslationServerMixin.class',
    'net/onlyjugs/fabric/mixin/VmTradeUiTranslationServerMixin.class',
    'opentrade-neoforge.mixins.json',
    'META-INF/accesstransformer.cfg',
    'META-INF/neoforge.mods.toml',
}

def forbidden(name: str) -> bool:
    return name in EXACT_EXCLUDES or name.startswith('net/onlyjugs/neoforge/') or name.startswith('META-INF/forgix/')

if not VENDOR.exists(): raise SystemExit('vendor jar missing')
if not LEGACY_UI.exists(): raise SystemExit('legacy VM Trade UI jar missing')
if not CLASSES.exists(): raise SystemExit('compiled classes missing')

# The VМуткоин must use a 1000-item maximum stack; keep this a source-level
# property so no vendor bytecode or constant-pool patching is involved.
init_source=(ROOT / 'src/main/java/com/puzzlechannel/vmtrade/VmTradeInitializer.java').read_text(encoding='utf-8')
if '.stacksTo(1000)' not in init_source:
    raise SystemExit('VМуткоин stack size is not 1000')

with tempfile.TemporaryDirectory() as td:
    stage = Path(td) / 'stage'
    stage.mkdir()

    with ZipFile(VENDOR) as z:
        for info in z.infolist():
            n = info.filename
            if n.endswith('/') or forbidden(n):
                continue
            dest = stage / n
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(z.read(n))

    # Only our wrapper classes are added; vendor classes stay untouched.
    for f in CLASSES.rglob('*'):
        if f.is_file():
            rel = f.relative_to(CLASSES)
            dest = stage / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(f, dest)

    # Restore previously shipped VM Trade UI/history classes that are not in
    # the source tree. Prefer freshly compiled current classes on path collisions,
    # matching Gradle's first-wins duplicate strategy.
    with ZipFile(LEGACY_UI) as legacy:
        for info in legacy.infolist():
            n = info.filename
            if n.endswith('/'):
                continue
            if not (n.startswith('com/puzzlechannel/vmtrade/')
                    or n in {
                        'net/onlyjugs/fabric/mixin/CurrencyDisplayMixin.class',
                        'net/onlyjugs/fabric/mixin/ItemKeysLabelMixin.class',
                    }):
                continue
            dest = stage / n
            if dest.exists():
                continue
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(legacy.read(n))

    # Only resources/metadata are localized or changed.
    for f in RES.rglob('*'):
        if f.is_file():
            rel = f.relative_to(RES)
            dest = stage / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(f, dest)

    meta_path = stage / 'fabric.mod.json'
    meta = json.loads(meta_path.read_text(encoding='utf-8'))
    meta['version'] = VERSION
    meta['accessWidener'] = 'opentrade.accesswidener'
    meta_path.write_text(json.dumps(meta, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

    # Hard fail if any bytecode contains our old translation patches or if original
    # vendor bytecode differs from the pristine user-supplied 1.4.2 JAR.
    with ZipFile(VENDOR) as pristine:
        for path in [
            'net/onlyjugs/client/screen/MarketScreen.class',
            'net/onlyjugs/client/screen/TradeChestScreen.class',
            'net/onlyjugs/OpenTradeConfig.class',
            'net/onlyjugs/net/OpenTradeNet.class',
        ]:
            a = pristine.read(path)
            b = (stage / path).read_bytes()
            if a != b:
                raise SystemExit(f'Vendor bytecode was modified: {path}')

    # Vendor bytecode must remain pristine; runtime localization is provided only by our own Mixins.
    forbidden_strings = [
        b'MarketScreenTranslationMixin',
        b'patch_class_string',
    ]
    for cls in (stage / 'net/onlyjugs').rglob('*.class'):
        data = cls.read_bytes()
        for needle in forbidden_strings:
            # Only check our generated/reworked classes for accidental references;
            # original Open Trade legitimately calls Component.literal.
            if cls.name in {'MarketScreen.class', 'TradeChestScreen.class'} and needle in data:
                raise SystemExit(f'Unexpected localization bytecode in {cls}')

    OUT.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(OUT, 'w', ZIP_DEFLATED, compresslevel=9) as z:
        for f in sorted(stage.rglob('*')):
            if f.is_file():
                z.write(f, f.relative_to(stage).as_posix())

sha = hashlib.sha256(OUT.read_bytes()).hexdigest()
print(OUT)
print('size', OUT.stat().st_size)
print('sha256', sha)
