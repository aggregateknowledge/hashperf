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
 * Byte manipulation utilities.
 */
public class ByteUtil {
    /**
     * Writes a <code>long</code> as <code>byte</code>s into the provided array.
     * @param value the <code>long</code> to encode
     * @param array the write target
     * @param offset the offset in the array at which to write <code>value</code>
     */
    public static void longToBytes(long value, byte[] array, int offset) {
        array[offset + 0]   = (byte)(0xff & (value >> 56));
        array[offset + 1] = (byte)(0xff & (value >> 48));
        array[offset + 2] = (byte)(0xff & (value >> 40));
        array[offset + 3] = (byte)(0xff & (value >> 32));
        array[offset + 4] = (byte)(0xff & (value >> 24));
        array[offset + 5] = (byte)(0xff & (value >> 16));
        array[offset + 6] = (byte)(0xff & (value >> 8));
        array[offset + 7] = (byte)(0xff & value);
    }

    /**
     * Reconstructs a <code>long</code> from <code>byte</code>s that was encoded
     * with {@link #longToBytes(long, byte[], int)}.
     *
     * @param  array the array of bytes
     * @param  offset the offset in <code>array</code> to start reading from
     * @return the encoded <code>long</code>
     */
    public static long bytesToLong(byte[] array, int offset) {
        long value = (long)array[offset + 0] & 0x00000000000000ff;
        value = (value << 8) | (array[offset + 1] & 0x00000000000000ff);
        value = (value << 8) | (array[offset + 2] & 0x00000000000000ff);
        value = (value << 8) | (array[offset + 3] & 0x00000000000000ff);
        value = (value << 8) | (array[offset + 4] & 0x00000000000000ff);
        value = (value << 8) | (array[offset + 5] & 0x00000000000000ff);
        value = (value << 8) | (array[offset + 6] & 0x00000000000000ff);
        value = (value << 8) | (array[offset + 7] & 0x00000000000000ff);

        return value;
    }
}
