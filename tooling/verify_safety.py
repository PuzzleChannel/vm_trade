"""Release-integrity checks for VM Trade.

This is a static audit, not a substitute for multiplayer exploit testing.
Run after building: python tooling/verify_safety.py [path/to/mod.jar]
"""
from pathlib import Path
from zipfile import ZipFile
import json
import sys

ROOT = Path(__file__).resolve().parents[1]
VERSION = next(
    (line.split("=", 1)[1].strip()
     for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines()
     if line.startswith("version=")),
    None,
)
if not VERSION:
    raise SystemExit("version= missing from gradle.properties")

jar_path = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / f"build/libs/vm-trade-{VERSION}.jar"
vendor_path = ROOT / "vendor/open-trade-1.4.2.jar"
if not jar_path.is_file():
    raise SystemExit(f"Release JAR not found: {jar_path}")
if not vendor_path.is_file():
    raise SystemExit(f"Pristine vendor JAR not found: {vendor_path}")

with ZipFile(jar_path) as release, ZipFile(vendor_path) as vendor:
    entries = release.infolist()
    names_list = [entry.filename for entry in entries]
    names = set(names_list)
    duplicates = sorted({name for name in names_list if names_list.count(name) > 1})
    if duplicates:
        raise AssertionError(f"Duplicate ZIP entries in release: {duplicates}")

    meta = json.loads(release.read("fabric.mod.json"))
    if meta.get("id") != "vmtrade" or meta.get("version") != VERSION:
        raise AssertionError(f"Unexpected mod metadata: id={meta.get('id')!r}, version={meta.get('version')!r}")
    if meta.get("depends", {}).get("minecraft") != ">=26.2 <26.3":
        raise AssertionError("Minecraft dependency range is not pinned to 26.2.x")
    if int(meta.get("depends", {}).get("java", "0").lstrip(">=")) < 25:
        raise AssertionError("Java 25+ is not declared")

    mixins = json.loads(release.read("vmtrade.mixins.json"))
    configured = list(mixins.get("mixins", [])) + list(mixins.get("client", []))
    for mixin in configured:
        path = f"{mixins['package'].replace('.', '/')}/{mixin}.class"
        if path not in names:
            raise AssertionError(f"Mixin is configured but missing from JAR: {path}")

    forbidden_suffixes = (
        "org/spongepowered/asm/mixin/Mixin.class",
        "net/minecraft/network/chat/Component.class",
        "net/minecraft/world/inventory/MenuProvider.class",
        "net/fabricmc/api/ModInitializer.class",
    )
    leaked = [name for name in names if any(name.endswith(suffix) for suffix in forbidden_suffixes)]
    if leaked:
        raise AssertionError(f"Development stub/API classes leaked into release: {leaked}")

    critical_vendor_classes = (
        "net/onlyjugs/client/screen/MarketScreen.class",
        "net/onlyjugs/client/screen/TradeChestScreen.class",
        "net/onlyjugs/net/OpenTradeNet.class",
        "net/onlyjugs/OpenTradeConfig.class",
    )
    vendor_names = set(vendor.namelist())
    for path in critical_vendor_classes:
        if path not in names or path not in vendor_names:
            raise AssertionError(f"Critical upstream class missing: {path}")
        if release.read(path) != vendor.read(path):
            raise AssertionError(f"Upstream class was modified unexpectedly: {path}")

    required_own_classes = (
        "com/puzzlechannel/vmtrade/VmTradeInitializer.class",
        "com/puzzlechannel/vmtrade/UiTextTranslations.class",
        "net/onlyjugs/fabric/mixin/CancelListingReturnMixin.class",
        "net/onlyjugs/fabric/mixin/UiTranslationClientMixin.class",
        "net/onlyjugs/fabric/mixin/UiTranslationServerMixin.class",
    )
    for path in required_own_classes:
        if path not in names:
            raise AssertionError(f"Required VM Trade class missing: {path}")

cancel_source = (ROOT / "src/main/java/net/onlyjugs/fabric/mixin/CancelListingReturnMixin.java").read_text(encoding="utf-8")
clear_at = cancel_source.find("menu.stageContainer().setItem(0, ItemStack.EMPTY)")
add_at = cancel_source.find("player.getInventory().add(returned)")
drop_at = cancel_source.find("player.drop(returned, false)")
if min(clear_at, add_at, drop_at) < 0 or not (clear_at < add_at < drop_at):
    raise AssertionError("Cancelled listing must be removed from staging before inventory insertion/drop")

print(f"VM Trade release safety audit: OK ({jar_path.name})")
print("Note: this checks archive integrity and known invariants; runtime exploit tests are still required.")
