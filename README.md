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

## Currency stack size

VМуткоин (ВК) has a maximum stack size of 1000 per slot so large market balances occupy less inventory space.

## Build and release checks

Use Java 25 and run these from the repository root:

```bash
./gradlew --no-daemon clean build verifyJar
python tooling/verify_static.py
python tooling/verify_release.py
python tooling/verify_safety.py
```

The release checks verify the mod metadata/version, required resources and mixins, absence of development stubs, duplicate ZIP entries, and byte-for-byte preservation of critical Open Trade classes. The checks are also run by GitHub Actions on pushes and pull requests.

These are archive-integrity and regression checks, not proof against every in-game exploit. Before deploying an update, test on a backup world with two clients: simultaneous purchase of one listing, repeated clicks/packets, cancellation followed by relog, full inventory, disconnect during purchase, and server restart while listings/deliveries exist.
