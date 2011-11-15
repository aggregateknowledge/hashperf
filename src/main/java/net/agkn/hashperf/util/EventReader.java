/******************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aggregate Knowledge - implementation
 ******************************************************************************/
package net.agkn.hashperf.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Reads csv-formatted, test-rig-formatted logs and converts all fields to
 * <code>long</code>s for packing/compression.
 *
 * Input CSV should have the following format:
 * person_id,...key fields... (longs).
 */
public class EventReader {
    private BufferedReader reader;
    private CsvRecordReader csv;

    /**
     * @param path the path to the input csv file
     */
    public EventReader(final String path) {
        try {
            reader = new BufferedReader(new FileReader(path), 8192 * 16 * 16 * 16/*32M, empirically 'fast enough'*/);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not find file " + path);
        }
        csv = new CsvRecordReader(',');
    }

    /**
     * Returns the next row of the CSV, parsed to <code>long</code>s or <code>null</code>
     * if EOF is reached.
     */
    public long[] next() {
        String line;
        try {
            line = reader.readLine();
        } catch(IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not read line.");
        }
        if(line == null) {
            try {
                reader.close();
            } catch(IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Could not close file.");
            }
            return null;
        } else {
            final String[] values = csv.readRecord(line);
            return parseValues(values);
        }
    }

    /**
     * Parses the line into longs. Expects <code>values</code> to be non-<code>null</code>.
     */
    private long[] parseValues(final String[] values) {
        final int length = values.length;
        final long[] out = new long[length];
        for(int i=0; i<length; i++) {
            out[i] = Long.parseLong(values[i]);
        }
        return out;
    }
}
