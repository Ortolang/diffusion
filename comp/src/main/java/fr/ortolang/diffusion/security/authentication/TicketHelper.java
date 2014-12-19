package fr.ortolang.diffusion.security.authentication;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.*;
import java.io.*;
import java.security.DigestException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TicketHelper {

    private static final Logger logger = Logger.getLogger(TicketHelper.class.getName());

    private static final String ALGORITHM = "AES";
    private static final String ALGORITHM_MODE_PADDING = ALGORITHM + "/ECB/PKCS5Padding";
    private static final int KEY_SIZE = 128;
    private static final String MESSAGE_DIGEST_ALGORITHM = "MD5";
    private static SecretKey key;

    // Default ticket validity period in ms
    private static final long DEFAULT_TICKET_VALIDITY = 5 * 60000;

    static {
        try {
            KeyGenerator generator = KeyGenerator.getInstance(ALGORITHM);
            generator.init(KEY_SIZE);
            key = generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Error during TicketHelper initialization", e);
        }
    }

    public static String makeTicket(String username, String hash, long ticketValidity) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(ALGORITHM_MODE_PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            Ticket ticket = new Ticket(username, hash, ticketValidity);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(ticket);
            MessageDigest md = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
            byte[] digest = md.digest(bos.toByteArray());
            bos.write(digest);
            byte[] encryptedBytes = cipher.doFinal(bos.toByteArray());
            return Base64.encodeBase64URLSafeString(encryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException | BadPaddingException | IllegalBlockSizeException e) {
            logger.log(Level.SEVERE, "Error when making a ticket", e);
        }
        return "";
    }

    public static String makeTicket(String username, String hash) {
        return makeTicket(username, hash, DEFAULT_TICKET_VALIDITY);
    }

    public static Ticket decodeTicket(String ticket) {
        byte[] encryptedBytes = Base64.decodeBase64(ticket.getBytes());
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM_MODE_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            // Extract digest from decryptedBytes (MD5 length is 16 bytes)
            byte[] digest = Arrays.copyOfRange(decryptedBytes, decryptedBytes.length - 16, decryptedBytes.length);
            byte[] serializedMap = Arrays.copyOfRange(decryptedBytes, 0, decryptedBytes.length - 16);
            MessageDigest md = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
            if (!Arrays.equals(digest, md.digest(serializedMap))) {
                throw new DigestException();
            }
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serializedMap));
            return (Ticket) ois.readObject();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IOException | ClassNotFoundException | DigestException e) {
            logger.log(Level.SEVERE, "Error when decoding ticket", e);
        }
        return null;
    }

    public static class Ticket implements Serializable {

        private String username;
        private String hash;
        private long expiration;

        public Ticket(String username, String hash, long validity) {
            this.username = username;
            this.hash = hash;
            this.expiration = System.currentTimeMillis() + validity;
        }

        public String getUsername() {
            return username;
        }

        public String getHash() {
            return hash;
        }

        public long getExpiration() {
            return expiration;
        }

        public boolean validate(String hash) {
            if (!hash.equals(this.hash)) {
                logger.log(Level.FINE, "Ticket not valid for this hash");
                return false;
            }
            Date date = new Date(expiration);
            if (date.after(new Date())) {
                return true;
            }
            logger.log(Level.FINE, "Ticket validity has expired");
            return false;
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            this.username = ois.readUTF() ;
            this.hash = ois.readUTF() ;
            this.expiration = ois.readLong();
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.writeUTF(username) ;
            oos.writeUTF(hash) ;
            oos.writeLong(expiration); ;
        }

        @Override
        public String toString() {
            return "Ticket [username: " + username + ", hash: " + hash + ", valid until: " + new Date(expiration) +  "]";
        }
    }
}
