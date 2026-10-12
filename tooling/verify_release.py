from classfile_checks import has_class_level_mixin_annotation
from pathlib import Path
from zipfile import ZipFile
from PIL import Image
import io,json,sys
ROOT=Path(__file__).resolve().parents[1]
VERSION=next((line.split("=",1)[1].strip() for line in (ROOT/"gradle.properties").read_text(encoding="utf-8").splitlines() if line.startswith("version=")),None)
if not VERSION: raise SystemExit("version= missing from gradle.properties")
jar=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/f"build/libs/vm-trade-{VERSION}.jar"
own=["CancelListingReturnMixin","ServerTransactionGuardMixin","VmTradeUiTranslationServerMixin","UiTranslationClientMixin","MarketScreenLayoutMixin","MarketReturnClientMixin","TradeChestWalletOrderMixin"]
with ZipFile(jar) as z:
    names_list=[x.filename for x in z.infolist()]; names=set(names_list)
    duplicates=sorted({n for n in names_list if names_list.count(n)>1}); assert not duplicates,duplicates
    meta=json.loads(z.read("fabric.mod.json"))
    assert meta.get("id")=="vmtrade" and meta.get("version")==VERSION
    assert "vmtrade-custom.mixins.json" in meta.get("mixins",[])
    assert meta.get("depends",{}).get("minecraft")==">=26.2 <26.3" and meta.get("accessWidener")=="opentrade.accesswidener"
    main=json.loads(z.read("vmtrade.mixins.json")); custom=json.loads(z.read("vmtrade-custom.mixins.json"))
    assert main.get("package")=="net.onlyjugs.fabric.mixin" and main.get("mixins")==["MinecraftServerTickMixin","ServerPlayerGameModeMixin","MerchantOpenScreenMixin"] and main.get("client",[])==["CurrencyDisplayMixin","ItemKeysLabelMixin"]
    assert custom.get("package")=="com.puzzlechannel.vmtrade.mixin"
    assert custom.get("mixins")==["CancelListingReturnMixin","ServerTransactionGuardMixin","VmTradeUiTranslationServerMixin"]
    assert custom.get("client")==["UiTranslationClientMixin","MarketScreenLayoutMixin","MarketReturnClientMixin","TradeChestWalletOrderMixin"]
    for n in own:
        p=f"com/puzzlechannel/vmtrade/mixin/{n}.class"; assert p in names,p
        b=z.read(p); assert has_class_level_mixin_annotation(b), f"@Mixin must be class-level RuntimeInvisibleAnnotations: {p}"
        assert f"net/onlyjugs/fabric/mixin/{n}.class" not in names
    for n in ["CurrencyDisplayMixin","ItemKeysLabelMixin"]:
        p=f"net/onlyjugs/fabric/mixin/{n}.class"; assert p in names,p
        assert has_class_level_mixin_annotation(z.read(p)), f"Legacy UI mixin lacks @Mixin: {p}"
    for p in ["com/puzzlechannel/vmtrade/TradeChestClientUi.class","com/puzzlechannel/vmtrade/SaleHistoryStore.class","com/puzzlechannel/vmtrade/TradeHistoryNetwork.class","com/puzzlechannel/vmtrade/TradeSyncService.class"]:
        assert p in names,p
    for n in ["CurrencyDisplayMixin","ItemKeysLabelMixin"]:
        p=f"net/onlyjugs/fabric/mixin/{n}.class"; assert p in names,p
        assert has_class_level_mixin_annotation(z.read(p)), f"Legacy UI mixin lacks @Mixin: {p}"
    for p in ["com/puzzlechannel/vmtrade/TradeChestClientUi.class","com/puzzlechannel/vmtrade/SaleHistoryStore.class","com/puzzlechannel/vmtrade/TradeHistoryNetwork.class","com/puzzlechannel/vmtrade/TradeSyncService.class"]:
        assert p in names,p
    req=["com/puzzlechannel/vmtrade/MarketBlock.class","com/puzzlechannel/vmtrade/TransactionGuard.class","com/puzzlechannel/vmtrade/UiTextTranslations.class","opentrade.accesswidener","assets/opentrade/lang/ru_ru.json","assets/vmtrade/lang/ru_ru.json","assets/vmtrade/models/block/market.json","assets/vmtrade/items/market.json","assets/vmtrade/models/item/market.json","assets/vmtrade/blockstates/market.json","assets/vmtrade/textures/block/market_front.png","assets/vmtrade/textures/block/market_side.png","assets/vmtrade/textures/block/market_top.png","assets/vmtrade/textures/block/market_bottom.png"]
    for p in req: assert p in names,p
    for tex in ["market_front.png","market_side.png","market_top.png","market_bottom.png"]:
        im=Image.open(io.BytesIO(z.read("assets/vmtrade/textures/block/"+tex))).convert("RGBA"); assert im.getchannel("A").getextrema()==(255,255),tex
    assert json.loads(z.read("assets/vmtrade/items/market.json"))["model"]["model"]=="vmtrade:block/market"
    assert json.loads(z.read("assets/vmtrade/models/item/market.json"))["parent"]=="vmtrade:block/market"
    assert json.loads(z.read("assets/vmtrade/lang/ru_ru.json")).get("item.vmtrade.vmcoin")=="ВМуткоин (ВК)"
    assert json.loads(z.read("assets/opentrade/lang/ru_ru.json")).get("block.opentrade.trade_chest")=="Торговый сундук"
    for p in ["org/spongepowered/asm/mixin/Mixin.class","net/minecraft/network/chat/Component.class","net/minecraft/world/inventory/MenuProvider.class","net/fabricmc/api/ModInitializer.class"]: assert p not in names,p
    with ZipFile(ROOT/"vendor/open-trade-1.4.2.jar") as vendor:
        for p in vendor.namelist():
            if p.startswith("net/onlyjugs/") and p.endswith(".class") and p in names:
                assert z.read(p)==vendor.read(p),f"Modified vendor class: {p}"
print("VM Trade release verification: OK")
