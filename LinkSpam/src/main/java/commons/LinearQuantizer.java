package commons;

import java.nio.ByteBuffer;

public class LinearQuantizer {
    private static int BYTE_SIZE = 8;
    private double precision;
    private int bytes;

    public LinearQuantizer(double precisionOrMax, int bytes, boolean precisionCalculated) {
        if(!precisionCalculated) {
            int intervals = (1 << (bytes * BYTE_SIZE)) - 1; // assuming no negative numbers. Assumption valid for page-rank values
            this.precision = precisionOrMax/intervals;
        } else {
            this.precision = precisionOrMax;
        }
        this.bytes = bytes;
    }

    public int getBytes() { return this.bytes; }
    public double getPrecision() { return this.precision; }

    public byte[] compressNum(double num) {
        int intRepresentation = (int) Math.ceil(num/precision);

        byte[] compressedNum = new byte[this.bytes];
        for(int i=0; i<bytes; i++) {
            int placesToShift = i * BYTE_SIZE;
            compressedNum[i] = (byte)((intRepresentation >> placesToShift) & 0xFF);
        }
        assert(compressedNum.length == this.bytes);
        return compressedNum;
    }

    // Will work only if desired number of bytes < 4.
    // If we would want >= 4 bytes for each double, we can just cast it to float
    public ByteBuffer compressArr(double[] arr) {
        // Buffer = Precision + BytesRepresented + Actual-Content
        ByteBuffer byteBuf = ByteBuffer.allocate(8 + 4 + 4 + arr.length * this.bytes);
        byteBuf.putDouble(this.precision);
        byteBuf.putInt(this.bytes);
        byteBuf.putInt(arr.length);
        for(int i=0; i<arr.length; i++) { byteBuf.put(compressNum(arr[i])); }
        return byteBuf;
    }

    public double decompressNum(byte[] bytes) {
        assert(bytes.length < Integer.SIZE/Byte.SIZE); // else there is no gain. We could have directly stored as 4 bytes floats
        int n = bytes.length;
        int temp;
        if(n == 1) temp = (int) bytes[0];
        else if(n == 2) temp = ((bytes[1] & 0xff) << 8) | (bytes[0] & 0xff);
        else temp = ((bytes[2] & 0xff) << 16) | ((bytes[1] & 0xff) << 8) | (bytes[0] & 0xff);
        return temp * this.precision;
    }
}