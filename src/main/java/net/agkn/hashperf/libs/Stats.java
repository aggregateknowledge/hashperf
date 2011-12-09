/******************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aggregate Knowledge - implementation
 ******************************************************************************/
package net.agkn.hashperf.libs;

import java.util.ArrayList;
import java.util.List;

public class Stats {
    private long startTime;
    private long startMemory;
    private List<Long> timeObservations;
    private List<Long> memoryObservations;

    public Stats() {
        timeObservations = new ArrayList<Long>();
        memoryObservations = new ArrayList<Long>();
    }

    public void start() {
        startTime = System.currentTimeMillis();
        final Runtime runtime = Runtime.getRuntime();
        startMemory = runtime.totalMemory() - runtime.freeMemory();
    }

    public void observe() {
        timeObservations.add(System.currentTimeMillis());
        final Runtime runtime = Runtime.getRuntime();
        final long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        memoryObservations.add(usedMemory);
    }

    public List<Long> getTimeDiffs() {
        long last = startTime;
        final List<Long> diffs = new ArrayList<Long>(timeObservations.size());
        for(final Long obs : timeObservations) {
            diffs.add(obs - last);
            last = obs;
        }
        return diffs;
    }

    public List<Long> getMemoryUsage() {
        final List<Long> usages = new ArrayList<Long>(memoryObservations.size());
        for(final Long obs : memoryObservations) {
            usages.add(obs - startMemory);
        }
        return usages;
    }
}
