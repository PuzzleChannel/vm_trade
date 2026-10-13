package com.puzzlechannel.vmtrade;

import java.util.UUID;

public final class TransactionGuardTest {
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        TransactionGuard.resetForTests();
        UUID player = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        require(TransactionGuard.allow(player, "buy", "payload-A", 10L), "first transaction should pass");
        require(!TransactionGuard.allow(player, "buy", "payload-A", 20L), "duplicate must be blocked");
        require(TransactionGuard.allow(player, "buy", "payload-A", 1_000_000_011L), "retry after replay window should pass");
        require(TransactionGuard.allow(player, "wallet", "payload-A", 1_000_000_012L), "different operations need independent keys");

        TransactionGuard.resetForTests();
        record CollisionInput(String value) {}
        String collisionA = TransactionGuard.fingerprint(new CollisionInput("Aa"));
        String collisionB = TransactionGuard.fingerprint(new CollisionInput("BB"));
        require(!collisionA.equals(collisionB), "different payload contents must not collide in the replay fingerprint");
        require(TransactionGuard.allow(player, "buy", collisionA, 100L), "first collision test payload should pass");
        require(TransactionGuard.allow(player, "buy", collisionB, 101L), "distinct payload with same Java String hash should pass");
        require(!TransactionGuard.allow(player, "buy", collisionA, 102L), "exact replay should still be blocked");

        require(LotPricingState.paymentAmount(100L, 6_400L, true) == 100L,
                "64 items listed for 100 cents must cost 100 cents total");
        require(LotPricingState.migrateLegacyUnitPrice(100, 64) == 6_400,
                "old 1-coin-per-item listing must migrate to 64 coins total for 64 items");
        require(LotPricingState.migrateLegacyUnitPrice(100, 1) == 100,
                "single-item listing price must stay unchanged during migration");

        require(LotPricingState.paymentAmount(100L, 6_400L, false) == 6_400L,
                "legacy per-item calculation must be preserved outside whole-lot purchases");
        require(LotPricingState.affordableQuantity(64L, 1L, true) == 64L,
                "a balance covering one full-lot price must allow all 64 items");
        require(LotPricingState.affordableQuantity(64L, 0L, true) == 0L,
                "a balance below the full-lot price must reject the purchase");
        require(LotPricingState.historyUnitPrice(100, 64, true) == 2,
                "history chart should retain a rounded per-item estimate for a whole-lot trade");

        TransactionGuard.resetForTests();
        UUID spammer = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        for (int i = 0; i < 12; i++) {
            require(TransactionGuard.allow(spammer, "buy", "payload-" + i, 5_000_000_000L + i), "requests under cap should pass");
        }
        require(!TransactionGuard.allow(spammer, "buy", "payload-13", 5_000_000_100L), "13th unique request per second must be blocked");
        require(TransactionGuard.allow(spammer, "buy", "payload-14", 6_000_000_001L), "rate window should reset");
        System.out.println("TransactionGuardTest: OK");
    }
}
