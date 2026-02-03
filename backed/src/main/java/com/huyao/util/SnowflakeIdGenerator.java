package com.huyao.util;

public final class SnowflakeIdGenerator {
    private static final long EPOCH = 1700000000000L;
    private static final long WORKER_ID = 1L;
    private static final long DATACENTER_ID = 1L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static long lastTimestamp = -1L;
    private static long sequence = 0L;

    private SnowflakeIdGenerator() {
    }

    public static synchronized long nextId() {
        long timestamp = currentTime();

        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("clock moved backwards");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(timestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (DATACENTER_ID << DATACENTER_ID_SHIFT)
                | (WORKER_ID << WORKER_ID_SHIFT)
                | sequence;
    }

    private static long waitNextMillis(long timestamp) {
        long current = currentTime();
        while (current <= timestamp) {
            current = currentTime();
        }
        return current;
    }

    private static long currentTime() {
        return System.currentTimeMillis();
    }
}
