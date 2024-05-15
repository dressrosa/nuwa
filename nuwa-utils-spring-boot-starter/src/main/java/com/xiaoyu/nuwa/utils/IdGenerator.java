/**
 * copyright com.xiaoyu
 */
package com.xiaoyu.nuwa.utils;

import java.util.Calendar;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

/**
 * 生成唯一性id
 * 
 */
public class IdGenerator {

    private static final SnowFlake Random = new SnowFlake();

    /**
     * 无分隔符的id
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 生成long型id
     */
    public static long randomLong() {
        return Random.generateKey();
    }

    public static String traceid() {
        return uuid();
    }

    public static String spanid() {
        return uuid();
    }

    /**
     * 雪花算法
     *
     */
    @Slf4j
    private static class SnowFlake {
        private static final long EPOCH;

        private static final long SEQUENCE_BITS = 12L;

        private static final long WORKER_ID_BITS = 10L;

        private static final long SEQUENCE_MASK = (1 << SEQUENCE_BITS) - 1;

        private static final long WORKER_ID_LEFT_SHIFT_BITS = SEQUENCE_BITS;

        private static final long TIMESTAMP_LEFT_SHIFT_BITS = WORKER_ID_LEFT_SHIFT_BITS + WORKER_ID_BITS;

        private static final long WORKER_ID = 0;

        private static final int DEFAULT_VIBRATION_VALUE = 1;

        private static final int MAX_TOLERATE_TIME_DIFFERENCE_MILLISECONDS = 10;

        private int sequenceOffset = -1;

        private long sequence;

        private long lastMilliseconds;

        static {
            Calendar calendar = Calendar.getInstance();
            calendar.set(2016, Calendar.NOVEMBER, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            EPOCH = calendar.getTimeInMillis();
        }

        public synchronized long generateKey() {
            long currentMilliseconds = System.currentTimeMillis();
            if (waitTolerateTimeDifferenceIfNeed(currentMilliseconds)) {
                currentMilliseconds = System.currentTimeMillis();
            }
            if (lastMilliseconds == currentMilliseconds) {
                if (0L == (sequence = (sequence + 1) & SEQUENCE_MASK)) {
                    currentMilliseconds = waitUntilNextTime(currentMilliseconds);
                }
            } else {
                vibrateSequenceOffset();
                sequence = sequenceOffset;
            }
            lastMilliseconds = currentMilliseconds;
            return ((currentMilliseconds - EPOCH) << TIMESTAMP_LEFT_SHIFT_BITS)
                    | (getWorkerId() << WORKER_ID_LEFT_SHIFT_BITS) | sequence;
        }

        private boolean waitTolerateTimeDifferenceIfNeed(final long currentMilliseconds) {
            if (lastMilliseconds <= currentMilliseconds) {
                return false;
            }
            long timeDifferenceMilliseconds = lastMilliseconds - currentMilliseconds;
            if (timeDifferenceMilliseconds < getMaxTolerateTimeDifferenceMilliseconds()) {
                log.warn("Clock is moving backwards, last time is %d milliseconds, current time is %d milliseconds",
                        lastMilliseconds, currentMilliseconds);
            }
            try {
                Thread.sleep(timeDifferenceMilliseconds);
            } catch (Exception ignore) {
            }
            return true;
        }

        private long getWorkerId() {
            return WORKER_ID;
        }

        private int getMaxVibrationOffset() {
            return DEFAULT_VIBRATION_VALUE;
        }

        private int getMaxTolerateTimeDifferenceMilliseconds() {
            return MAX_TOLERATE_TIME_DIFFERENCE_MILLISECONDS;
        }

        private long waitUntilNextTime(final long lastTime) {
            long result = System.currentTimeMillis();
            while (result <= lastTime) {
                result = System.currentTimeMillis();
            }
            return result;
        }

        private void vibrateSequenceOffset() {
            sequenceOffset = sequenceOffset >= getMaxVibrationOffset() ? 0 : sequenceOffset + 1;
        }

    }
}
