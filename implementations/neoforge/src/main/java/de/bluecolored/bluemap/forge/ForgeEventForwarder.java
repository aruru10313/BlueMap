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
package de.bluecolored.bluemap.forge;

import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ForgeEventForwarder {

    private final Collection<ServerEventListener> eventListeners;
    private ForgeMod forgeMod;

    public ForgeEventForwarder() {
        this(null);
    }

    public ForgeEventForwarder(ForgeMod forgeMod) {
        this.eventListeners = new ArrayList<>(1);
        this.forgeMod = forgeMod;
    }

    public void setForgeMod(ForgeMod forgeMod) {
        this.forgeMod = forgeMod;
    }

    public synchronized void addEventListener(ServerEventListener listener) {
        this.eventListeners.add(listener);
    }

    public synchronized void removeAllListeners() {
        this.eventListeners.clear();
    }

    @SubscribeEvent
    public synchronized void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        UUID uuid = evt.getEntity().getUUID();
        for (ServerEventListener listener : eventListeners) listener.onPlayerJoin(uuid);
    }

    @SubscribeEvent
    public synchronized void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        UUID uuid = evt.getEntity().getUUID();
        for (ServerEventListener listener : eventListeners) listener.onPlayerLeave(uuid);
    }

    @SubscribeEvent
    public synchronized void onBlockPlace(BlockEvent.EntityPlaceEvent evt) {
        if (evt.isCanceled() || forgeMod == null) return;
        if (!(evt.getLevel() instanceof ServerLevel level)) return;
        ServerWorld world = forgeMod.getServerWorld(level);
        if (world == null) return;
        BlockPos pos = evt.getPos();
        String blockId = BuiltInRegistries.BLOCK.getKey(evt.getPlacedBlock().getBlock()).toString();
        String player = null;
        if (evt.getEntity() instanceof Player p) {
            player = p.getGameProfile().getName();
        } else if (evt.getEntity() != null) {
            player = evt.getEntity().getDisplayName().getString();
        } else {
            Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8.0, false);
            if (nearest != null) {
                player = nearest.getGameProfile().getName();
            }
        }

        for (ServerEventListener listener : eventListeners) {
            listener.onBlockChange(world, pos.getX(), pos.getY(), pos.getZ(), blockId, player, true);
        }

        // Handle multi-place events (e.g. Create large water wheel, multi-tanks, tracks, beds)
        if (evt instanceof BlockEvent.EntityMultiPlaceEvent multiEvt) {
            for (var snapshot : multiEvt.getReplacedBlockSnapshots()) {
                BlockPos snapPos = snapshot.getPos();
                if (!snapPos.equals(pos)) {
                    String snapBlockId = BuiltInRegistries.BLOCK.getKey(snapshot.getCurrentState().getBlock()).toString();
                    if ("minecraft:air".equals(snapBlockId)) {
                        snapBlockId = BuiltInRegistries.BLOCK.getKey(evt.getPlacedBlock().getBlock()).toString();
                    }
                    for (ServerEventListener listener : eventListeners) {
                        listener.onBlockChange(world, snapPos.getX(), snapPos.getY(), snapPos.getZ(), snapBlockId, player, true);
                    }
                }
            }
        }

        // Check if placed block is a sign, delay read text after player closes GUI
        if (evt.getPlacedBlock().getBlock() instanceof net.minecraft.world.level.block.SignBlock || blockId.contains("sign")) {
            final String finalPlayer = player;
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    level.getServer().execute(() -> {
                        if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
                            String text = extractSignText(sign);
                            if (text != null) {
                                for (ServerEventListener listener : eventListeners) {
                                    listener.onBlockChange(world, pos.getX(), pos.getY(), pos.getZ(), blockId, finalPlayer, true, text);
                                }
                            }
                        }
                    });
                }
            }, 2500);
        }
    }

    @SubscribeEvent
    public synchronized void onBlockBreak(BlockEvent.BreakEvent evt) {
        if (evt.isCanceled() || forgeMod == null) return;
        if (!(evt.getLevel() instanceof ServerLevel level)) return;
        ServerWorld world = forgeMod.getServerWorld(level);
        if (world == null) return;
        BlockPos pos = evt.getPos();
        String blockId = BuiltInRegistries.BLOCK.getKey(evt.getState().getBlock()).toString();
        String player = null;
        if (evt.getPlayer() != null) {
            player = evt.getPlayer().getGameProfile().getName();
        }
        for (ServerEventListener listener : eventListeners) {
            listener.onBlockChange(world, pos.getX(), pos.getY(), pos.getZ(), blockId, player, false);
        }
    }

    @SubscribeEvent
    public synchronized void onExplosion(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate evt) {
        if (forgeMod == null) return;
        if (!(evt.getLevel() instanceof ServerLevel level)) return;
        ServerWorld world = forgeMod.getServerWorld(level);
        if (world == null) return;

        String player = "Explosion";
        var explosion = evt.getExplosion();
        if (explosion != null) {
            if (explosion.getIndirectSourceEntity() instanceof Player p) {
                player = p.getGameProfile().getName();
            } else if (explosion.getDirectSourceEntity() instanceof Player p) {
                player = p.getGameProfile().getName();
            } else if (explosion.getDirectSourceEntity() != null) {
                player = explosion.getDirectSourceEntity().getType().getDescription().getString();
            }
        }

        for (BlockPos pos : evt.getAffectedBlocks()) {
            String blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
            for (ServerEventListener listener : eventListeners) {
                listener.onBlockChange(world, pos.getX(), pos.getY(), pos.getZ(), blockId, player, false);
            }
        }
    }

    @SubscribeEvent
    public synchronized void onRightClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock evt) {
        if (forgeMod == null) return;
        if (!(evt.getLevel() instanceof ServerLevel level)) return;
        ServerWorld world = forgeMod.getServerWorld(level);
        if (world == null) return;
        BlockPos pos = evt.getPos();
        final String prevBlockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
        final String player = evt.getEntity().getGameProfile().getName();

        if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
            String text = extractSignText(sign);
            if (text != null) {
                for (ServerEventListener listener : eventListeners) {
                    listener.onBlockChange(world, pos.getX(), pos.getY(), pos.getZ(), prevBlockId, player, true, text);
                }
            }
        }

        // Detect Create mod block conversions (e.g. casing applied to shafts/cogwheels, wrench modifications)
        level.getServer().execute(() -> {
            String newBlockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
            if (!newBlockId.equals(prevBlockId)) {
                for (ServerEventListener listener : eventListeners) {
                    listener.onBlockChange(world, pos.getX(), pos.getY(), pos.getZ(), newBlockId, player, true);
                }
            }
        });
    }

    private String extractSignText(net.minecraft.world.level.block.entity.SignBlockEntity sign) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String l = sign.getFrontText().getMessage(i, false).getString().trim();
            if (!l.isEmpty()) lines.add(l);
        }
        for (int i = 0; i < 4; i++) {
            String l = sign.getBackText().getMessage(i, false).getString().trim();
            if (!l.isEmpty()) lines.add("(뒤) " + l);
        }
        return lines.isEmpty() ? null : String.join(" | ", lines);
    }

}
