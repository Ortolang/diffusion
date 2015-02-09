package fr.ortolang.diffusion.store.binary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BinaryStoreVolumeMapper {
	
	private static final String PREFIX = "volume";
	
	private static Map<String, String> mapping = new HashMap<String, String> ();
	private static List<String> volumes = new ArrayList<String> ();
	
	static {
		int n = Integer.parseInt("10", 16);
		int v = 1;
		while ( n <= Integer.parseInt("ff", 16) ) {
			volumes.add(PREFIX + v);
			for (int i=0; i<31; i++) {
				if ( n <= Integer.parseInt("ff", 16) ) {
					mapping.put(Integer.toHexString(n), PREFIX + v);
					n++;
				}
			}
			v++;
		}
	}
	
	public static String getVolume(String digit) throws VolumeNotFoundException {
		if ( mapping.containsKey(digit) ) {
			return mapping.get(digit);
		} else {
			throw new VolumeNotFoundException("No volume mapped for digit: " + digit);
		}
	}
	
	public static List<String> listVolumes() {
		return volumes;
	}
	
	public static void main(String[] args) throws VolumeNotFoundException {
		System.out.println(BinaryStoreVolumeMapper.getVolume("b1"));
	}

}
