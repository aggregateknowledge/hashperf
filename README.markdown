What
====

A rig for comparing the performance of various hash tables in Java. Used to produce the results for [this blog post](http://blog.aggregateknowledge.com/2011/11/15/big-memory-part-3/) on hash table 'services' and [this blog post](http://blog.aggregateknowledge.com/2011/12/12/big-memory-part-4/) on Java hash table 'libraries'.

It's neither the prettiest code, nor a rigorous benchmark. It was written to get a solid feel for the relative performance of these packages for our particular workload. See the links above for more details.

Setup
=====

* Go to EC2 and provision a `m2.4xlarge` instance with AMI `ami-31d41658` (Standard Redhat 6.1 64-bit).
* `ssh` in as root.
* Download Maven 3.0.x binaries from [a mirror](http://www.apache.org/dyn/closer.cgi/maven/binaries/apache-maven-3.0.3-bin.tar.gz) and install per [the instructions](http://maven.apache.org/download.html).

If you want to set up the hash table 'services' comparison:
-----------------------------------------------------------

* `yum install gcc-c++ zlib-devel java-1.6.0-openjdk-devel.x86_64`
* `export JAVA_HOME=/usr/lib/jvm/java-1.6.0-openjdk.x86_64`

* Download the sources for the data stores and install them:

  * [redis-2.4.2](http://redis.googlecode.com/files/redis-2.4.2.tar.gz)

    ```
        make
        make install
    ```
  
  * [kyotocabinet-1.2.70](http://fallabs.com/kyotocabinet/pkg/kyotocabinet-1.2.70.tar.gz) 

    ```
        ./configure
        make
        make install
    ```
  
  * [kyotocabinet-java-1.21](http://fallabs.com/kyotocabinet/javapkg/kyotocabinet-java-1.21.tar.gz)

    ```
        # make sure you have $JAVA_HOME set appropriately!
        ./configure
        make
        make install
    ```

  * [berkeley-db-5.2.36](http://download.oracle.com/otn/berkeley-db/db-5.2.36.tar.gz) (You'll need to log in to get this one.)

    ```
        cd build_unix
        ../dist/configure --enable-java
        make
        make install
    ```

* Add `/usr/local/lib/` to `/etc/ld.so.conf` so that Java can see the shared Kyoto Cabinet libraries.
* Run a `ldconfig` to make sure the links and caches are fresh.

If you want to set up the hash table 'libraries' comparison:
------------------------------------------------------------

* `yum install java-1.6.0-openjdk-devel.x86_64`

Run
===

* Get the test code source

    ```
      git clone git://github.com/aggregateknowledge/hashperf.git
    ```

* Grab dependencies and compile

    ```
      cd hashperf
      mvn compile
      mkdir deps
      mvn dependency:copy-dependencies -DoutputDirectory=deps
    ```

If you want to run the hash table 'services' comparison:
--------------------------------------------------------

* Start the daemons
      
    ```
      /path/to/redis-server /path/to/hashperf/redis.conf
    ```

* Run!

    ```
      java -server -Djava.library.path=/usr/local/lib/:/usr/local/BerkeleyDB.5.2/lib/ -classpath deps/*:target/classes net.agkn.hashperf.services.FullPerformanceTestSuite /path/to/data.csv /path/to/stats/dir/ warmupCount obsCount pollingInterval
    ```

    in my case, this was:

    ```
      mkdir /dev/shm/stats/
      mv data.csv /dev/shm/hash_test.csv
      java -server -Djava.library.path=/usr/local/lib/:/usr/local/BerkeleyDB.5.2/lib/ -classpath deps/*:target/classes net.agkn.hashperf.services.FullPerformanceTestSuite /dev/shm/hash_test.csv /dev/shm/stats/ 10 30 1000000
    ```

Note that the line count in your test file divided by the `pollingInterval` should be less than or equal to `net.agkn.hashperf.util.RecordIterator#POLL_COUNT`.

If you want to run the hash table 'libraries' comparison:
---------------------------------------------------------

* Run!

    ```
      java -server -XmxNNNg -classpath deps/*:target/classes net.agkn.hashperf.libs.PerformanceTestSuite /path/to/data.csv /path/to/stats/dir/ warmupCount obsCount pollingInterval sizeHint
    ```

    in my case, an example of this was:

    ```
      mkdir /dev/shm/stats/
      mv data.csv /dev/shm/hash_test.csv
      java -server -Xmx50g -classpath deps/*:target/classes net.agkn.hashperf.libs.PerformanceTestSuite /dev/shm/hash_test.csv /dev/shm/stats/ 2 2 10000000 976000000
    ```

License
======================

This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.

*Contributors:*

Aggregate Knowledge - implementation
