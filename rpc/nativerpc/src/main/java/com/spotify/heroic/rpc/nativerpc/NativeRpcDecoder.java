/*
 * Copyright (c) 2015 Spotify AB.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.heroic.rpc.nativerpc;

import com.spotify.heroic.rpc.nativerpc.message.NativeRpcError;
import com.spotify.heroic.rpc.nativerpc.message.NativeRpcHeartBeat;
import com.spotify.heroic.rpc.nativerpc.message.NativeRpcRequest;
import com.spotify.heroic.rpc.nativerpc.message.NativeRpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.msgpack.MessagePack;
import org.msgpack.unpacker.Unpacker;

import java.util.List;

public class NativeRpcDecoder extends ByteToMessageDecoder {
    private final MessagePack messagePack = new MessagePack();

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out)
        throws Exception {
        final int length = in.readableBytes();

        if (length == 0) {
            return;
        }

        try (final ByteBufInputStream stream = new ByteBufInputStream(in)) {
            final Unpacker unpacker = messagePack.createUnpacker(stream);

            final byte type = unpacker.readByte();

            switch (type) {
                case NativeRpc.HEARTBEAT:
                    out.add(NativeRpcHeartBeat.unpack(unpacker));
                    return;
                case NativeRpc.REQUEST:
                    out.add(NativeRpcRequest.unpack(unpacker));
                    return;
                case NativeRpc.RESPONSE:
                    out.add(NativeRpcResponse.unpack(unpacker));
                    return;
                case NativeRpc.ERR_RESPONSE:
                    out.add(NativeRpcError.unpack(unpacker));
                    return;
                default:
                    throw new IllegalArgumentException("Invalid RPC message type: " + type);
            }
        }
    }
}
