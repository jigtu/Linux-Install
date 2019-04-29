package cn.com.cintel.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

/**
 * 描述：AES 文件加解密
 *
 * @author ZiQiang
 */
public class AESFileUtil {

    private static Logger logger = LoggerFactory.getLogger(AESFileUtil.class);


    /**
     * init AES Cipher
     */
    public static Cipher initAESCipher(String encodeRules, int cipherMode) {
        Cipher cipher = null;
        try {
            //1.构造密钥生成器，指定为AES算法,不区分大小写
            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            //2.根据encode Rules规则初始化密钥生成器
            //生成一个128位的随机源,根据传入的字节数组
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(encodeRules.getBytes());
            keygen.init(128, random);
            //3.产生原始对称密钥
            SecretKey originalKey = keygen.generateKey();
            //4.获得原始对称密钥的字节数组
            byte[] raw = originalKey.getEncoded();
            //5.根据字节数组生成AES密钥
            SecretKey key = new SecretKeySpec(raw, "AES");
            //6.根据指定算法AES自成密码器
            cipher = Cipher.getInstance("AES");
            //7.初始化密码器，第一个参数为加密(Encrypt_mode)或者解密解密(Decrypt_mode)操作，第二个参数为使用的KEY
            cipher.init(cipherMode, key);
        } catch (Exception e) {
            logger.debug("异常：" + e.getMessage());
            e.printStackTrace();
        }
        return cipher;
    }

    private static SecretKey getKey(String password) {
        int keyLength = 256;
        byte[] keyBytes = new byte[keyLength / 8];
        Arrays.fill(keyBytes, (byte) 0x0);
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        int length = passwordBytes.length < keyBytes.length ? passwordBytes.length : keyBytes.length;
        System.arraycopy(passwordBytes, 0, keyBytes, 0, length);
        return new SecretKeySpec(keyBytes, "AES");
    }


    /**
     * AES 加密
     *
     * @param encryptPath 要加密的文件
     * @param decryptPath 加密后的文件
     * @param encodeRules 加密秘钥
     * @return 加密结果
     */
    public static boolean encryptFile(String encryptPath, String decryptPath, String encodeRules) {
        File encryptFile;
        File decryptFile = null;
        CipherOutputStream cipherOutputStream;
        BufferedInputStream bufferedInputStream;
        try {
            encryptFile = new File(encryptPath);
            if (!encryptFile.exists()) {
                logger.debug("Encrypt file is empty");
                throw new NullPointerException("Encrypt file is empty");
            }
            decryptFile = new File(decryptPath);
            if (decryptFile.exists()) {
                boolean delete = decryptFile.delete();
                logger.debug("delete existing files :" + delete);
            }
            boolean newFile = decryptFile.createNewFile();
            logger.debug("create a file :" + newFile);
            Cipher cipher = initAESCipher(encodeRules, Cipher.ENCRYPT_MODE);
            cipherOutputStream = new CipherOutputStream(new FileOutputStream(decryptFile), cipher);
            bufferedInputStream = new BufferedInputStream(new FileInputStream(encryptFile));
            byte[] buffer = new byte[1024];
            int bufferLength;
            while ((bufferLength = bufferedInputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bufferLength);
            }
            bufferedInputStream.close();
            cipherOutputStream.close();
        } catch (IOException e) {
            // 异常后 删除加密后的文件（属于半成品）
            boolean flag = delFile(decryptFile.getAbsolutePath());
            e.printStackTrace();
            logger.debug("异常：" + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * AES 解密
     *
     * @param encryptPath 加密的文件路径
     * @param decryptPath 解密的文件路径
     * @param mKey        解密秘钥
     * @return 解密结果
     */

    public static boolean decryptFile(String encryptPath, String decryptPath, String mKey) {
        File encryptFile;
        File decryptFile = null;
        BufferedOutputStream outputStream;
        CipherInputStream inputStream;
        try {
            encryptFile = new File(encryptPath);
            if (!encryptFile.exists()) {
                logger.debug("Decrypt file is empty");
                throw new NullPointerException("Decrypt file is empty");
            }
            decryptFile = new File(decryptPath);
            if (decryptFile.exists()) {
                boolean delete = decryptFile.delete();
                logger.debug("delete existing files :" + delete);
            }
            boolean newFile = decryptFile.createNewFile();

            Cipher cipher = initAESCipher(mKey, Cipher.DECRYPT_MODE);

            outputStream = new BufferedOutputStream(new FileOutputStream(decryptFile));
            inputStream = new CipherInputStream(new FileInputStream(encryptFile), cipher);

            int bufferLength;
            byte[] buffer = new byte[1024];

            while ((bufferLength = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bufferLength);
            }
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            boolean flag = delFile(decryptFile.getAbsolutePath());
            e.printStackTrace();
            return false;
        }
        return true;
    }


    /**
     * delete File
     *
     * @param pathFile 待删除的文件路径
     * @return 删除结果
     */
    public static boolean delFile(String pathFile) {
        boolean flag = false;
        if (pathFile == null || pathFile.length() <= 0) {
            throw new NullPointerException("文件不能为空");
        } else {
            File file = new File(pathFile);
            // 路径为文件且不为空则进行删除
            if (file.isFile() && file.exists()) {
                flag = file.delete();
            }
        }
        return flag;
    }


    public static void main(String[] args) {
        //加密盐值  双方统一使用，请勿擅自修改
        String encodeRules = "sjdq@GAB";
        //文件加密
        boolean flag1 = AESFileUtil.encryptFile("D:\\阿里ak.txt", "D:\\阿里ak.json", encodeRules);
        System.out.println(flag1 ? "加密成功" : "加密失败");
        //文件解密
        // boolean flag2 = AESFileUtil.decryptFile("D:\\logfile\\encr\\20190411_20190412154140010.txt", "D:\\logfile\\decr\\20190411_20190412154140010.txt", encodeRules);
        // System.out.println(flag2 ? "解密成功" : "解密失败");
    }
}