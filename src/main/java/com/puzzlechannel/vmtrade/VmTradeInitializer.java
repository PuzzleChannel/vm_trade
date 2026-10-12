package com.puzzlechannel.vmtrade;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.core.Registry;

public final class VmTradeInitializer implements ModInitializer {
    public static final String MOD_ID = "vmtrade";
    public static final ResourceKey<Item> VMCOIN_KEY = ResourceKey.create(Registries.ITEM, id("vmcoin"));
    public static final ResourceKey<Block> MARKET_KEY = ResourceKey.create(Registries.BLOCK, id("market"));
    public static Item VMCOIN;
    public static Block MARKET;
    public static Item MARKET_ITEM;
    public static BlockEntityType<?> MARKET_BE_TYPE;

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static net.minecraft.core.Registry registry(String fieldName) {
        try {
            java.lang.reflect.Field field = Class.forName("net.minecraft.core.registries.BuiltInRegistries").getField(fieldName);
            return (net.minecraft.core.Registry) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to access BuiltInRegistries." + fieldName, e);
        }
    }

    @Override
    public void onInitialize() {
        registerCoin();
        registerMarket();
        forceVmCoinCurrency();
        installOriginalOpenTrade();
        installLegacySalesHistory();
        includeMarketInTradeChestBlockEntity();
        registerMarketBlockEntityType();
        installCreativeEntries();
    }

    private static void registerCoin() {
        VMCOIN = Registry.register(registry("ITEM"), VMCOIN_KEY,
                new Item(new Item.Properties().setId(VMCOIN_KEY).stacksTo(1000)));
    }

    private static void registerMarket() {
        MARKET = Registry.register(registry("BLOCK"), MARKET_KEY,
                new MarketBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST)
                        .setId(MARKET_KEY)
                        .strength(-1.0f, 3600000.0f)
                        .pushReaction(PushReaction.BLOCK)));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id("market"));
        MARKET_ITEM = Registry.register(registry("ITEM"), itemKey,
                new BlockItem(MARKET, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)));
    }

    private static void forceVmCoinCurrency() {
        try {
            Class<?> cfg = Class.forName("net.onlyjugs.OpenTradeConfig");
            java.lang.reflect.Field itemField = cfg.getDeclaredField("currencyItem");
            java.lang.reflect.Field idField = cfg.getDeclaredField("currencyId");
            java.lang.reflect.Field loadedField = cfg.getDeclaredField("loaded");
            itemField.setAccessible(true);
            idField.setAccessible(true);
            loadedField.setAccessible(true);
            itemField.set(null, VMCOIN);
            idField.set(null, MOD_ID + ":vmcoin");
            loadedField.setBoolean(null, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to set VMCoin as market currency", e);
        }
    }

    /**
     * Register a dedicated BlockEntityType for the Market.
     *
     * The original Open Trade access widener deliberately exposes the
     * BlockEntityType constructor and supplier interface, so this uses that
     * stable API directly instead of reflective construction. The dedicated
     * type has no client renderer registered, preventing the vanilla chest
     * renderer from drawing an opening chest inside the Market model.
     */
    private static void registerMarketBlockEntityType() {
        BlockEntityType.BlockEntitySupplier<MarketBlockEntity> supplier =
                MarketBlockEntity::new;
        BlockEntityType<MarketBlockEntity> type = new BlockEntityType<>(
                supplier,
                java.util.Set.of(MARKET));
        ResourceKey<BlockEntityType<?>> key =
                (ResourceKey<BlockEntityType<?>>) (ResourceKey<?>) ResourceKey.create(
                        Registries.BLOCK_ENTITY_TYPE, id("market"));
        MARKET_BE_TYPE = Registry.register(
                (Registry) registry("BLOCK_ENTITY_TYPE"),
                (ResourceKey) key,
                type);
    }

    /**
     * TradeChestBlockEntity hard-codes the original Open Trade BlockEntityType in its
     * constructor. We intentionally reuse that type for the VM Market, so the vanilla
     * Open Trade menu/network/listing implementation remains authoritative. Minecraft
     * keeps the type's valid-block set private and initially contains only trade_chest;
     * expand that set after Open Trade registers it so VM Market blocks are accepted by
     * the same BlockEntityType.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void includeMarketInTradeChestBlockEntity() {
        try {
            Class<?> blocksClass = Class.forName("net.onlyjugs.block.OpenTradeBlocks");
            Object tradeChestType = blocksClass.getField("TRADE_CHEST_BE").get(null);
            if (tradeChestType == null) {
                throw new IllegalStateException("Open Trade TradeChestBlockEntity type was not initialized");
            }

            Class<?> typeClass = Class.forName("net.minecraft.world.level.block.entity.BlockEntityType");
            java.lang.reflect.Field validBlocksField = null;
            // BlockEntityType also contains static Set constants. Never touch those; the
            // instance field we need is the private Set<Block> validBlocks. Prefer the
            // mapping-independent field name when available, otherwise select the first
            // non-static Set field whose value is a Set on this BlockEntityType instance.
            for (java.lang.reflect.Field field : typeClass.getDeclaredFields()) {
                if (!java.util.Set.class.isAssignableFrom(field.getType())
                        || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                if ("validBlocks".equals(field.getName())) {
                    validBlocksField = field;
                    break;
                }
                Object value = field.get(tradeChestType);
                if (value instanceof java.util.Set) {
                    validBlocksField = field;
                }
            }
            if (validBlocksField == null) {
                throw new NoSuchFieldException("BlockEntityType validBlocks Set");
            }

            java.util.Set current = (java.util.Set) validBlocksField.get(tradeChestType);
            if (current != null && current.contains(MARKET)) {
                return;
            }

            java.util.Set expanded = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            if (current != null) expanded.addAll(current);
            expanded.add(MARKET);
            java.util.Set replacement = java.util.Collections.unmodifiableSet(expanded);

            try {
                validBlocksField.set(tradeChestType, replacement);
            } catch (IllegalAccessException | RuntimeException ex) {
                // Java 25 may refuse reflective writes to a final field. Fall back to Unsafe.
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                java.lang.reflect.Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                Object unsafe = unsafeField.get(null);
                java.lang.reflect.Method staticFieldOffset = unsafeClass.getMethod("objectFieldOffset", java.lang.reflect.Field.class);
                java.lang.reflect.Method putObject = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);
                long offset = ((Number) staticFieldOffset.invoke(unsafe, validBlocksField)).longValue();
                putObject.invoke(unsafe, tradeChestType, offset, replacement);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to register VM Market with Open Trade's TradeChestBlockEntity type", e);
        }
    }


    /** Add VM Trade items to the normal vanilla Creative tabs using the Fabric 26.2 API. */
    private static void installCreativeEntries() {
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
                .modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS)
                .register(output -> output.prepend(MARKET_ITEM));

        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
                .modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.INGREDIENTS)
                .register(output -> output.prepend(VMCOIN));
    }

    /**
     * Restore the previous release's player sales history and delivery-sync feature.
     * These classes are shipped from the last complete VM Trade UI bundle so the
     * original Open Trade screens can remain untouched.
     */
    private static void installLegacySalesHistory() {
        try {
            Class.forName("com.puzzlechannel.vmtrade.TradeSyncService")
                    .getMethod("install").invoke(null);
            Class<?> serverClass = Class.forName("net.minecraft.server.MinecraftServer");
            Class.forName("com.puzzlechannel.vmtrade.SaleHistoryStore")
                    .getMethod("initialize", serverClass).invoke(null, new Object[]{null});
            Class.forName("com.puzzlechannel.vmtrade.TradeHistoryNetwork")
                    .getMethod("registerServer").invoke(null);
        } catch (ReflectiveOperationException e) {
            Throwable cause = e instanceof java.lang.reflect.InvocationTargetException invocation
                    && invocation.getCause() != null ? invocation.getCause() : e;
            throw new RuntimeException("Unable to restore VM Trade sales history", cause);
        }
    }

    private static void installOriginalOpenTrade() {
        try {
            Object original = Class.forName("net.onlyjugs.fabric.OpenTradeFabric").getConstructor().newInstance();
            original.getClass().getMethod("onInitialize").invoke(original);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to initialize the restored Open Trade engine", e);
        }
    }
}
