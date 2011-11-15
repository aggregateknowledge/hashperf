/******************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aggregate Knowledge - implementation
 ******************************************************************************/
package net.agkn.hashperf;

import java.io.FileWriter;
import java.io.IOException;

import net.agkn.hashperf.util.RecordIterator;

/**
 * Runner for a performance test.
 */
public class PerformanceTestHarness {
    // the test to run
    final IPerformanceTest test;
    // the path of the test record file
    final String testFilePath;
    // the path where the stats file should be written
    final String statsOutfilePath;
    // number of warmup runs
    final int warmupRuns;
    // number of observation runs
    final int observationRuns;
    // records between stats polling
    final int pollingInterval;

    /**
     * @param test the performance test to run
     * @param testFilePath the path of the file with the test records
     * @param statsOutfilePath the path where the stats file should be written
     * @param warmupRuns the number of warmup runs (results are discarded for these initial runs)
     * @param observationRuns the number of observation runs after the warmup runs
     * @param pollingInterval the number of records in between polls for stats
     */
    public PerformanceTestHarness(final IPerformanceTest test, final String testFilePath, final String statsOutfilePath, final int warmupRuns, final int observationRuns, final int pollingInterval) {
        this.test = test;
        this.statsOutfilePath = statsOutfilePath;
        this.testFilePath = testFilePath;
        this.warmupRuns = warmupRuns;
        this.observationRuns = observationRuns;
        this.pollingInterval = pollingInterval;
    }

    public void run() {
        FileWriter output;
        try {
            // warmups
            for(int i=0; i<warmupRuns; i++) {
                System.err.println(test.getClass().getSimpleName() + " warmup " + i);

                final RecordIterator iter = new RecordIterator(testFilePath, pollingInterval);
                test.doRun(iter);
            }

            // read the stats file
            output = new FileWriter(statsOutfilePath);
            output.write("run,record_number,time,time_diff\n");
            output.flush();

            // observed runs
            for(int i=0; i<observationRuns; i++) {
                System.err.println(test.getClass().getSimpleName() + " obs " + i);

                final RecordIterator iter = new RecordIterator(testFilePath, pollingInterval);
                test.doRun(iter);

                // write stats to file
                final long[] polledTimes = iter.getPolledTimes();
                long lastTime = 0;
                for(int j=0; j<polledTimes.length; j++) {
                    final int recordCount = (j + 1) * pollingInterval;
                    final long time = polledTimes[j];
                    final long timeDiff = time - lastTime;
                    output.write(i + "," + recordCount + "," + time + "," + timeDiff + "\n");
                    lastTime = time;
                }
                output.flush();
            }

            output.close();
        } catch(IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't open file " + statsOutfilePath);
        }
    }
}
