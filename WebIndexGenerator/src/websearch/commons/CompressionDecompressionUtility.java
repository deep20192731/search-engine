package websearch.commons;

import java.util.ArrayList;
import java.util.List;



public class CompressionDecompressionUtility {
	private static int BASE = 128;
	private static int SIGNIFICANT_BITS = 7;
	
	
	public static List<Byte> getBytesForInt(int num) {
		List<Byte> bytes = new ArrayList<Byte>();
		bytes.add((byte) (num >>> 24));
		bytes.add((byte) (num >>> 16));
		bytes.add((byte) (num >>> 8));
		bytes.add((byte) (num));
		return bytes;
	}
	
	// Overloaded fn
	public static byte[] getBytesForIntAsArr(int num) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) (num >>> 24);
		bytes[1] = (byte) (num >>> 16);
		bytes[2] = (byte) (num >>> 8);
		bytes[3] = (byte) (num);
		return bytes;
	}
	
	public static List<Integer> compressWithDifference(List<Integer> baseArr) {
		List<Integer> toReturn = new ArrayList<Integer>();
		toReturn.add(baseArr.get(0));
		for(int i=1; i<baseArr.size(); i++) {
			toReturn.add(i, baseArr.get(i)-baseArr.get(i-1));
		}
		return toReturn;
	}
	
	public static List<Integer> decompressWithDifferences(List<Integer> arr) {
		List<Integer> toReturn = new ArrayList<Integer>();
		for(int i=0; i<arr.size(); i++) {
			if(i == 0) toReturn.add(arr.get(0));
			else toReturn.add(arr.get(i) + toReturn.get(i-1));
		}
		return toReturn;
	}
	
	public static List<Byte> compressArrWithVarByte(List<Integer> arr) {
		List<Byte> baseBytes = new ArrayList<Byte>();
		for(int i=0; i<arr.size(); i++) {
			baseBytes.addAll(compressIntWithVarByte(arr.get(i)));
		}
		
		return baseBytes;
	}
	
	public static byte[] compressArrWithVarByteAsArr(List<Integer> arr) {
		List<Byte> baseBytes = new ArrayList<Byte>();
		for(int i=0; i<arr.size(); i++) {
			baseBytes.addAll(compressIntWithVarByte(arr.get(i)));
		}
		
		byte[] bytes = new byte[baseBytes.size()];
		for(int i=0; i<baseBytes.size(); i++)
			bytes[i] = baseBytes.get(i);
		return bytes;
	}
	
	public static List<Byte> compress(int num) {
		List<Byte> bytes = new ArrayList<Byte>();
		boolean firstByte = true;
		while(num > 127) {
			if(firstByte) {
				bytes.add((byte) (num&127));
				firstByte = false;
			} else
				bytes.add((byte) ((num&127)|128));
			num >>= 7;
		}
		
		if(firstByte) {
			bytes.add((byte) (num&127));
			firstByte = false;
		} else
			bytes.add((byte) ((num&127)|128));
		
		return bytes;
	}
	
	// Assumption - Numbers are less than 268435456 (268million)
	public static List<Byte> compressIntWithVarByte(int num) {
		List<Byte> bytes = new ArrayList<Byte>();
		
		int shiftBits = SIGNIFICANT_BITS*3;
		int radix = BASE - 1;
		
		byte b;
		while(shiftBits >= 0) {
			if((b = (byte) ((num >>> shiftBits) & radix)) > 0 || shiftBits == 0) {
				if(shiftBits == 0)
					bytes.add(b);
				else
					bytes.add((byte) (b | BASE));
			}
			
			shiftBits -= 7;
		}
		return bytes;
	}
	
	public static List<Integer> decompressBytesWithVarByte(byte[] bytes) {
		List<Integer> ints = new ArrayList<Integer>();
		int radix = BASE - 1;
		
		List<Integer> tempNumbers = new ArrayList<Integer>();
		
		for(int i=0; i<bytes.length; i++) {
			byte b = bytes[i];
			tempNumbers.add(b & radix);
			if(b >= 0) {
				// means number formed. Accumulate the complete num from templist
				int actualNum = 0;
				for(int j = 0, k=tempNumbers.size()-1; j < tempNumbers.size(); j++, k--) {
					actualNum += (tempNumbers.get(j) * Math.pow(BASE, k));
				}
				
				ints.add(actualNum);
				tempNumbers.clear();
			}
		}
		return ints;
	}
	
	public static byte getTwosComplement(byte b) {
		return (byte) ((~b) + 1);
	}
	
	public static void main(String[] args) {
		List<Integer> a = new ArrayList<Integer>();
		a.add(20224);
		a.add(2);
		a.add(1);
		a.add(20352);
		a.add(11);
		
		List<Byte> bytes = compressArrWithVarByte(a);
		byte[] b = new byte[bytes.size()];
		
		//System.out.println(b.length);
		
		for(int j=0; j<bytes.size(); j++) {
			b[j] = bytes.get(j);
			//System.out.println(String.format("%8s", Integer.toBinaryString(bytes.get(j) & 0xFF)).replace(' ', '0'));
		}
		
		//System.out.println(String.format("%8s", Integer.toBinaryString((byte)127 & 0xFF)).replace(' ', '0'));
		List<Integer> i = decompressBytesWithVarByte(b);
		
		for(Integer d : i)
			System.out.println(d);		
	}
}
