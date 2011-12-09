package net.agkn.hashperf.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongOpenHashBigSet;

public class Counter {
    final private String filePath;
    final private long interval;
    final private List<Long> uniques;
    final private LongOpenHashBigSet set;

    public Counter(final String filePath) {
        this.filePath = filePath;
        this.interval = 10000000L;
        this.uniques = new ArrayList<Long>(1000);
        this.set = new LongOpenHashBigSet(1610612736, (float)0.75);
    }

    public void count() {
        final BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filePath), 8192 * 16 * 16 * 16/*32M, empirically 'fast enough'*/);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not find file " + filePath);
        }

        try {
            long counter = 0L;
            String line = reader.readLine();

            while(line != null) {
                set.add(Long.valueOf(line));
                counter += 1;
                line = reader.readLine();

                if(counter % interval == 0) {
                    final long size = set.size64();
                    System.err.println(counter + " - " + size);
                    uniques.add(size);
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Counter died.");
        }
    }

    static public void main(String[] args) {
        final Counter c = new Counter(args[0]);
        c.count();

        final String outfilePath = args[1];
        FileWriter output;

        try {
            output = new FileWriter(outfilePath);
            output.write("rec_no,unique_count,unique_diff\n");
            output.flush();

            long lastUniqueCount = 0;
            for(int i=0; i<c.uniques.size(); i++) {
                final long currentUniques = c.uniques.get(i);
                output.write(((i + 1) * c.interval) + "," + currentUniques + "," + (currentUniques - lastUniqueCount) + "\n");
                output.flush();
                lastUniqueCount = currentUniques;
            }
            output.close();
        } catch(IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't open file " + outfilePath);
        }
    }
}
