package com.puzzlechannel.vmtrade;

import net.minecraft.server.level.ServerPlayer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded replay and flood guard for state-changing server packets. */
public final class TransactionGuard {
    private static final long DEDUPE_WINDOW_NANOS = 1_000_000_000L;
    private static final long RATE_WINDOW_NANOS = 1_000_000_000L;
    private static final long PLAYER_IDLE_NANOS = 60_000_000_000L;
    private static final int MAX_REQUESTS_PER_SECOND = 12;
    private static final int MAX_KEYS_PER_PLAYER = 64;
    private static final int MAX_TRACKED_PLAYERS = 2048;
    private static final LinkedHashMap<UUID, PlayerState> PLAYERS = new LinkedHashMap<>(64, 0.75f, true);

    private TransactionGuard() {}

    public static boolean allow(ServerPlayer player, String operation, Object payload) {
        if (player == null || operation == null || operation.isBlank() || payload == null) return false;
        String text;
        try { text = payload.toString(); }
        catch (RuntimeException exception) { return false; }
        String fingerprint = payload.getClass().getName() + ":" + Integer.toHexString(text.hashCode()) + ":" + text.length();
        return allow(player.getUUID(), operation, fingerprint, System.nanoTime());
    }

    static boolean allow(UUID playerId, String operation, String fingerprint, long nowNanos) {
        if (playerId == null || operation == null || operation.isBlank()
                || fingerprint == null || fingerprint.isBlank()) return false;
        String key = operation + "|" + fingerprint;
        synchronized (PLAYERS) {
            PlayerState state = PLAYERS.get(playerId);
            if (state == null) {
                state = new PlayerState();
                PLAYERS.put(playerId, state);
            }
            state.lastSeen = nowNanos;
            Iterator<Map.Entry<String, Long>> recent = state.recent.entrySet().iterator();
            while (recent.hasNext()) {
                long seenAt = recent.next().getValue();
                if (nowNanos >= seenAt && nowNanos - seenAt > DEDUPE_WINDOW_NANOS) recent.remove();
            }
            Long previous = state.recent.get(key);
            if (previous != null && (nowNanos < previous || nowNanos - previous < DEDUPE_WINDOW_NANOS)) return false;

            if (!state.windowInitialized || nowNanos < state.windowStart
                    || nowNanos - state.windowStart >= RATE_WINDOW_NANOS) {
                state.windowInitialized = true;
                state.windowStart = nowNanos;
                state.acceptedInWindow = 0;
            }
            if (state.acceptedInWindow >= MAX_REQUESTS_PER_SECOND) return false;

            state.acceptedInWindow++;
            state.recent.put(key, nowNanos);
            while (state.recent.size() > MAX_KEYS_PER_PLAYER) {
                Iterator<String> keys = state.recent.keySet().iterator();
                if (keys.hasNext()) { keys.next(); keys.remove(); } else break;
            }
            Iterator<Map.Entry<UUID, PlayerState>> players = PLAYERS.entrySet().iterator();
            while (players.hasNext()) {
                Map.Entry<UUID, PlayerState> entry = players.next();
                if (entry.getKey().equals(playerId)) continue;
                long seenAt = entry.getValue().lastSeen;
                if (nowNanos >= seenAt && nowNanos - seenAt > PLAYER_IDLE_NANOS) players.remove();
            }
            while (PLAYERS.size() > MAX_TRACKED_PLAYERS) {
                Iterator<UUID> oldest = PLAYERS.keySet().iterator();
                if (oldest.hasNext()) { oldest.next(); oldest.remove(); } else break;
            }
            return true;
        }
    }

    static void resetForTests() {
        synchronized (PLAYERS) { PLAYERS.clear(); }
    }

    private static final class PlayerState {
        private final LinkedHashMap<String, Long> recent = new LinkedHashMap<>(16, 0.75f, true);
        private long windowStart;
        private int acceptedInWindow;
        private long lastSeen;
        private boolean windowInitialized;
    }
}
