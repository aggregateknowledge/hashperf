/******************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aggregate Knowledge - implementation
 ******************************************************************************/
package net.agkn.hashperf.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

import com.sleepycat.db.Database;
import com.sleepycat.db.DatabaseConfig;
import com.sleepycat.db.DatabaseEntry;
import com.sleepycat.db.DatabaseException;
import com.sleepycat.db.DatabaseType;
import com.sleepycat.db.Environment;
import com.sleepycat.db.EnvironmentConfig;

import net.agkn.hashperf.IPerformanceTest;
import net.agkn.hashperf.util.RecordIterator;

/**
 * Tests the performance of BDB.
 *
 * The source to Berkeley DB may be found at http://www.oracle.com/technetwork/database/berkeleydb/downloads/index.html.
 *
 * The license to Berkeley DB may be found at http://www.oracle.com/technetwork/database/berkeleydb/downloads/oslicense-093458.html
 */
public class BDBPerformanceTest implements IPerformanceTest {
    private Database db;
    private Environment env;

    /**
     * Builds a in-memory BDB Hash database.
     */
    private void buildDB() {
        EnvironmentConfig envConfig = new EnvironmentConfig();

        envConfig.setAllowCreate(true);/*allow creation*/
        envConfig.setPrivate(true);/*only one process*/
        envConfig.setNoLocking(true);/*no need for locks when only one process*/
        envConfig.setTransactional(false);/*no transactions*/

        envConfig.setCacheSize(10000000000L);/*10G memory pools*/
        envConfig.setCacheMax(10000000000L);/*10G max memory*/
        envConfig.setCacheCount(1);/*only a single memory pool*/
        envConfig.setInitializeCache(true)/*DB_INIT_MPOOL, start up the memory pool subsystem*/;

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);/*allow creation*/
        dbConfig.setType(DatabaseType.HASH);/*HASH db*/

        // Our data set has a bit over 78 million unique elements
        dbConfig.setHashNumElements(78000000);/*estimated number of elements*/

        // Fill factor is recommended to be:
        // (pagesize - 32)/(average_key_size + average_data_size + 8)
        // 
        // pagesize = 4096 on m2.4xlarge, AMI ami-31d41658 (Standard Redhat 6.1 64-bit)
        // $ getconf PAGESIZE
        // 4096
        // 
        // for my data:
        // average_key_size = 8 bytes
        // average_data_size = 2 longs * 2 = 32 bytes
        // (4064)/(8 + 32 + 8) = 4064/48 = 508
        // SEE:  http://download.oracle.com/docs/cd/E17076_02/html/programmer_reference/hash_conf.html
        dbConfig.setHashFillFactor(508);

        dbConfig.setReadUncommitted(true);/*dirty reads are fine*/
        dbConfig.setTransactional(false);/*no need for transactions on one process*/

        File f = new File("/tmp/bdb_performance_test.db");
        f.mkdirs();

        try {
            env = new Environment(f, envConfig);
            db = env.openDatabase(null, null, null, dbConfig);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not find BDB file.");
        } catch(DatabaseException e) {
            e.printStackTrace();
            throw new RuntimeException("BDB pooped the bed.");
        }
    }

    /* (non-Javadoc)
     * @see net.agkn.hashperf.IPerformanceTest#doRun(RecordIterator)
     */
    public void doRun(final RecordIterator iter) {
        buildDB();

        try {
            iter.start();
            while(iter.hasNext()) {
                DatabaseEntry keyDBE = new DatabaseEntry(iter.nextPersonId());
                DatabaseEntry valueDBE = new DatabaseEntry();

                db.get(null, keyDBE, valueDBE, null);

                byte[] newdata = iter.nextEvent();
                byte[] olddata = valueDBE.getData();

                if(olddata == null) {
                    valueDBE.setData(newdata);
                } else {
                    byte[] outdata = Arrays.copyOf(olddata, olddata.length + newdata.length);
                    System.arraycopy(newdata, 0, outdata, olddata.length, newdata.length);
                    valueDBE.setData(outdata);
                }

                db.put(null, keyDBE, valueDBE);
                iter.next();
            }
            iter.stop();
        } catch(DatabaseException e) {
            e.printStackTrace();
            throw new RuntimeException("BDB couldn't get/put.");
        } finally {
            // make absolutely sure that the memory allocated can be reused
            try {
                db.truncate(null, false);
                db.close();
                env.close();
            } catch(DatabaseException e) {
                e.printStackTrace();
                throw new RuntimeException("BDB couldn't close.");
            }
        }
    }
}
