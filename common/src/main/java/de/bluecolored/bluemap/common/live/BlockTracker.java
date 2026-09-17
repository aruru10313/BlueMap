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
import de.bluecolored.bluemap.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BlockTracker {

    private static final int MAX_EVENTS_PER_WORLD = 100_000;

    private final long serverStartTime;
    private final AtomicLong seqGenerator;
    private final Map<Key, WorldBlockHistory> worldHistories;

    public BlockTracker() {
        this.serverStartTime = System.currentTimeMillis();
        this.seqGenerator = new AtomicLong(0);
        this.worldHistories = new ConcurrentHashMap<>();
    }

    public long getServerStartTime() {
        return serverStartTime;
    }

    public void addBlock(ServerWorld world, int x, int y, int z, String block, @Nullable String player, boolean placement) {
        if (world == null) return;
        addBlock(world.getDimension(), x, y, z, block, player, placement);
    }

    public void addBlock(Key dimension, int x, int y, int z, String block, @Nullable String player, boolean placement) {
        if (dimension == null) return;
        long seq = seqGenerator.incrementAndGet();
        long now = System.currentTimeMillis();
        BlockRecord record = new BlockRecord(seq, now, x, y, z, block, player, placement);
        WorldBlockHistory history = worldHistories.computeIfAbsent(dimension, k -> new WorldBlockHistory());
        history.add(record);
    }

    public String toJson(Key dimension, long sinceSeq) {
        long now = System.currentTimeMillis();
        long currentSeq = seqGenerator.get();

        WorldBlockHistory history = worldHistories.get(dimension);
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
