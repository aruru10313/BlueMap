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
import {
    BoxGeometry,
    Color,
    DynamicDrawUsage,
    Group,
    InstancedMesh,
    LineBasicMaterial,
    LineSegments,
    Matrix4,
    MeshLambertMaterial,
    WireframeGeometry
} from "three";
import {reactive} from "vue";
import {alert} from "../util/Utils";

const BLOCK_COLORS = {
    "minecraft:stone": 0x757575,
    "minecraft:granite": 0x9a6a57,
    "minecraft:polished_granite": 0x9a6a57,
    "minecraft:diorite": 0xbdbdbd,
    "minecraft:polished_diorite": 0xbdbdbd,
    "minecraft:andesite": 0x858585,
    "minecraft:polished_andesite": 0x858585,
    "minecraft:grass_block": 0x5b8f39,
    "minecraft:dirt": 0x866043,
    "minecraft:coarse_dirt": 0x77553b,
    "minecraft:podzol": 0x5d3f23,
    "minecraft:cobblestone": 0x6e6e6e,
    "minecraft:mossy_cobblestone": 0x596b52,
    "minecraft:oak_planks": 0xa2824e,
    "minecraft:spruce_planks": 0x674d30,
    "minecraft:birch_planks": 0xc5b47a,
    "minecraft:jungle_planks": 0xa07455,
    "minecraft:acacia_planks": 0xa85a32,
    "minecraft:dark_oak_planks": 0x432a15,
    "minecraft:mangrove_planks": 0x763631,
    "minecraft:cherry_planks": 0xe29da8,
    "minecraft:bamboo_planks": 0xc3ac53,
    "minecraft:crimson_planks": 0x653043,
    "minecraft:warped_planks": 0x2b6863,
    "minecraft:bedrock": 0x2b2b2b,
    "minecraft:sand": 0xdbce97,
    "minecraft:red_sand": 0xba632b,
    "minecraft:gravel": 0x837f7e,
    "minecraft:gold_ore": 0x918873,
    "minecraft:deepslate_gold_ore": 0x524e47,
    "minecraft:iron_ore": 0x8c7c72,
    "minecraft:deepslate_iron_ore": 0x554c46,
    "minecraft:coal_ore": 0x6e6e6e,
    "minecraft:deepslate_coal_ore": 0x454545,
    "minecraft:nether_gold_ore": 0x733633,
    "minecraft:lapis_ore": 0x60697f,
    "minecraft:deepslate_lapis_ore": 0x414652,
    "minecraft:diamond_ore": 0x5e8c8a,
    "minecraft:deepslate_diamond_ore": 0x3d5453,
    "minecraft:redstone_ore": 0x8a5b5b,
    "minecraft:deepslate_redstone_ore": 0x544040,
    "minecraft:emerald_ore": 0x588863,
    "minecraft:deepslate_emerald_ore": 0x3d5444,
    "minecraft:oak_log": 0x6b5331,
    "minecraft:spruce_log": 0x3b2712,
    "minecraft:birch_log": 0xdcd5ca,
    "minecraft:jungle_log": 0x56431e,
    "minecraft:acacia_log": 0x68625b,
    "minecraft:dark_oak_log": 0x362818,
    "minecraft:mangrove_log": 0x542621,
    "minecraft:cherry_log": 0x331c23,
    "minecraft:oak_leaves": 0x336b1d,
    "minecraft:spruce_leaves": 0x274327,
    "minecraft:birch_leaves": 0x5e7939,
    "minecraft:jungle_leaves": 0x36631b,
    "minecraft:acacia_leaves": 0x476326,
    "minecraft:dark_oak_leaves": 0x234415,
    "minecraft:mangrove_leaves": 0x526b2b,
    "minecraft:cherry_leaves": 0xe28da5,
    "minecraft:glass": 0xaae7ff,
    "minecraft:white_wool": 0xe9ecec,
    "minecraft:orange_wool": 0xf07613,
    "minecraft:magenta_wool": 0xbd44b3,
    "minecraft:light_blue_wool": 0x3aaad8,
    "minecraft:yellow_wool": 0xf8c627,
    "minecraft:lime_wool": 0x70b919,
    "minecraft:pink_wool": 0xed8dac,
    "minecraft:gray_wool": 0x3e4447,
    "minecraft:light_gray_wool": 0x8e8e86,
    "minecraft:cyan_wool": 0x158991,
    "minecraft:purple_wool": 0x792aac,
    "minecraft:blue_wool": 0x35399d,
    "minecraft:brown_wool": 0x724728,
    "minecraft:green_wool": 0x546d1b,
    "minecraft:red_wool": 0xa12722,
    "minecraft:black_wool": 0x141519,
    "minecraft:gold_block": 0xf6d03d,
    "minecraft:iron_block": 0xd8d8d8,
    "minecraft:diamond_block": 0x62e3dd,
    "minecraft:netherite_block": 0x443a3b,
    "minecraft:bricks": 0x966153,
    "minecraft:bookshelf": 0x745634,
    "minecraft:obsidian": 0x14121d,
    "minecraft:crying_obsidian": 0x230c3d,
    "minecraft:water": 0x3f76e4,
    "minecraft:lava": 0xe06100,
    "minecraft:ice": 0x90b9fa,
    "minecraft:snow_block": 0xf0fbfb,
    "minecraft:clay": 0xa0a6b3,
    "minecraft:netherrack": 0x652828,
    "minecraft:soul_sand": 0x514035,
    "minecraft:soul_soil": 0x4b3b31,
    "minecraft:glowstone": 0x8f7647,
    "minecraft:end_stone": 0xddde9d,
    "minecraft:purpur_block": 0xa87ca8,
    "minecraft:prismarine": 0x639c97,
    "minecraft:dark_prismarine": 0x335b4b,
    "minecraft:sea_lantern": 0xabc8bf,
    "minecraft:terracotta": 0x985e44,
    "minecraft:white_concrete": 0xcfcfd1,
    "minecraft:red_concrete": 0x8e2020,
    "minecraft:blue_concrete": 0x2c2e8f,
    "minecraft:black_concrete": 0x080a0f,
    "minecraft:tnt": 0xdb4437
};

function getBlockColor(blockId) {
    if (!blockId) return 0xaaaaaa;
    let baseId = blockId.split("[")[0].toLowerCase();
    if (BLOCK_COLORS[baseId]) return BLOCK_COLORS[baseId];

    // Fallback: fast hash to pleasant HSL color
    let hash = 0;
    for (let i = 0; i < baseId.length; i++) {
        hash = (hash << 5) - hash + baseId.charCodeAt(i);
        hash |= 0;
    }
    let hue = Math.abs(hash) % 360;
    let color = new Color();
    color.setHSL(hue / 360, 0.45, 0.55);
    return color.getHex();
}

export class BlockManager {

    /**
     * @param rootGroup {Group} - Group to add Three.js meshes to
     * @param fileUrl {string} - Base URL to live/blocks.json
     * @param events {EventTarget}
     * @param mapViewer {any}
     */
    constructor(rootGroup, fileUrl, events = null, mapViewer = null) {
        this.rootGroup = rootGroup;
        this.fileUrl = fileUrl;
        this.events = events;
        this.mapViewer = mapViewer;
        this.disposed = false;

        this.data = reactive({
            active: true,
            timelapseOpen: false,
            isPlaying: false,
            speed: 5,
            progress: 1.0,
            currentTime: Date.now(),
            serverStartTime: Date.now(),
            serverEndTime: Date.now(),
            totalEvents: 0,
            renderedBlocks: 0,
            recentPlacedCount: 0,
            autoFollow: false,
            latestPlayer: null,
            latestBlock: null
        });

        // Event records storage
        this.eventsList = [];
        this.latestSeq = 0;

        // Visual containers
        this.sceneGroup = new Group();
        this.sceneGroup.name = "bm-live-blocks";
        this.rootGroup.add(this.sceneGroup);

        // InstancedMesh for high performance (Single draw call)
        this.maxInstances = 50000;
        const boxGeometry = new BoxGeometry(1.002, 1.002, 1.002);
        const boxMaterial = new MeshLambertMaterial({
            color: 0xffffff,
            roughness: 0.7,
            metalness: 0.1
        });
        this.instancedMesh = new InstancedMesh(boxGeometry, boxMaterial, this.maxInstances);
        this.instancedMesh.instanceMatrix.setUsage(DynamicDrawUsage);
        this.instancedMesh.count = 0;
        this.sceneGroup.add(this.instancedMesh);

        // Real-time flash effect pulse boxes
        this.pulseGroup = new Group();
        this.pulseGroup.name = "bm-block-pulses";
        this.sceneGroup.add(this.pulseGroup);

        const wireGeometry = new WireframeGeometry(new BoxGeometry(1.04, 1.04, 1.04));
        this.wireGeometry = wireGeometry;
        this.wireMaterial = new LineBasicMaterial({
            color: 0x00ffcc,
            transparent: true,
            opacity: 0.9,
            linewidth: 2
        });

        this.pulses = [];

        // Active block simulation state: map of "x,y,z" -> {x, y, z, color, time, player, block}
        this.activeBlocks = new Map();
        this.activeBlockArray = [];

        this._updateInterval = null;
        this._stepHandler = (e) => this.onFrame(e.detail ? e.detail.delta : 16);

        if (this.events) {
            this.events.addEventListener("bluemapRenderFrame", this._stepHandler);
        }

        // Start initial load & poll
        this.fetchFullHistory().then(() => {
            this.startPolling(1000);
        });
    }

    startPolling(ms = 1000) {
        if (this._updateInterval) clearInterval(this._updateInterval);
        this._updateInterval = setInterval(() => {
            if (this.disposed) return;
            this.pollNewEvents();
        }, ms);
    }

    stopPolling() {
        if (this._updateInterval) {
            clearInterval(this._updateInterval);
            this._updateInterval = null;
        }
    }

    async fetchFullHistory() {
        try {
            let res = await fetch(`${this.fileUrl}?since=0`, { cache: "no-cache" });
            if (!res.ok) return;
            let data = await res.json();
            this.handlePayload(data, true);
        } catch (e) {
            // Silently retry later
        }
    }

    async pollNewEvents() {
        try {
            let url = `${this.fileUrl}?since=${this.latestSeq}`;
            let res = await fetch(url, { cache: "no-cache" });
            if (!res.ok) return;
            let data = await res.json();
            this.handlePayload(data, false);
        } catch (e) {
            // Silently retry later
        }
    }

    handlePayload(payload, isInitial) {
        if (!payload || !Array.isArray(payload.events)) return;

        if (payload.serverStartTime) {
            this.data.serverStartTime = payload.serverStartTime;
        }
        if (payload.currentTime) {
            this.data.serverEndTime = Math.max(this.data.serverEndTime, payload.currentTime);
        }
        if (payload.latestSeq) {
            this.latestSeq = Math.max(this.latestSeq, payload.latestSeq);
        }

        let newEvents = payload.events;
        if (newEvents.length === 0 && !isInitial) return;

        if (isInitial) {
            this.eventsList = newEvents;
        } else {
            for (let evt of newEvents) {
                if (evt.seq > this.latestSeq - newEvents.length) {
                    this.eventsList.push(evt);
                }
            }
        }

        // Keep bounded list on client
        if (this.eventsList.length > this.maxInstances) {
            this.eventsList.splice(0, this.eventsList.length - this.maxInstances);
        }

        this.data.totalEvents = this.eventsList.length;

        // If not scrubbing back in timelapse, keep cursor at latest
        if (!this.data.isPlaying && this.data.progress >= 0.99) {
            this.data.currentTime = this.data.serverEndTime;
            this.rebuildActiveBlocks(this.data.currentTime);
        }

        // Spawn pulse effect for newly placed blocks in real time
        if (!isInitial && newEvents.length > 0 && this.data.active) {
            this.data.recentPlacedCount += newEvents.length;
            let lastEvt = newEvents[newEvents.length - 1];
            this.data.latestPlayer = lastEvt.p || "Player";
            this.data.latestBlock = (lastEvt.b || "").replace("minecraft:", "");

            for (let evt of newEvents.slice(-10)) { // Limit pulses to 10 latest
                if (evt.a === "place") {
                    this.spawnPulse(evt.x, evt.y, evt.z);
                }
            }

            if (this.data.autoFollow && this.mapViewer && lastEvt) {
                this.focusOnBlock(lastEvt.x, lastEvt.y, lastEvt.z);
            }
        }
    }

    rebuildActiveBlocks(upToTime) {
        this.activeBlocks.clear();
        let targetTime = upToTime !== undefined ? upToTime : this.data.currentTime;

        for (let evt of this.eventsList) {
            if (evt.t > targetTime) break;
            let key = `${evt.x},${evt.y},${evt.z}`;
            if (evt.a === "place") {
                this.activeBlocks.set(key, {
                    x: evt.x,
                    y: evt.y,
                    z: evt.z,
                    color: getBlockColor(evt.b),
                    b: evt.b,
                    p: evt.p,
                    t: evt.t
                });
            } else if (evt.a === "break") {
                this.activeBlocks.delete(key);
            }
        }

        this.syncInstancedMesh();
    }

    syncInstancedMesh() {
        if (!this.instancedMesh) return;

        let blocks = Array.from(this.activeBlocks.values());
        let count = Math.min(blocks.length, this.maxInstances);

        const matrix = new Matrix4();
        const color = new Color();

        for (let i = 0; i < count; i++) {
            let b = blocks[i];
            matrix.setPosition(b.x + 0.5, b.y + 0.5, b.z + 0.5);
            this.instancedMesh.setMatrixAt(i, matrix);

            color.setHex(b.color);
            this.instancedMesh.setColorAt(i, color);
        }

        this.instancedMesh.count = count;
        this.instancedMesh.instanceMatrix.needsUpdate = true;
        if (this.instancedMesh.instanceColor) {
            this.instancedMesh.instanceColor.needsUpdate = true;
        }

        this.data.renderedBlocks = count;
    }

    spawnPulse(x, y, z) {
        if (this.pulses.length > 20) {
            let old = this.pulses.shift();
            this.pulseGroup.remove(old.mesh);
        }

        const lines = new LineSegments(this.wireGeometry, this.wireMaterial.clone());
        lines.position.set(x + 0.5, y + 0.5, z + 0.5);
        this.pulseGroup.add(lines);

        this.pulses.push({
            mesh: lines,
            life: 1.0,
            scale: 1.0
        });
    }

    onFrame(deltaMs) {
        let deltaSec = deltaMs / 1000;

        // Update real-time pulses
        if (this.pulses.length > 0) {
            for (let i = this.pulses.length - 1; i >= 0; i--) {
                let p = this.pulses[i];
                p.life -= deltaSec * 0.8;
                p.scale += deltaSec * 0.2;
                p.mesh.scale.set(p.scale, p.scale, p.scale);
                p.mesh.material.opacity = Math.max(0, p.life);

                if (p.life <= 0) {
                    this.pulseGroup.remove(p.mesh);
                    this.pulses.splice(i, 1);
                }
            }
        }

        // Timelapse playback
        if (this.data.isPlaying) {
            let totalSpan = Math.max(1000, this.data.serverEndTime - this.data.serverStartTime);
            // Speed factor: 1x = real-time, 10x = 10x faster
            let advanceMs = deltaMs * this.data.speed;
            this.data.currentTime += advanceMs;

            if (this.data.currentTime >= this.data.serverEndTime) {
                this.data.currentTime = this.data.serverEndTime;
                this.data.isPlaying = false;
                this.data.progress = 1.0;
            } else {
                let currentSpan = this.data.currentTime - this.data.serverStartTime;
                this.data.progress = Math.min(1.0, Math.max(0.0, currentSpan / totalSpan));
            }

            this.rebuildActiveBlocks(this.data.currentTime);
        }
    }

    seek(progress) {
        this.data.progress = Math.max(0.0, Math.min(1.0, progress));
        let totalSpan = Math.max(1000, this.data.serverEndTime - this.data.serverStartTime);
        this.data.currentTime = this.data.serverStartTime + totalSpan * this.data.progress;
        this.rebuildActiveBlocks(this.data.currentTime);
    }

    play() {
        if (this.data.progress >= 0.999) {
            this.seek(0.0);
        }
        this.data.isPlaying = true;
    }

    pause() {
        this.data.isPlaying = false;
    }

    togglePlay() {
        if (this.data.isPlaying) {
            this.pause();
        } else {
            this.play();
        }
    }

    setSpeed(speed) {
        this.data.speed = speed;
    }

    toggleTimelapse() {
        this.data.timelapseOpen = !this.data.timelapseOpen;
    }

    focusOnBlock(x, y, z) {
        if (!this.mapViewer || !this.mapViewer.controlsManager) return;
        let controls = this.mapViewer.controlsManager;
        if (controls.position) {
            controls.position.x = x + 0.5;
            controls.position.z = z + 0.5;
        }
    }

    dispose() {
        this.disposed = true;
        this.stopPolling();

        if (this.events && this._stepHandler) {
            this.events.removeEventListener("bluemapRenderFrame", this._stepHandler);
        }

        if (this.sceneGroup && this.rootGroup) {
            this.rootGroup.remove(this.sceneGroup);
        }

        if (this.instancedMesh) {
            this.instancedMesh.geometry.dispose();
            if (Array.isArray(this.instancedMesh.material)) {
                this.instancedMesh.material.forEach(m => m.dispose());
            } else {
                this.instancedMesh.material.dispose();
            }
        }

        if (this.wireGeometry) this.wireGeometry.dispose();
        if (this.wireMaterial) this.wireMaterial.dispose();
    }

}
