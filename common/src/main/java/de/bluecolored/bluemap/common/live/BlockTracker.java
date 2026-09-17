/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.common.live;

import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class BlockTracker implements AutoCloseable {

    private static final int MAX_EVENTS_PER_WORLD = 100_000;
    private static final int MAX_PERSIST_QUEUE_SIZE = 50_000;

    private volatile long serverStartTime;
    private final AtomicLong seqGenerator;
    private final Map<Key, WorldBlockHistory> worldHistories;

    private Path storageDir;
    private final ConcurrentLinkedQueue<PersistEntry> persistQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService persistExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "BlueMap-Timelapse-Writer");
        t.setDaemon(true);
        return t;
    });

    record PersistEntry(Key dimension, BlockRecord record) {}

    public BlockTracker() {
        this.serverStartTime = System.currentTimeMillis();
        this.seqGenerator = new AtomicLong(0);
        this.worldHistories = new ConcurrentHashMap<>();

        // Periodic async disk flush every 2 seconds
        this.persistExecutor.scheduleWithFixedDelay(this::drainQueueToDisk, 2, 2, TimeUnit.SECONDS);
    }

    public synchronized void init(Path baseDataDir) {
        if (baseDataDir == null) return;
        this.storageDir = baseDataDir.resolve("timelapse");
        try {
            Files.createDirectories(this.storageDir);
            loadPersistedData();
        } catch (Exception ex) {
            Logger.global.logError("Failed to initialize timelapse storage directory!", ex);
        }
    }

    private void loadPersistedData() {
        if (storageDir == null || !Files.exists(storageDir)) return;

        Path metadataFile = storageDir.resolve("metadata.json");
        if (Files.exists(metadataFile)) {
            try {
                String metaContent = Files.readString(metadataFile, StandardCharsets.UTF_8).trim();
                for (String part : metaContent.replace("{", "").replace("}", "").split(",")) {
                    String[] kv = part.split(":");
                    if (kv.length == 2) {
                        String k = kv[0].trim().replace("\"", "");
                        String v = kv[1].trim();
                        if (k.equals("serverStartTime")) {
                            this.serverStartTime = Long.parseLong(v);
                        } else if (k.equals("latestSeq")) {
                            long savedSeq = Long.parseLong(v);
                            this.seqGenerator.set(Math.max(this.seqGenerator.get(), savedSeq));
                        }
                    }
                }
            } catch (Exception ex) {
                Logger.global.logWarning("Could not parse timelapse metadata.json: " + ex.getMessage());
            }
        }

        // Load all .jsonl files in timelapse/
        try (var stream = Files.list(storageDir)) {
            stream.filter(p -> p.toString().endsWith(".jsonl")).forEach(path -> {
                String fileName = path.getFileName().toString();
                String dimKeyStr = fileName.substring(0, fileName.length() - 6).replace('_', ':');
                Key dimensionKey = Key.parse(dimKeyStr);
                WorldBlockHistory history = worldHistories.computeIfAbsent(dimensionKey, k -> new WorldBlockHistory());

                try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    String line;
                    long maxSeq = 0;
                    while ((line = reader.readLine()) != null) {
                        BlockRecord record = BlockRecord.fromJson(line);
                        if (record != null) {
                            history.add(record);
                            if (record.getSeq() > maxSeq) {
                                maxSeq = record.getSeq();
                            }
                        }
                    }
                    if (maxSeq > seqGenerator.get()) {
                        seqGenerator.set(maxSeq);
                    }
                } catch (Exception ex) {
                    Logger.global.logWarning("Failed to load timelapse events from " + fileName + ": " + ex.getMessage());
                }
            });
        } catch (Exception ex) {
            Logger.global.logWarning("Failed to scan timelapse directory: " + ex.getMessage());
        }

        Logger.global.logInfo("Loaded persistent timelapse history: seq=" + seqGenerator.get() + ", start=" + serverStartTime);
    }

    private synchronized void drainQueueToDisk() {
        if (storageDir == null || persistQueue.isEmpty()) return;

        Map<Key, List<BlockRecord>> grouped = new HashMap<>();
        PersistEntry entry;
        while ((entry = persistQueue.poll()) != null) {
            grouped.computeIfAbsent(entry.dimension, k -> new ArrayList<>()).add(entry.record);
        }

        for (var e : grouped.entrySet()) {
            Key dim = e.getKey();
            List<BlockRecord> list = e.getValue();
            if (list.isEmpty()) continue;

            String safeName = dim.getFormatted().replaceAll("[^a-zA-Z0-9._-]", "_") + ".jsonl";
            Path file = storageDir.resolve(safeName).normalize();
            if (!file.startsWith(storageDir.normalize())) {
                Logger.global.logWarning("Blocked potential path traversal in dimension name: " + safeName);
                continue;
            }

            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                StringBuilder sb = new StringBuilder();
                for (BlockRecord r : list) {
                    sb.setLength(0);
                    r.appendJson(sb);
                    writer.write(sb.toString());
                    writer.newLine();
                }
            } catch (Exception ex) {
                Logger.global.logWarning("Failed to persist timelapse records to " + safeName + ": " + ex.getMessage());
            }
        }

        saveMetadata();
    }

    private void saveMetadata() {
        if (storageDir == null) return;
        Path meta = storageDir.resolve("metadata.json");
        Path tmp = storageDir.resolve("metadata.json.tmp");
        String content = "{\"serverStartTime\":" + serverStartTime + ",\"latestSeq\":" + seqGenerator.get() + "}";
        try {
            Files.writeString(tmp, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(tmp, meta, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception fallback) {
                Files.move(tmp, meta, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            Logger.global.logWarning("Failed to atomic-write timelapse metadata: " + ex.getMessage());
        }
    }

    public synchronized void flush() {
        drainQueueToDisk();
    }

    public long getServerStartTime() {
        return serverStartTime;
    }

    public void addBlock(ServerWorld world, int x, int y, int z, String block, @Nullable String player, boolean placement) {
        addBlock(world, x, y, z, block, player, placement, null);
    }

    public void addBlock(ServerWorld world, int x, int y, int z, String block, @Nullable String player, boolean placement, @Nullable String message) {
        if (world == null) return;
        addBlock(world.getDimension(), x, y, z, block, player, placement, message);
    }

    public void addBlock(Key dimension, int x, int y, int z, String block, @Nullable String player, boolean placement) {
        addBlock(dimension, x, y, z, block, player, placement, null);
    }

    public void addBlock(Key dimension, int x, int y, int z, String block, @Nullable String player, boolean placement, @Nullable String message) {
        if (dimension == null) return;
        long seq = seqGenerator.incrementAndGet();
        long now = System.currentTimeMillis();
        BlockRecord record = new BlockRecord(seq, now, x, y, z, block, player, placement, message);
        WorldBlockHistory history = worldHistories.computeIfAbsent(dimension, k -> new WorldBlockHistory());
        history.add(record);

        // Queue for non-blocking asynchronous persistence with bounded memory protection
        if (persistQueue.size() >= MAX_PERSIST_QUEUE_SIZE) {
            persistQueue.poll();
        }
        persistQueue.add(new PersistEntry(dimension, record));
    }

    public String toJson(Key dimension, long sinceSeq) {
        long now = System.currentTimeMillis();
        long currentSeq = seqGenerator.get();

        WorldBlockHistory history = dimension != null ? worldHistories.get(dimension) : null;
        List<BlockRecord> records = history != null ? history.getSince(sinceSeq) : Collections.emptyList();

        StringBuilder sb = new StringBuilder(64 + records.size() * 64);
        sb.append("{\"serverStartTime\":").append(serverStartTime)
                .append(",\"currentTime\":").append(now)
                .append(",\"latestSeq\":").append(currentSeq)
                .append(",\"events\":[");

        for (int i = 0; i < records.size(); i++) {
            if (i > 0) sb.append(',');
            records.get(i).appendJson(sb);
        }

        sb.append("]}");
        return sb.toString();
    }

    @Override
    public void close() {
        persistExecutor.shutdown();
        try {
            if (!persistExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                persistExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            persistExecutor.shutdownNow();
        }
        flush();
    }

    private static class WorldBlockHistory {

        private final ArrayDeque<BlockRecord> deque = new ArrayDeque<>();

        public synchronized void add(BlockRecord record) {
            if (deque.size() >= MAX_EVENTS_PER_WORLD) {
                deque.pollFirst();
            }
            deque.addLast(record);
        }

        public synchronized List<BlockRecord> getSince(long sinceSeq) {
            if (sinceSeq <= 0) {
                return new ArrayList<>(deque);
            }

            List<BlockRecord> result = new ArrayList<>();
            for (BlockRecord record : deque) {
                if (record.getSeq() > sinceSeq) {
                    result.add(record);
                }
            }
            return result;
        }

    }

}
