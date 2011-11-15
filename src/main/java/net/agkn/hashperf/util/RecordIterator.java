/******************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aggregate Knowledge - implementation
 ******************************************************************************/
package net.agkn.hashperf.util;

/**
 * Wraps an {@link EventReader}, and yields each record as bytes. Also provides
 * basic polling-based statistics collection.
 */
public class RecordIterator {
    // The maximum number of times to poll
    private static int POLL_COUNT = 1000;

    // ************************************************************************

    // the wrapped reader
    final private EventReader reader;

    // ------------------------------------------------------------------------
    // stats

    // the polled times
    final private long[] times;

    // the number of records between polls
    final private int pollingInterval;

    // the pointer to the next available position to write to in the stats array
    private int pollPosition;

    // the starting and ending times of this iterator
    private long start;
    private long end;

    // the number of records seen
    private long counter;

    // ------------------------------------------------------------------------
    // iterator state

    // the next person id to be yielded
    private byte[] personId;
    // the next event to be yielded
    private byte[] event;

    /**
     * @param filePath the path of the file containing the records
     * @param pollingInterval the number of records in between timing polls
     */
    public RecordIterator(final String filePath, final int pollingInterval) {
        this.pollingInterval = pollingInterval;
        counter = 0L;
        personId = new byte[8];
        event = new byte[16];
        times = new long[POLL_COUNT];
        pollPosition = 0;
        reader = new EventReader(filePath);
        // prime the iterator
        getNext();
    }

    // ************************************************************************
    // iterator interface methods
    /**
     * Checks if any records remain in the iterator.
     */
    public boolean hasNext() {
        return (event != null);
    }

    /**
     * Advances the iterator to the next record.
     */
    public void next() {
        getNext();
    }

    /**
     * Yields the <code>person_id</code> (as a <code>byte[]</code>) for the current
     * record.
     */
    public byte[] nextPersonId() {
        return personId;
    }

    /**
     * Yields the <code>event</code> (packed as a <code>byte[]</code>) for the
     * current record.
     */
    public byte[] nextEvent() {
        return event;
    }

    /**
     * Advances the iterator's internal state.
     */
    private void getNext() {
        long[] raw = reader.next();
        if(raw == null) {
            personId = null;
            event = null;
        } else {
            ByteUtil.longToBytes(raw[0], personId, 0);
            final long[] packed = EventPacker.packEvent(raw);
            ByteUtil.longToBytes(packed[0], event, 0);
            ByteUtil.longToBytes(packed[1], event, 8);
            counter += 1;
            if(counter % pollingInterval == 0) {
                System.err.println(counter);
                times[pollPosition] = (System.currentTimeMillis() - start);
                pollPosition += 1;
            }
        }
    }

    // ************************************************************************
    // timer methods
    /**
     * Starts the timer.
     */
    public void start() {
        start = System.currentTimeMillis();
    }

    /**
     * Stops the timer.
     */
    public void stop() {
        end = System.currentTimeMillis();
    }

    // ************************************************************************
    // stats getters
    public int getPollingInterval() {
        return pollingInterval;
    }

    /**
     * Computes the number of transactions per second. A transaction is considered
     * complete once {@link #next()} is called.
     *
     * NOTE:  {@link #start()} and then {@link #stop()} must be called before
     *        this method is called, for sensical results.
     */
    public double getTransactionsPerSecond() {
        return 1000 * (counter/(double)(end - start));
    }

    /**
     * Computes the elapsed time in seconds between the calls to {@link #start()}
     * and {@link #stop()}.
     */
    public double getElapsedSeconds() {
        return (end - start)/(double)1000;
    }

    /**
     * Computes the number of records iterated over.
     */
    public long getCount() {
        return counter;
    }

    /**
     * Returns the polled times.
     */
    public long[] getPolledTimes() {
        final long[] usedTimes = new long[pollPosition];
        System.arraycopy(times, 0, usedTimes, 0, pollPosition);
        return usedTimes;
    }
}
