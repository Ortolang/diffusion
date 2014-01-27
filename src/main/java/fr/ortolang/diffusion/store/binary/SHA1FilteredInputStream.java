package fr.ortolang.diffusion.store.binary;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1FilteredInputStream extends FilterInputStream {
	
	private MessageDigest digest;
 
    public SHA1FilteredInputStream (InputStream in) throws NoSuchAlgorithmException {
        super(in);
        digest = MessageDigest.getInstance("SHA-1"); 
    }
 
    @Override 
    public int read() throws IOException {
        int c = in.read();
        if (c == -1) {
            return -1;
        }
        digest.update((byte)(c & 0xff));
        return c;
    }
 
    @Override 
    public int read(byte[] bytes, int offset, int length) throws IOException {
        int r;
        if ((r = in.read(bytes, offset, length)) == -1) {
            return r;
        }
        digest.update(bytes, offset, r);
        return r;
    }
 
    public byte[] getHash(){
    	return digest.digest();
    }
 
    public String getHashString(){
    	BigInteger bi = new BigInteger(1, digest.digest());
		return bi.toString(16);
    }
}