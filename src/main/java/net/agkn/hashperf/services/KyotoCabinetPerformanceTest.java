/******************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aggregate Knowledge - implementation
 ******************************************************************************/
package net.agkn.hashperf.services;

import kyotocabinet.DB;
import net.agkn.hashperf.IPerformanceTest;
import net.agkn.hashperf.util.RecordIterator;

/**
 * Tests the performance of Kyoto Cabinet.
 */
public class KyotoCabinetPerformanceTest implements IPerformanceTest {

    /**
     * Builds an in-memory hash database.
     * @return
     */
    private static DB buildDB() {
        final DB db = new DB();
        db.open("+", DB.OWRITER | DB.OCREATE);

        return db;
    }

    /* (non-Javadoc)
     * @see net.agkn.hashperf.IPerformanceTest#doRun(RecordIterator)
     */
    public void doRun(final RecordIterator iter) {
        final DB db = buildDB();

        iter.start();
        while(iter.hasNext()) {
            db.append(iter.nextPersonId(), iter.nextEvent());
            iter.next();
        }
        iter.stop();

        db.close();
    }
}
