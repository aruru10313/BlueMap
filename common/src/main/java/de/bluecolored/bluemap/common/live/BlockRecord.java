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

import org.jetbrains.annotations.Nullable;

public class BlockRecord {

    private final long seq;
    private final long timestamp;
    private final int x;
    private final int y;
    private final int z;
    private final String block;
    private final @Nullable String player;
    private final boolean placement;

    public BlockRecord(long seq, long timestamp, int x, int y, int z, String block, @Nullable String player, boolean placement) {
        this.seq = seq;
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
        this.player = player;
        this.placement = placement;
    }

    public long getSeq() {
        return seq;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getBlock() {
        return block;
    }

    public @Nullable String getPlayer() {
        return player;
    }

    public boolean isPlacement() {
        return placement;
    }

    public void appendJson(StringBuilder sb) {
        sb.append("{\"seq\":").append(seq)
                .append(",\"t\":").append(timestamp)
                .append(",\"x\":").append(x)
                .append(",\"y\":").append(y)
                .append(",\"z\":").append(z)
                .append(",\"b\":").append(escapeJson(block))
                .append(",\"a\":").append(placement ? "\"place\"" : "\"break\"");
        if (player != null) {
            sb.append(",\"p\":").append(escapeJson(player));
        }
        sb.append('}');
    }

    public static @Nullable BlockRecord fromJson(String line) {
        if (line == null || line.isBlank() || !line.startsWith("{")) return null;
        try {
            long seq = 0;
            long timestamp = 0;
            int x = 0, y = 0, z = 0;
            String block = "minecraft:air";
            String player = null;
            boolean placement = true;

            String trimmed = line.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            String[] parts = trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String part : parts) {
                int colon = part.indexOf(':');
                if (colon == -1) continue;
                String k = part.substring(0, colon).trim().replace("\"", "");
                String v = part.substring(colon + 1).trim();
                if (v.startsWith("\"") && v.endsWith("\"")) {
                    v = v.substring(1, v.length() - 1);
                }
                switch (k) {
                    case "seq" -> seq = Long.parseLong(v);
                    case "t" -> timestamp = Long.parseLong(v);
                    case "x" -> x = Integer.parseInt(v);
                    case "y" -> y = Integer.parseInt(v);
                    case "z" -> z = Integer.parseInt(v);
                    case "b" -> block = v;
                    case "p" -> player = v.equals("null") ? null : v;
                    case "a" -> placement = !"break".equalsIgnoreCase(v);
                }
            }
            return new BlockRecord(seq, timestamp, x, y, z, block, player, placement);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

}
