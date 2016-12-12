package commons;

import org.apache.tika.io.IOExceptionWithCause;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommonUtilities {
    private static Properties CONF_FILE = null;
    private static final String CONF_FILE_NAME = "/resources/link-spam.properties";
    private static final Logger LOGGER = Logger.getLogger(CommonUtilities.class.getName());

    public static Properties getConfFile() {
        if(CONF_FILE != null) return CONF_FILE;

        CONF_FILE = new Properties();
        try {
            CONF_FILE.load(CommonUtilities.class.getResourceAsStream(CONF_FILE_NAME));
        } catch(IOException e) {
            LOGGER.log(Level.SEVERE, "Not able to open the config file " + CONF_FILE_NAME);
        }
        return CONF_FILE;
    }

    public static double getMinimum(double[] arr) {
        double min = Double.MAX_VALUE;
        for(int i=0; i<arr.length; i++) {
            if(arr[i] < min) min = arr[i];
        }
        return min;
    }

    public static double getMaximum(double[] arr) {
        double max = Double.MIN_VALUE;
        for(int i=0; i<arr.length; i++) {
            if(arr[i] > max) max = arr[i];
        }
        return max;
    }

    public static double[] readVectorFromFile(String file) throws IOException {
        RandomAccessFile f = new RandomAccessFile(file, "r");
        byte[] b = new byte[(int)f.length()];
        f.readFully(b);

        ByteBuffer buf = ByteBuffer.wrap(b);
        double precision = buf.getDouble();
        int bytes = buf.getInt();
        int total = buf.getInt();

        LinearQuantizer lq = new LinearQuantizer(precision, bytes, true);

        byte[] dummy = new byte[bytes];
        double[] nums = new double[total];
        for(int i=0; i<nums.length; i++) {
            buf.get(dummy);
            nums[i] = lq.decompressNum(dummy);
        }

        f.close();
        return nums;
    }

    public static void saveBytesToFile(String file, ByteBuffer buffer) throws IOException {
        // Converts double to float to save space. We will have enough precision with float
        DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(file));
        outputStream.write(buffer.array());

        outputStream.flush();
        outputStream.close();
    }
}