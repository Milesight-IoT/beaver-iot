package com.milesight.beaveriot.base.utils.snowflake;

import com.milesight.beaveriot.base.constants.StringConstant;
import com.milesight.beaveriot.base.exception.BaseException;
import com.milesight.beaveriot.base.utils.SystemClock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 */
@Slf4j
public class Sequence {

    /**
     * The starting point of time, as a benchmark, is generally the latest time of the system (it cannot be changed once determined)
     */
    private final long twepoch = 1288834974657L;
    /**
     * Machine identification digits
     */
    private final long workerIdBits = 5L;
    private final long datacenterIdBits = 5L;
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    /**
     * Increment within milliseconds
     */
    private final long sequenceBits = 12L;
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    /**
     * Timestamp shifted left
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private final long workerId;

    /**
     * Data identification ID part
     */
    private final long datacenterId;
    /**
     * Concurrency control
     */
    private long sequence = 0L;
    /**
     * Last production ID timestamp
     */
    private long lastTimestamp = -1L;

    private final long initialTimestamp;

    private final long initialNanoTime;

    private InetAddress inetAddress;

    public Sequence() {
        this.initialTimestamp = SystemClock.now();
        this.initialNanoTime = System.nanoTime();
        this.inetAddress = getLocalHost();
        this.datacenterId = getDatacenterId(maxDatacenterId);
        this.workerId = getMaxWorkerId(datacenterId, maxWorkerId);
    }

    public Sequence(InetAddress inetAddress) {
        this.initialTimestamp = SystemClock.now();
        this.initialNanoTime = System.nanoTime();
        this.inetAddress = inetAddress;
        this.datacenterId = getDatacenterId(maxDatacenterId);
        this.workerId = getMaxWorkerId(datacenterId, maxWorkerId);
    }

    private InetAddress getLocalHost() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new BaseException(e);
        }
    }

    /**
     * @return InetAddress
     * @since 3.4.3
     */
    protected InetAddress getInetAddress() {
        return Optional.ofNullable(this.inetAddress).orElseGet(this::getLocalHost);
    }

    /**
     * @param workerId     WORKING MACHINE ID
     * @param datacenterId SerialNumber
     */
    public Sequence(long workerId, long datacenterId) {
        this.initialTimestamp = SystemClock.now();
        this.initialNanoTime = System.nanoTime();
        Assert.isTrue(!(workerId > maxWorkerId || workerId < 0),
                String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        Assert.isTrue(!(datacenterId > maxDatacenterId || datacenterId < 0),
                String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * get maxWorkerId
     */
    protected long getMaxWorkerId(long datacenterId, long maxWorkerId) {
        StringBuilder mpid = new StringBuilder();
        mpid.append(datacenterId);
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (StringUtils.hasText(name)) {
            /*
             * GET jvmPid
             */
            mpid.append(name.split(StringConstant.AT)[0]);
        }
        /*
         * The hashcode of MAC + PID gets the 16 low bits
         */
        return (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
    }

    /**
     * Data identification id part
     */
    protected long getDatacenterId(long maxDatacenterId) {
        long id = 0L;
        try {
            NetworkInterface network = NetworkInterface.getByInetAddress(this.getInetAddress());
            if (network == null) {
                id = 1L;
            } else {
                byte[] mac = network.getHardwareAddress();
                if (null != mac) {
                    id = ((0x000000FF & (long) mac[mac.length - 2]) | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
                    id = id % (maxDatacenterId + 1);
                }
            }
        } catch (Exception e) {
            log.warn(" getDatacenterId: " + e.getMessage());
        }
        return id;
    }

    /**
     * Get next ID
     *
     * @return next ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
            }
        }

        if (lastTimestamp == timestamp) {
            // Within the same millisecond, the sequence number increases automatically.
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // The sequence number for the same millisecond has reached its maximum.
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // Within different milliseconds, the sequence number is set to 1 - 3 random numbers
            sequence = ThreadLocalRandom.current().nextLong(1, 3);
        }

        lastTimestamp = timestamp;

        // Timestamp part | Data center part | Machine identification part | Serial number part
        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        long nanoTime = System.nanoTime();
        return initialTimestamp + ((nanoTime - initialNanoTime) / 1000000);
    }

}
