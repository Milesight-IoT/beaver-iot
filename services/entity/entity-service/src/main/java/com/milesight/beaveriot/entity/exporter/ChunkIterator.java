package com.milesight.beaveriot.entity.exporter;

import java.util.List;

/**
 * Iterator for exporting data. <br>
 * To avoid heap memory overflow, the data should be provided in small chunks.
 *
 * @param <T> Type of return data
 */
@FunctionalInterface
public interface ChunkIterator<T> {

    /**
     * Get data chunk by given iteration number
     *
     * @param iteration Iteration number
     * @return Slice of data
     */
    List<T> get(int iteration);

}
