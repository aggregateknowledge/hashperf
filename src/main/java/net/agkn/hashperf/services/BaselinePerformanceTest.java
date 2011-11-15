/******************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aggregate Knowledge - implementation
 ******************************************************************************/
package net.agkn.hashperf.services;

import net.agkn.hashperf.IPerformanceTest;
import net.agkn.hashperf.util.RecordIterator;

/**
 * Tests the baseline performance of reading and packing records.
 */
public class BaselinePerformanceTest implements IPerformanceTest{
    /* (non-Javadoc)
     * @see net.agkn.hashperf.IPerformanceTest#doRun(RecordIterator)
     */
    public void doRun(final RecordIterator iter) {
        iter.start();
        while(iter.hasNext()) {
            iter.nextPersonId();
            iter.nextEvent();
            iter.next();
        }
        iter.stop();
    }
}
