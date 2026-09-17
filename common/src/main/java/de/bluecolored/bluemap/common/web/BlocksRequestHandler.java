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
package de.bluecolored.bluemap.common.web;

import de.bluecolored.bluemap.common.live.BlockTracker;
import de.bluecolored.bluemap.common.serverinterface.Server;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.common.web.http.HttpRequest;
import de.bluecolored.bluemap.common.web.http.HttpRequestHandler;
import de.bluecolored.bluemap.common.web.http.HttpResponse;
import de.bluecolored.bluemap.common.web.http.HttpStatusCode;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class BlocksRequestHandler implements HttpRequestHandler {

    private final BlockTracker blockTracker;
    private final Server server;
    private final World world;
    private transient @Nullable ServerWorld serverWorld;
    private final Random random = new Random();

    public BlocksRequestHandler(BlockTracker blockTracker, Server server, World world) {
        this.blockTracker = blockTracker;
        this.server = server;
        this.world = world;
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        if (serverWorld == null) {
            serverWorld = server.getServerWorld(world).orElse(null);
        }
        Key dimension = serverWorld != null ? serverWorld.getDimension() : null;

        long since = 0;
        String sinceStr = request.getGETParams().get("since");
        if (sinceStr != null && !sinceStr.isEmpty()) {
            try {
                since = Long.parseLong(sinceStr);
            } catch (NumberFormatException ignored) {}
        }

        String json = blockTracker.toJson(dimension, since);

        HttpResponse response = new HttpResponse(HttpStatusCode.OK);
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Content-Type", "application/json");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setData(json);
        return response;
    }

}
