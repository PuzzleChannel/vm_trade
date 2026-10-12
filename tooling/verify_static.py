import json, zipfile, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
VERSION = next((line.split('=', 1)[1].strip() for line in (ROOT / 'gradle.properties').read_text(encoding='utf-8').splitlines() if line.startswith('version=')), None)
if not VERSION:
    raise SystemExit('version= missing from gradle.properties')
jar = sys.argv[1] if len(sys.argv) > 1 else str(ROOT / f"build/libs/vm-trade-{VERSION}.jar")
with zipfile.ZipFile(jar) as z:
    names=set(z.namelist())
    meta=json.loads(z.read("fabric.mod.json"))
    assert meta["id"]=="vmtrade" and meta["version"]==VERSION
    assert meta["depends"]["minecraft"]==">=26.2 <26.3"
    assert meta.get("accessWidener")=="opentrade.accesswidener"
    assert "opentrade.accesswidener" in names
    assert "net/onlyjugs/fabric/mixin/AbstractContainerScreenInitMixin.class" not in names
    assert "opentrade-fabric.mixins.json" not in names
    assert "opentrade-neoforge.mixins.json" not in names
    assert "net/onlyjugs/fabric/mixin/MarketScreenTranslationMixin.class" not in names
    assert "net/onlyjugs/fabric/mixin/UiTranslationClientMixin.class" in names
    assert "net/onlyjugs/fabric/mixin/UiTranslationServerMixin.class" in names
    mix=json.loads(z.read("vmtrade.mixins.json"))
    assert "MarketScreenTranslationMixin" not in mix.get("client",[])
    assert "UiTranslationClientMixin" in mix.get("client",[])
    assert "UiTranslationServerMixin" in mix.get("mixins",[])
    init=z.read("com/puzzlechannel/vmtrade/VmTradeInitializer.class")
    market=z.read("com/puzzlechannel/vmtrade/MarketBlock.class")
    assert b"SimpleMenuProvider" not in market
    assert b"MenuConstructor" not in market
    # Market client-item definition points directly to the 3D block model.
    item=json.loads(z.read("assets/vmtrade/items/market.json"))
    assert item["model"]["model"]=="vmtrade:block/market"
    im=json.loads(z.read("assets/vmtrade/models/item/market.json"))
    assert im["parent"]=="vmtrade:block/market"
    ru=json.loads(z.read("assets/vmtrade/lang/ru_ru.json"))
    assert ru.get("opentrade.market.no_trades")=="Сделок пока нет."
      # Access widener must explicitly legalize the legacy server field.
    aw=z.read("opentrade.accesswidener").decode("utf-8")
    assert "accessible field net/minecraft/server/level/ServerPlayer server Lnet/minecraft/server/MinecraftServer;" in aw
    for n in [
      "assets/vmtrade/textures/block/market_front.png",
      "assets/vmtrade/textures/block/market_side.png",
      "assets/vmtrade/textures/block/market_top.png",
      "assets/vmtrade/textures/block/market_bottom.png",
      "assets/vmtrade/models/block/market.json",
      "assets/vmtrade/blockstates/market.json",
      "data/vmtrade/recipes/market.json",
      "assets/vmtrade/lang/ru_ru.json"]:
        assert n in names,n
print("VM Trade static verification: OK")
