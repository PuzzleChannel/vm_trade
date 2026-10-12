import json, sys, zipfile
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
VERSION = next((line.split("=", 1)[1].strip() for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines() if line.startswith("version=")), None)
if not VERSION: raise SystemExit("version= missing from gradle.properties")
jar = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / f"build/libs/vm-trade-{VERSION}.jar"
own = ["CancelListingReturnMixin", "ServerTransactionGuardMixin", "VmTradeUiTranslationServerMixin", "UiTranslationClientMixin", "MarketScreenLayoutMixin", "MarketScreenBackMixin", "MarketReturnClientMixin"]
with zipfile.ZipFile(jar) as z:
    names=set(z.namelist()); meta=json.loads(z.read("fabric.mod.json"))
    assert meta.get("id")=="vmtrade" and meta.get("version")==VERSION
    assert meta.get("depends",{}).get("minecraft")==">=26.2 <26.3"
    assert "vmtrade.mixins.json" in meta.get("mixins",[]) and "vmtrade-custom.mixins.json" in meta.get("mixins",[])
    assert meta.get("accessWidener")=="opentrade.accesswidener" and "opentrade.accesswidener" in names
    for p in ["vmtrade.mixins.json","vmtrade-custom.mixins.json","assets/vmtrade/lang/ru_ru.json","assets/opentrade/lang/ru_ru.json","assets/vmtrade/models/block/market.json","assets/vmtrade/items/market.json","assets/vmtrade/models/item/market.json","assets/vmtrade/blockstates/market.json","assets/vmtrade/textures/block/market_front.png","assets/vmtrade/textures/block/market_side.png","assets/vmtrade/textures/block/market_top.png","assets/vmtrade/textures/block/market_bottom.png"]:
        assert p in names,p
    main=json.loads(z.read("vmtrade.mixins.json")); custom=json.loads(z.read("vmtrade-custom.mixins.json"))
    assert main.get("package")=="net.onlyjugs.fabric.mixin"
    assert main.get("mixins")==["MinecraftServerTickMixin","ServerPlayerGameModeMixin","MerchantOpenScreenMixin"] and main.get("client",[])==[]
    assert custom.get("package")=="com.puzzlechannel.vmtrade.mixin"
    assert custom.get("mixins")==["CancelListingReturnMixin","ServerTransactionGuardMixin","VmTradeUiTranslationServerMixin"]
    assert custom.get("client")==["UiTranslationClientMixin","MarketScreenLayoutMixin","MarketScreenBackMixin","MarketReturnClientMixin"]
    for n in own:
        p=f"com/puzzlechannel/vmtrade/mixin/{n}.class"; assert p in names,p
        data=z.read(p); assert b"RuntimeVisibleAnnotations" in data and b"Lorg/spongepowered/asm/mixin/Mixin;" in data,p
        assert f"net/onlyjugs/fabric/mixin/{n}.class" not in names
    assert "net/onlyjugs/fabric/mixin/AbstractContainerScreenInitMixin.class" not in names
    assert "opentrade-fabric.mixins.json" not in names and "opentrade-neoforge.mixins.json" not in names
    assert "assets/opentrade/lang/en_us.json" not in names
    assert json.loads(z.read("assets/vmtrade/items/market.json"))["model"]["model"]=="vmtrade:block/market"
    assert json.loads(z.read("assets/vmtrade/models/item/market.json"))["parent"]=="vmtrade:block/market"
print("VM Trade static verification: OK")
