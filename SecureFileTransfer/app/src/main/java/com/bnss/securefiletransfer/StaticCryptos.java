package com.bnss.securefiletransfer;

import android.util.Base64;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.Security;

import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


/**
 *
 * Created by luceat on 3/3/15.
 */
public class StaticCryptos {

    final static String aesSetting = "AES/CBC/PKCS5Padding", rsaSetting = "RSA/ECB/PKCS1Padding";

static {
    Security.addProvider(new BouncyCastleProvider());
}

    /*
    Encrypt the symmetric key using RSA public key.
    Uses the Cipher.WRAP_MODE which is basically encrypt for keys (key wrapping)
     */
    public static byte[] encSymKey(SecretKey sym_key, String publicKey) {
        byte[] encryptedSymKey = null;
        try {
            System.out.println("******************************************************");
            System.out.println("In encSymKey (  )");
          byte[] pubBytes = publicKey.substring(27, publicKey.length() -25).getBytes();

            PublicKey pubKey = KeyFactory.getInstance("RSA","SC").generatePublic(
                    new X509EncodedKeySpec(Base64.decode(pubBytes, Base64.DEFAULT)));
            System.out.println("PublicKey is ="  + publicKey);

            Cipher rsaCipher = Cipher.getInstance(rsaSetting,"AndroidOpenSSL");
            rsaCipher.init(Cipher.WRAP_MODE, pubKey);
            encryptedSymKey = rsaCipher.wrap(sym_key);
            System.out.println("Before Encrypting symkey = " + sym_key.getEncoded());
            System.out.println("after Encrypting symkey = " + encryptedSymKey);

            System.out.println("******************************************************");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return encryptedSymKey;
    }

    /*
    Decrypt the symmetric key using RSA Private key.
    Uses the Cipher.UNWRAP_MODE which is basically decrypt for keys (key wrapping)
     */
    public static Key decSymKey(byte[] enc_sym_key, PrivateKey privatekey) {
        Key decryptedKey = null;
        if(privatekey == null){
            System.out.println("******************************************************");
            System.out.println("IN decSymKey ( ) ");
            System.out.println("privatekey is null =(");
            return null;
        }
        try {
            System.out.println("******************************************************");
            System.out.println("IN decSymKey ( ) ");
            System.out.println("Before decrypt sym_key length = " + enc_sym_key.length);
            System.out.println("Before decrypt sym_key = " + new String(enc_sym_key));

            Cipher rsaCipher = Cipher.getInstance(rsaSetting, "AndroidOpenSSL");
            rsaCipher.init(Cipher.UNWRAP_MODE, privatekey);
            decryptedKey = rsaCipher.unwrap(enc_sym_key, aesSetting, Cipher.SECRET_KEY);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        //System.out.println("After decrypt sym_key getEncoded = " + decryptedKey.getEncoded());
        System.out.println("After decrypt sym_key  getAlgorithm = " + decryptedKey.getAlgorithm());
        System.out.println("After decrypt sym_key  toString = " + new String(decryptedKey.getEncoded()));
        System.out.println("After decrypt sym_key  getFormat = " + decryptedKey.getFormat());
        System.out.println("After decrypt sym_key  getEncoded = " + decryptedKey.getEncoded());
        System.out.println("******************************************************");
        return decryptedKey;
    }

    public static byte[][] encrypt(Key sym_key, byte[] data) {
        byte[][] ret = new byte[2][];
        try {
            System.out.println("******************************************************");
            System.out.println("IN encrypt() ");
            System.out.println("data length before encrypt: " + data.length);
            Cipher cipher = Cipher.getInstance(aesSetting, "SC");
            cipher.init(Cipher.ENCRYPT_MODE, sym_key);
            ret[0] = cipher.doFinal(data);
            ret[1] = cipher.getIV();
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("data after encrypt: " + ret[0].length);
        System.out.println("iv after encrypt: " + Base64.encodeToString(ret[1], Base64.DEFAULT));
        System.out.println("iv.length after encrypt: " + ret[1].length);
        System.out.println("******************************************************");
        return ret;
    }

    public static byte[] decrypt(Key sym_key, byte[] enc_data, byte[] iv) {
        byte[] data = null;
        try {
            System.out.println("******************************************************");
            System.out.println("In decrypt (    )");
            System.out.println("data length before encrypt: " + enc_data.length);
            System.out.println("sym_key.getFormat: " + sym_key.getFormat());
            System.out.println("sym_key.getAlgorithm() " + sym_key.getAlgorithm());
            System.out.println("sym_key.toString: " + sym_key.toString());

            Cipher cipher = Cipher.getInstance(aesSetting,"SC");
            cipher.init(Cipher.DECRYPT_MODE,sym_key, new IvParameterSpec(iv));
            data = cipher.doFinal(enc_data);
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("******************************************************");
        return data;
    }

    public static SecretKey generateSymKey() {
        byte[] key = null;
        SecretKey skey = null;
        try {

            KeyGenerator kgen = KeyGenerator.getInstance("AES", "SC");
            kgen.init(256); // 192 and 256 bits may not be available
            skey = kgen.generateKey();
            System.out.println("******************************************************");
            System.out.println("generateSymKey()) ");
            System.out.println("Base64.encodeToString(skey.getEncoded()"  + Base64.encodeToString(skey.getEncoded(), Base64.DEFAULT));
            System.out.println("skey  toString() " + skey.toString());
            System.out.println("skey  getAlgorithm() " + skey.getAlgorithm());
            System.out.println("skey  length " + skey.getEncoded().length);
            System.out.println("******************************************************");
        } catch (Exception e){
            e.printStackTrace();
        }
        return skey;
    }
}
