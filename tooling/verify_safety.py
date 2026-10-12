"""Release-integrity and transaction-hardening checks for VM Trade."""
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
    duplicates=sorted({n for n in names_list if names_list.count(n)>1})
    if duplicates: raise AssertionError(f"Duplicate ZIP entries: {duplicates}")
    meta=json.loads(release.read("fabric.mod.json"))
    if meta.get("id")!="vmtrade" or meta.get("version")!=VERSION: raise AssertionError("Unexpected mod metadata/version")
    if meta.get("depends",{}).get("minecraft")!=">=26.2 <26.3" or int(meta.get("depends",{}).get("java","0").lstrip(">="))<25: raise AssertionError("Minecraft or Java dependency range mismatch")
    main=json.loads(release.read("vmtrade.mixins.json")); custom=json.loads(release.read("vmtrade-custom.mixins.json"))
    if "vmtrade-custom.mixins.json" not in meta.get("mixins",[]): raise AssertionError("Custom Mixin config is not registered in Fabric metadata")
    if main.get("package")!="net.onlyjugs.fabric.mixin" or main.get("mixins")!=["MinecraftServerTickMixin","ServerPlayerGameModeMixin","MerchantOpenScreenMixin"] or main.get("client",[])!=[]: raise AssertionError("Upstream Mixin config unexpectedly contains custom classes")
    expected_server=["CancelListingReturnMixin","ServerTransactionGuardMixin","VmTradeUiTranslationServerMixin"]
    expected_client=["UiTranslationClientMixin","MarketScreenLayoutMixin","MarketScreenBackMixin","MarketReturnClientMixin"]
    if custom.get("package")!="com.puzzlechannel.vmtrade.mixin" or custom.get("mixins")!=expected_server or custom.get("client")!=expected_client: raise AssertionError("VM Trade mixins are not isolated/configured correctly")
    for n in expected_server+expected_client:
        p=f"com/puzzlechannel/vmtrade/mixin/{n}.class"
        if p not in names: raise AssertionError(f"Configured Mixin class missing: {p}")
        b=release.read(p)
        if b"Lorg/spongepowered/asm/mixin/Mixin;" not in b or b"RuntimeVisibleAnnotations" not in b: raise AssertionError(f"Configured class has no @Mixin annotation: {p}")
        if f"net/onlyjugs/fabric/mixin/{n}.class" in names: raise AssertionError(f"Legacy mixin namespace leaked: {n}")
    for p in ("org/spongepowered/asm/mixin/Mixin.class","net/minecraft/network/chat/Component.class","net/minecraft/world/inventory/MenuProvider.class","net/fabricmc/api/ModInitializer.class"):
        if p in names: raise AssertionError(f"Development stub leaked: {p}")
    for p in ("net/onlyjugs/client/screen/MarketScreen.class","net/onlyjugs/client/screen/TradeChestScreen.class","net/onlyjugs/net/OpenTradeNet.class","net/onlyjugs/OpenTradeConfig.class"):
        if p not in names or p not in vendor.namelist() or release.read(p)!=vendor.read(p): raise AssertionError(f"Critical upstream class missing/modified: {p}")
required_own=("com/puzzlechannel/vmtrade/VmTradeInitializer.class","com/puzzlechannel/vmtrade/UiTextTranslations.class","com/puzzlechannel/vmtrade/TransactionGuard.class","com/puzzlechannel/vmtrade/mixin/CancelListingReturnMixin.class","com/puzzlechannel/vmtrade/mixin/UiTranslationClientMixin.class","com/puzzlechannel/vmtrade/mixin/VmTradeUiTranslationServerMixin.class","com/puzzlechannel/vmtrade/mixin/MarketScreenLayoutMixin.class","com/puzzlechannel/vmtrade/mixin/MarketScreenBackMixin.class","com/puzzlechannel/vmtrade/mixin/MarketReturnClientMixin.class","com/puzzlechannel/vmtrade/mixin/ServerTransactionGuardMixin.class")
with ZipFile(jar_path) as release:
    names=set(release.namelist())
    for p in required_own:
        if p not in names: raise AssertionError(f"Required VM Trade class missing: {p}")
cancel=(ROOT/"src/main/java/com/puzzlechannel/vmtrade/mixin/CancelListingReturnMixin.java").read_text(encoding="utf-8")
a=cancel.find("menu.stageContainer().setItem(0, ItemStack.EMPTY)"); b=cancel.find("player.getInventory().add(returned)"); c=cancel.find("player.drop(returned, false)")
if min(a,b,c)<0 or not a<b<c: raise AssertionError("Cancelled listing must be removed from staging before inventory insertion/drop")
transaction=(ROOT/"src/main/java/com/puzzlechannel/vmtrade/mixin/ServerTransactionGuardMixin.java").read_text(encoding="utf-8")
for h in ("handleBuy","handleBuyUniqueStack","handleWallet","handlePlaceBuyOrder","handleCancelBuyOrder","handleVillagerExecute","handleListStage","handleCancelStageListing","handleSetListing"):
    if f'method = "{h}"' not in transaction: raise AssertionError(f"Missing server transaction guard for {h}")
if "TransactionGuard.allow" not in transaction or "ci.cancel()" not in transaction: raise AssertionError("Server guard does not reject replay/invalid requests")
back=(ROOT/"src/main/java/com/puzzlechannel/vmtrade/mixin/MarketScreenBackMixin.java").read_text(encoding="utf-8")
if 'new OpenMarketPayload("")' not in back or "lambda$buildOurWidgets$0" not in back: raise AssertionError("Back button does not return to market root")
ret=(ROOT/"src/main/java/com/puzzlechannel/vmtrade/mixin/MarketReturnClientMixin.java").read_text(encoding="utf-8")
if "lambda$registerClient$1" not in ret or "ItemDetailCache.clear()" not in ret: raise AssertionError("Market return does not clear stale detail cache")
print(f"VM Trade release safety audit: OK ({jar_path.name})")
print("Static checks do not replace runtime multiplayer exploit testing.")
