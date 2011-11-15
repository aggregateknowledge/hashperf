/******************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aggregate Knowledge - implementation
 ******************************************************************************/
package net.agkn.hashperf.services;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import net.agkn.hashperf.IPerformanceTest;
import net.agkn.hashperf.util.RecordIterator;

/**
 * Tests Redis performance <em>without</em> pipelining. Very slow.
 *
 * NOTE:  Expects a redis-server instance to be running on localhost at the default port.
 */
public class SlowRedisPerformanceTest implements IPerformanceTest {
    /* (non-Javadoc)
     * @see net.agkn.hashperf.IPerformanceTest#doRun(RecordIterator)
     */
    public void doRun(final RecordIterator iter) {
        final Jedis j = new Jedis("localhost");

        iter.start();
        while(iter.hasNext()) {
            j.append(iter.nextPersonId(), iter.nextEvent());
            iter.next();
        }
        iter.stop();

        // clean up after the test is run
        try {
            j.flushDB();
        } catch(JedisConnectionException e) {
            System.err.println("It took a bit to flush Redis. Not to worry.");
        }

        // make sure the flush is done on the server as well
        boolean isDone = false;
        while(!isDone) {
            try {
                j.ping();
                isDone = true;
            } catch(JedisConnectionException e) {
                System.err.println("Not responding yet.");
            }
        }
        j.disconnect();
    }
}