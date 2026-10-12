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
        UUID spammer = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        for (int i = 0; i < 12; i++) {
            require(TransactionGuard.allow(spammer, "buy", "payload-" + i, 5_000_000_000L + i), "requests under cap should pass");
        }
        require(!TransactionGuard.allow(spammer, "buy", "payload-13", 5_000_000_100L), "13th unique request per second must be blocked");
        require(TransactionGuard.allow(spammer, "buy", "payload-14", 6_000_000_001L), "rate window should reset");
        System.out.println("TransactionGuardTest: OK");
    }
}
