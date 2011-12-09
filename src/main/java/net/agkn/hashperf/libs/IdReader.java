/******************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aggregate Knowledge - implementation
 ******************************************************************************/
package net.agkn.hashperf.libs;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class IdReader {
    private BufferedReader reader;
    private String nextLine;

    public IdReader(final String path) {
        try {
            reader = new BufferedReader(new FileReader(path), 1024*1024*1024);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not find file " + path);
        }
        readNext();
    }

    private void readNext() {
        try {
            nextLine = reader.readLine();
        } catch(IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not read line.");
        }
        if(nextLine == null) {
            try {
                reader.close();
            } catch(IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Could not close file.");
            }
        }
    }

    /**
     * Returns whether or not it is valid to call {@link #next()}.
     */
    public boolean hasNext() {
        return nextLine != null;
    }

    /**
     * Returns the next id, parsed to a <code>long</code>.
     */
    public long next() {
        final long value = Long.valueOf(nextLine);
        readNext();
        return value;
    }
}
