package com.nexusfin.equity.util;

import com.nexusfin.equity.config.AsyncCompensationProperties;
import org.springframework.stereotype.Component;

@Component
public class AsyncCompensationPartitioner {

    private final AsyncCompensationProperties properties;

    public AsyncCompensationPartitioner(AsyncCompensationProperties properties) {
        this.properties = properties;
    }

    public int partitionOf(String bizKey) {
        return Math.floorMod(bizKey.hashCode(), properties.getPartitionCount());
    }
}
