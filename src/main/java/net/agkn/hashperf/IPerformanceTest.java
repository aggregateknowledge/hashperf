/******************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aggregate Knowledge - implementation
 ******************************************************************************/
package net.agkn.hashperf;

import net.agkn.hashperf.util.RecordIterator;

/**
 * Defines the common interface for a runnable performance test.
 */
public interface IPerformanceTest {
    /**
     * Runs the performance test using the given test file, containing test
     * records.
     *
     * @param iter the {@link RecordIterator} that will time this test and
     *        yield the records. The {@link IPerformanceTest} is responsible for
     *        calling {@link RecordIterator#start()} and {@link RecordIterator#stop()}.
     */
    void doRun(RecordIterator iter);
}
