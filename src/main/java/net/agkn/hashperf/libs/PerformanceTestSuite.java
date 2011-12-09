/******************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aggregate Knowledge - implementation
 ******************************************************************************/
package net.agkn.hashperf.libs;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import net.agkn.hashperf.IPerformanceTest;
import net.agkn.hashperf.PerformanceTestHarness;
import net.agkn.hashperf.services.BaselinePerformanceTest;
import net.agkn.hashperf.util.RecordIterator;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import bak.pcj.map.LongKeyLongChainedHashMap;
import bak.pcj.map.LongKeyLongOpenHashMap;
import java.util.HashMap;
import com.carrotsearch.hppc.LongLongOpenHashMap;
import org.apache.mahout.math.map.OpenLongLongHashMap;

public class PerformanceTestSuite {
    static public void main(String[] args) {

        final String testFilePath = args[0];
        final String statsOutfilePrefix = args[1];
        final int warmupRuns = Integer.valueOf(args[2]);
        final int observationRuns = Integer.valueOf(args[3]);
        final int pollingInterval = Integer.valueOf(args[4]);
        final int sizeHint = Integer.valueOf(args[5]);
        final float loadFactor = Float.valueOf(args[6]);

        // Substitute any of the maps here down below. Make sure to change the
        // value of the variable 'c', as well.

        // chained
        // final HashMap<Long, Long> map = new HashMap<Long, Long>(sizeHint, loadFactor); 
        // final LongKeyLongChainedHashMap map = new LongKeyLongChainedHashMap(sizeHint, loadFactor);

        // linear probing
        // final LongLongOpenHashMap map = new LongLongOpenHashMap(sizeHint, loadFactor);
        // final Long2LongOpenHashMap map = new Long2LongOpenHashMap(sizeHint, loadFactor);

        // double hashing
        // final LongKeyLongOpenHashMap map = new LongKeyLongOpenHashMap(sizeHint, loadFactor);
        // final TLongLongHashMap map = new TLongLongHashMap(sizeHint, loadFactor);
        // final OpenLongLongHashMap map = new OpenLongLongHashMap(sizeHint, 0.0, loadFactor);

        // replace class below with class choice from above
        final Class c = HashMap.class;
        final String statsOutfilePath = statsOutfilePrefix + c.getSimpleName() + "_lf" + (int)Math.floor(loadFactor * 100) + "_init" + (sizeHint/1000000) + ".csv";
        FileWriter output;

        try {
            // warmups
            for(int i=0; i<warmupRuns; i++) {
                final Random random = new Random(1L);
                final IdReader iter = new IdReader(testFilePath);
                final Stats stats = new Stats();

                long counter = 0L;
                stats.start();

                // replace line below with choice of map from above
                final HashMap<Long, Long> map = new HashMap<Long, Long>(sizeHint, loadFactor);
                System.err.println(c.getSimpleName() + " warmup " + i);

                while(iter.hasNext()) {
                    map.put(iter.next(), random.nextLong());
                    counter +=1;
                    if(counter % pollingInterval == 0) {
                        System.err.println(counter);
                        stats.observe();
                    }
                }
            }

            output = new FileWriter(statsOutfilePath);
            output.write("run,rec_no,time_diff,memory_usage\n");
            output.flush();

            // observed runs
            for(int obsRunNo=0; obsRunNo<observationRuns; obsRunNo++) {
                final Random random = new Random(1L);
                final IdReader iter = new IdReader(testFilePath);
                final Stats stats = new Stats();

                long counter = 0L;
                stats.start();

                // replace line below with choice of map from above
                final HashMap<Long, Long> map = new HashMap<Long, Long>(sizeHint, loadFactor);
                System.err.println(c.getSimpleName() + " obs " + obsRunNo);

                while(iter.hasNext()) {
                    map.put(iter.next(), random.nextLong());
                    counter +=1;
                    if(counter % pollingInterval == 0) {
                        System.err.println(counter);
                        stats.observe();
                    }
                }

                // write stats to file
                final List<Long> timeDiffs = stats.getTimeDiffs();
                final List<Long> memoryUsage = stats.getMemoryUsage();
                for(int i=0; i<timeDiffs.size(); i++) {
                    output.write(obsRunNo + "," + ((long)i * (long)pollingInterval) + "," + timeDiffs.get(i) + "," + memoryUsage.get(i) + "\n");
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
