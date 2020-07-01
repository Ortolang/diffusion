package fr.ortolang.diffusion.store.binary.hash;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;

public class MD5FilterInputStream extends HashedFilterInputStream {

    private MessageDigest digest;

    protected MD5FilterInputStream (InputStream in) throws NoSuchAlgorithmException {
        super(in);
        digest = MessageDigest.getInstance("MD5");
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
    public int read(@Nonnull byte[] bytes, int offset, int length) throws IOException {
        int r;
        if ((r = in.read(bytes, offset, length)) == -1) {
            return r;
        }
        digest.update(bytes, offset, r);
        return r;
    }

    @Override
    public String getHash(){
        BigInteger bi = new BigInteger(1, digest.digest());
        return bi.toString(16);
    }
}
