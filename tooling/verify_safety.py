"""Release-integrity and transaction-hardening checks for VM Trade."""
from classfile_checks import has_class_level_mixin_annotation
from pathlib import Path
from zipfile import ZipFile
import json,sys
ROOT=Path(__file__).resolve().parents[1]
VERSION=next((line.split("=",1)[1].strip() for line in (ROOT/"gradle.properties").read_text(encoding="utf-8").splitlines() if line.startswith("version=")),None)
if not VERSION: raise SystemExit("version= missing from gradle.properties")
jar_path=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/f"build/libs/vm-trade-{VERSION}.jar"
vendor_path=ROOT/"vendor/open-trade-1.4.2.jar"
if not jar_path.is_file() or not vendor_path.is_file(): raise SystemExit("Release JAR or pristine vendor JAR is missing")
with ZipFile(jar_path) as release, ZipFile(vendor_path) as vendor:
    names_list=[x.filename for x in release.infolist()]; names=set(names_list)
    for p in ("net/onlyjugs/fabric/mixin/UiTranslationServerMixin.class","net/onlyjugs/fabric/mixin/UiTranslationClientMixin.class"):
        if p in names: raise AssertionError(f"Stale upstream translation mixin leaked into release: {p}")
    duplicates=sorted({n for n in names_list if names_list.count(n)>1})
    if duplicates: raise AssertionError(f"Duplicate ZIP entries: {duplicates}")
    meta=json.loads(release.read("fabric.mod.json"))
    if meta.get("id")!="vmtrade" or meta.get("version")!=VERSION: raise AssertionError("Unexpected mod metadata/version")
    if meta.get("depends",{}).get("minecraft")!=">=26.2 <26.3" or int(meta.get("depends",{}).get("java","0").lstrip(">="))<25: raise AssertionError("Minecraft or Java dependency range mismatch")
    main=json.loads(release.read("vmtrade.mixins.json")); custom=json.loads(release.read("vmtrade-custom.mixins.json"))
    if "vmtrade-custom.mixins.json" not in meta.get("mixins",[]): raise AssertionError("Custom Mixin config is not registered in Fabric metadata")
    if main.get("package")!="net.onlyjugs.fabric.mixin" or main.get("mixins")!=["MinecraftServerTickMixin","ServerPlayerGameModeMixin","MerchantOpenScreenMixin"] or main.get("client",[])!=["CurrencyDisplayMixin","ItemKeysLabelMixin"]: raise AssertionError("Upstream Mixin config unexpectedly contains custom classes")
    expected_server=["CancelListingReturnMixin","ServerTransactionGuardMixin","VmTradeUiTranslationServerMixin"]
    expected_client=["UiTranslationClientMixin","MarketScreenLayoutMixin","MarketReturnClientMixin","TradeChestWalletOrderMixin","TradeChestScreenPolishMixin"]
    if custom.get("package")!="com.puzzlechannel.vmtrade.mixin" or custom.get("mixins")!=expected_server or custom.get("client")!=expected_client: raise AssertionError("VM Trade mixins are not isolated/configured correctly")
    for n in expected_server+expected_client:
        p=f"com/puzzlechannel/vmtrade/mixin/{n}.class"
        if p not in names: raise AssertionError(f"Configured Mixin class missing: {p}")
        b=release.read(p)
        if not has_class_level_mixin_annotation(b): raise AssertionError(f"Configured class lacks class-level RuntimeInvisibleAnnotations @Mixin: {p}")
        if f"net/onlyjugs/fabric/mixin/{n}.class" in names: raise AssertionError(f"Legacy mixin namespace leaked: {n}")
    for p in ("org/spongepowered/asm/mixin/Mixin.class","net/minecraft/network/chat/Component.class","net/minecraft/world/inventory/MenuProvider.class","net/fabricmc/api/ModInitializer.class"):
        if p in names: raise AssertionError(f"Development stub leaked: {p}")
    for p in ("net/onlyjugs/client/screen/MarketScreen.class","net/onlyjugs/client/screen/TradeChestScreen.class","net/onlyjugs/net/OpenTradeNet.class","net/onlyjugs/OpenTradeConfig.class"):
        if p not in names or p not in vendor.namelist() or release.read(p)!=vendor.read(p): raise AssertionError(f"Critical upstream class missing/modified: {p}")
required_own=("com/puzzlechannel/vmtrade/VmTradeInitializer.class","com/puzzlechannel/vmtrade/UiTextTranslations.class","com/puzzlechannel/vmtrade/TransactionGuard.class","com/puzzlechannel/vmtrade/mixin/CancelListingReturnMixin.class","com/puzzlechannel/vmtrade/mixin/UiTranslationClientMixin.class","com/puzzlechannel/vmtrade/mixin/VmTradeUiTranslationServerMixin.class","com/puzzlechannel/vmtrade/mixin/MarketScreenLayoutMixin.class","com/puzzlechannel/vmtrade/mixin/TradeChestWalletOrderMixin.class","com/puzzlechannel/vmtrade/mixin/TradeChestScreenPolishMixin.class","com/puzzlechannel/vmtrade/mixin/MarketReturnClientMixin.class","com/puzzlechannel/vmtrade/mixin/ServerTransactionGuardMixin.class")
with ZipFile(jar_path) as release:
    names=set(release.namelist())
    for p in required_own:
        if p not in names: raise AssertionError(f"Required VM Trade class missing: {p}")
cancel=(ROOT/"src/main/java/com/puzzlechannel/vmtrade/mixin/CancelListingReturnMixin.java").read_text(encoding="utf-8")
a=cancel.find("menu.stageContainer().setItem(0, ItemStack.EMPTY)"); b=cancel.find("player.getInventory().add(returned)"); c=cancel.find("player.drop(returned, false)")
if min(a,b,c)<0 or not a<b<c: raise AssertionError("Cancelled listing must be removed from staging before inventory insertion/drop")
transaction=(ROOT/"src/main/java/com/puzzlechannel/vmtrade/mixin/ServerTransactionGuardMixin.java").read_text(encoding="utf-8")
expected_payloads = {
    "vmtrade$guardBuy": "BuyListingPayload",
    "vmtrade$guardUniqueBuy": "BuyUniqueStackPayload",
    "vmtrade$guardWallet": "WalletActionPayload",
    "vmtrade$guardPlaceOrder": "PlaceBuyOrderPayload",
    "vmtrade$guardCancelOrder": "CancelBuyOrderPayload",
    "vmtrade$guardVillagerTrade": "VillagerExecuteTradePayload",
    "vmtrade$guardSetListing": "SetListingPayload",
    "vmtrade$guardListStage": "ListStagePayload",
    "vmtrade$guardCancelStage": "CancelStageListingPayload",
}
if "@Coerce Object payload" in transaction:
    raise AssertionError("Transaction guard must use exact payload parameter types")
for method, payload_type in expected_payloads.items():
    if f"{method}(net.onlyjugs.net.{payload_type} payload, ServerPlayer player, CallbackInfo ci)" not in transaction:
        raise AssertionError(f"Wrong target descriptor for {method}: expected {payload_type}")
for h in ("handleBuy","handleBuyUniqueStack","handleWallet","handlePlaceBuyOrder","handleCancelBuyOrder","handleVillagerExecute","handleListStage","handleCancelStageListing","handleSetListing"):
    if f'method = "{h}"' not in transaction: raise AssertionError(f"Missing server transaction guard for {h}")
if "TransactionGuard.allow" not in transaction or "ci.cancel()" not in transaction: raise AssertionError("Server guard does not reject replay/invalid requests")
if 'intValue(payload, "quantity") < 1' not in transaction:
    raise AssertionError("Buy handler must reject zero-quantity packets")

wallet_ui=(ROOT/"src/main/java/com/puzzlechannel/vmtrade/mixin/TradeChestWalletOrderMixin.java").read_text(encoding="utf-8")
for needle in ('value.matches("[0-9]{0,7}")', 'Integer.parseInt(value) <= 1_000_000', 'writeIntField(amount, "width", 44)'):
    if needle not in wallet_ui:
        raise AssertionError(f"Wallet UI is missing release input/layout guard: {needle}")

custom_ui=(ROOT/"src/main/resources/vmtrade-custom.mixins.json").read_text(encoding="utf-8")
if "MarketScreenBackMixin" in custom_ui: raise AssertionError("Broken Back override must stay disabled to preserve upstream return-to-Trade-Chest navigation")
for p in ["com/puzzlechannel/vmtrade/TradeChestClientUi.class","com/puzzlechannel/vmtrade/SaleHistoryStore.class","com/puzzlechannel/vmtrade/TradeHistoryNetwork.class","com/puzzlechannel/vmtrade/TradeSyncService.class","net/onlyjugs/fabric/mixin/CurrencyDisplayMixin.class","net/onlyjugs/fabric/mixin/ItemKeysLabelMixin.class"]:
    if p not in names: raise AssertionError(f"Restored UI/history support class is missing: {p}")
client_init=(ROOT/"src/main/java/com/puzzlechannel/vmtrade/VmTradeClientInitializer.java").read_text(encoding="utf-8")
for feature in ("TradeHistoryNetwork","registerClient","TradeChestClientUi","install"):
    if feature not in client_init: raise AssertionError(f"Client UI restoration hook missing: {feature}")
server_init=(ROOT/"src/main/java/com/puzzlechannel/vmtrade/VmTradeInitializer.java").read_text(encoding="utf-8")
for feature in ("TradeSyncService","TradeHistoryNetwork","registerServer"):
    if feature not in server_init: raise AssertionError(f"Server sales-history hook missing: {feature}")
ret=(ROOT/"src/main/java/com/puzzlechannel/vmtrade/mixin/MarketReturnClientMixin.java").read_text(encoding="utf-8")
if "lambda$registerClient$1" not in ret or "ItemDetailCache.clear()" not in ret: raise AssertionError("Market return does not clear stale detail cache")
print(f"VM Trade release safety audit: OK ({jar_path.name})")
print("Static checks do not replace runtime multiplayer exploit testing.")
