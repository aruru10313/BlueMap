package de.bluecolored.bluemap.core.map;

import de.bluecolored.bluemap.core.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;

final class TerritoryChunkFilter {
    private static final Path CLAIMS_FILE = Path.of("config", "territory", "claims.txt");
    private static final long RELOAD_INTERVAL_NANOS = 1_000_000_000L;
    private static volatile long loadedAt;
    private static volatile Set<String> claims = Set.of();

    private TerritoryChunkFilter() {}

    static BiPredicate<Integer, Integer> forWorld(String worldId) {
        return (x, z) -> isClaimed(worldId, x, z);
    }

    private static boolean isClaimed(String worldId, int x, int z) {
        reloadIfNeeded();
        String coordinate = "|" + x + "," + z;
        for (String claim : claims) {
            if (claim.endsWith(coordinate) && worldMatches(worldId, claim)) return true;
        }
        return false;
    }

    private static boolean worldMatches(String worldId, String claim) {
        int separator = claim.indexOf('|');
        if (separator < 0) return false;
        String dimension = claim.substring(0, separator);
        return worldId.contains(dimension) || worldId.contains(dimension.replace("minecraft:", ""));
    }

    private static synchronized void reloadIfNeeded() {
        long now = System.nanoTime();
        if (now - loadedAt < RELOAD_INTERVAL_NANOS) return;
        loadedAt = now;
        if (!Files.exists(CLAIMS_FILE)) {
            claims = Set.of();
            return;
        }
        try {
            Set<String> loaded = new HashSet<>();
            for (String line : Files.readAllLines(CLAIMS_FILE)) {
                String value = line.trim();
                if (!value.isEmpty() && !value.startsWith("#")) loaded.add(value);
            }
            claims = Set.copyOf(loaded);
        } catch (IOException ex) {
            Logger.global.logError("Failed to load territory claims from " + CLAIMS_FILE, ex);
        }
    }
}
