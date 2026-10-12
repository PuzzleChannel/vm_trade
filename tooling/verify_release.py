from pathlib import Path
from zipfile import ZipFile
from PIL import Image
import io,json,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
VERSION=next((line.split('=',1)[1].strip() for line in (ROOT/'gradle.properties').read_text(encoding='utf-8').splitlines() if line.startswith('version=')),None)
if not VERSION: raise SystemExit('version= missing from gradle.properties')
jar=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT / f"build/libs/vm-trade-{VERSION}.jar"
with ZipFile(jar) as z:
  names=set(z.namelist()); meta=json.loads(z.read("fabric.mod.json"))
  assert meta["id"]=="vmtrade" and meta["version"]==VERSION
  assert meta.get("accessWidener")=="opentrade.accesswidener" and "opentrade.accesswidener" in names
  required=[
  'com/puzzlechannel/vmtrade/MarketBlock.class',
    "fabric.mod.json","vmtrade.mixins.json","opentrade.accesswidener",
    "assets/opentrade/lang/ru_ru.json","assets/vmtrade/lang/ru_ru.json",
    "assets/vmtrade/models/block/market.json","assets/vmtrade/items/market.json",
    "assets/vmtrade/models/item/market.json","assets/vmtrade/blockstates/market.json",
    "assets/vmtrade/textures/block/market_front.png","assets/vmtrade/textures/block/market_side.png",
    "assets/vmtrade/textures/block/market_top.png","assets/vmtrade/textures/block/market_bottom.png"
  ]
  for p in required: assert p in names,p
  mix=json.loads(z.read("vmtrade.mixins.json"))
  assert "MarketScreenTranslationMixin" not in mix.get("client",[])
  assert "UiTranslationClientMixin" in mix.get("client",[])
  assert "UiTranslationServerMixin" not in mix.get("mixins",[])
  assert "VmTradeUiTranslationServerMixin" in mix.get("mixins",[])
  assert mix["package"]=="net.onlyjugs.fabric.mixin"
  for tex in ["market_front.png","market_side.png","market_top.png","market_bottom.png"]:
    im=Image.open(io.BytesIO(z.read("assets/vmtrade/textures/block/"+tex))).convert("RGBA")
    assert im.getchannel("A").getextrema()==(255,255), tex
  item=json.loads(z.read("assets/vmtrade/items/market.json")); assert item["model"]["model"]=="vmtrade:block/market"
  vmru=json.loads(z.read("assets/vmtrade/lang/ru_ru.json")); op_ru=json.loads(z.read("assets/opentrade/lang/ru_ru.json"))
  assert vmru.get("item.vmtrade.vmcoin")=="ВМуткоин (ВК)"
  assert op_ru.get("block.opentrade.trade_chest")=="Торговый сундук"
  # Every vendor Open Trade class that is present in the final JAR must be byte-for-byte
  # identical to the pristine 1.4.2 vendor JAR. Our own wrapper/mixin classes are the only additions.
  pristine=ZipFile(Path("vendor/open-trade-1.4.2.jar"))
  try:
    vendor_class_names={n for n in pristine.namelist() if n.startswith("net/onlyjugs/") and n.endswith(".class")}
    excluded_vendor_classes={
      "net/onlyjugs/fabric/mixin/AbstractContainerScreenInitMixin.class",
    }
    for name in sorted(vendor_class_names - excluded_vendor_classes):
      if name in names:
        assert z.read(name)==pristine.read(name), name
  finally:
    pristine.close()
  # No compile-time stub leakage.
  forbidden=["net/minecraft/world/level/block/entity/BlockEntityType.class","net/minecraft/network/chat/Component.class","org/spongepowered/asm/mixin/Mixin.class"]
  for p in forbidden: assert p not in names,p
  assert "net/onlyjugs/fabric/mixin/VmTradeClientTextMixin.class" not in names
  assert "net/onlyjugs/fabric/mixin/VmTradeServerTextMixin.class" not in names
  assert "net/onlyjugs/fabric/mixin/UiTranslationClientMixin.class" in names
  assert "net/onlyjugs/fabric/mixin/UiTranslationServerMixin.class" not in names
  assert "net/onlyjugs/fabric/mixin/VmTradeUiTranslationServerMixin.class" in names
  assert b"RuntimeVisibleAnnotations" in z.read("net/onlyjugs/fabric/mixin/UiTranslationClientMixin.class")
  server_mix=z.read("net/onlyjugs/fabric/mixin/VmTradeUiTranslationServerMixin.class")
  assert b"RuntimeVisibleAnnotations" in server_mix
  assert b"Lorg/spongepowered/asm/mixin/Mixin;" in server_mix
  mb=z.read('com/puzzlechannel/vmtrade/MarketBlock.class')
  assert b'RenderShape' in mb and b'MODEL' in mb
print("VM Trade release verification: OK")
