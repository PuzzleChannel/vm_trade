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
