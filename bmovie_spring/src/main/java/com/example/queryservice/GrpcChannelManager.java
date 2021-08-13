package com.example.queryservice;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GrpcChannelManager {
    private static final GrpcChannelManager instance = new GrpcChannelManager();
    public static final GrpcChannelManager getInstance() {
        return instance;
    }

    private final ConcurrentMap<String, ManagedChannel> channelMap = new ConcurrentHashMap<>();

    public ManagedChannel getChannel(String host, int port) {
        String channelKey = String.format("%s:%d", host, port);

        ManagedChannel channel = channelMap.get(channelKey);
        if (channel == null) {
            ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port).usePlaintext();
            channel = channelBuilder.build();
            ManagedChannel existingChannel = channelMap.putIfAbsent(channelKey, channel);
            if (existingChannel != null) {
                channel.shutdown();
                return existingChannel;
            }
        }
        return channel;
    }
}

