package fr.ortolang.diffusion.authentication;

import org.junit.Test;

import fr.ortolang.diffusion.security.authentication.TOTPHelper;

public class TOTPHelperTest {
    
    @Test
    public void testTotp() {
        String secret = TOTPHelper.generateSecret();
        System.out.println("secret: " + secret);

        int code1 = TOTPHelper.getCode(secret);
        System.out.println("code1: " + code1);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            //
        }
        int code2 = TOTPHelper.getCode(secret);
        System.out.println("code2: " + code2);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            //
        }
        int code3 = TOTPHelper.getCode(secret);
        System.out.println("code3: " + code3);
        
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            //
        }        
        boolean valid1 = TOTPHelper.checkCode(secret, code1);
        boolean valid2 = TOTPHelper.checkCode(secret, code2);
        boolean valid3 = TOTPHelper.checkCode(secret, code3);
        System.out.println("valid1?" + valid1);
        System.out.println("valid2?" + valid2);
        System.out.println("valid3?" + valid3);
        
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            //
        }
        valid1 = TOTPHelper.checkCode(secret, code1);
        valid2 = TOTPHelper.checkCode(secret, code2);
        valid3 = TOTPHelper.checkCode(secret, code3);
        System.out.println("valid1?" + valid1);
        System.out.println("valid2?" + valid2);
        System.out.println("valid3?" + valid3);
    }

}
