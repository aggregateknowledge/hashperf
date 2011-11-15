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
import net.agkn.hashperf.PerformanceTestHarness;

/**
 * Runs the full suite of performance tests.
 */
public class FullPerformanceTestSuite {
    static public void main(String[] args) {
        final IPerformanceTest[] tests = new IPerformanceTest[] {
                new BaselinePerformanceTest(),
                new BDBPerformanceTest(),
                new KyotoCabinetPerformanceTest(),
                new RedisPerformanceTest()
        };

        final String testFilePath = args[0];
        final String statsOutfilePrefix = args[1];
        final int warmupRuns = Integer.valueOf(args[2]);
        final int observationRuns = Integer.valueOf(args[3]);
        final int pollingInterval = Integer.valueOf(args[4]);

        for(final IPerformanceTest test : tests) {
            System.err.println("Running test " + test.toString());
            final PerformanceTestHarness harness = new PerformanceTestHarness(
                    test,
                    testFilePath,
                    statsOutfilePrefix + test.getClass().getSimpleName().toString() + ".csv",
                    warmupRuns,
                    observationRuns,
                    pollingInterval
            );
            harness.run();
        }
    }
}
