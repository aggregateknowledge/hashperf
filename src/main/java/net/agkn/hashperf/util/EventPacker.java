/******************************************************************************
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Aggregate Knowledge - implementation
 ******************************************************************************/
package net.agkn.hashperf.util;

/**
 * Simulates a hash of the key ids.
 */
public class EventPacker {
    /**
     * Pack an event from n + 1 <code>long</code>s down to 2 longs by XORing
     * everything but the first (the primary key) element of the event array.
     * @param event the event to pack
     * @return the packed event
     */
    public static long[] packEvent(final long[] event) {
        long acc = event[1];
        for(int i=2;i<event.length;i++) {
            acc ^= event[i];
        }
        return new long[] { event[0], acc };
    }
}
