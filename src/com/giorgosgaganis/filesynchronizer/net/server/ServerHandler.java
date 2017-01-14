/*
 * Copyright (C) 2017 Giorgos Gaganis
 *
 * This file is part of odoxSync.
 *
 * odoxSync is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * odoxSync is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with odoxSync.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.giorgosgaganis.filesynchronizer.net.server;
import com.giorgosgaganis.filesynchronizer.FileSynchronizer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Handles both client-side and server-side handler depending on which
 * constructor was called.
 */
public class ServerHandler extends SimpleChannelInboundHandler<IntroductionRequest>{

    private final FileSynchronizer fileSynchronizer;

    public ServerHandler(FileSynchronizer fileSynchronizer) {
        this.fileSynchronizer = fileSynchronizer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IntroductionRequest msg) throws Exception {
        long clientId = fileSynchronizer.setupClient();
        ctx.channel().write(Long.valueOf(clientId));
    }
}
