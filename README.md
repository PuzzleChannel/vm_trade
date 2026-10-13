# VM Trade — Fabric 26.2

Resource-safe Russian localization and VM Trade rework around the pristine Open Trade 1.4.2 Fabric binary.

## Localization policy

The original Open Trade bytecode is copied unchanged. No JVM constant-pool patching or vendor class rewriting is used. UI text is translated at runtime by isolated Mixins that redirect only Component.literal calls; vendor class files in the JAR remain unchanged.

Russian resources are provided under both namespaces:
- `assets/opentrade/lang/ru_ru.json` for the original Open Trade translation keys.
- `assets/vmtrade/lang/ru_ru.json` for VM Trade content.

VMCoin is displayed as `ВМуткоин (ВК)` and the static wallet buttons use `ВК`.

Original Open Trade UI strings that are hard-coded with `Component.literal(...)` are translated through safe runtime `@Redirect` Mixins. The stored vendor classes remain byte-for-byte intact; no class-file constant-pool rewriting is performed.

## Build policy

The release build copies the pristine vendor Open Trade classes unchanged and only adds VM Trade classes/resources while excluding the original Convert-button mixin and NeoForge loader content.

## Sell listing price semantics

A sell listing price is the **total price for the full stack in that slot**, not the price of each item. For example, a stack of 64 iron ingots listed for 100 cents (1 VМуткоин) is purchased as 64 ingots for 1 VМуткоин total. Market clicks purchase the complete lot, and the server rejects partial-quantity purchase packets. The item-price history chart continues to show a rounded per-item estimate because its legacy format stores whole cents.

Buy orders retain their existing per-item price and quantity semantics. Automatic matching therefore skips multi-item sell lots instead of applying a per-item buy order to a whole-lot price; one-item listings continue to match normally.

Existing listings are migrated once when their trade chest is next loaded: the previous per-item price is multiplied by the current stack count to preserve the old total value. Sellers can then cancel and relist a stack at their preferred total price.

## Currency stack size

VМуткоин (ВК) has a maximum stack size of 1000 per slot so large market balances occupy less inventory space.

## Build and release checks

Use Java 25 and run these from the repository root:

```bash
./gradlew --no-daemon clean build verifyJar
python tooling/verify_static.py
python tooling/verify_release.py
python tooling/verify_safety.py
python tooling/verify_translation.py
```

The release checks verify the mod metadata/version, required resources and mixins, absence of development stubs, duplicate ZIP entries, and byte-for-byte preservation of critical Open Trade classes. The checks are also run by GitHub Actions on pushes and pull requests.

These are archive-integrity and regression checks, not proof against every in-game exploit. Before deploying an update, test on a backup world with two clients: simultaneous purchase of one listing, repeated clicks/packets, cancellation followed by relog, full inventory, disconnect during purchase, and server restart while listings/deliveries exist.
