package com.yuqing.magic.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;

/**
 * MD5工具.
 */
public class MD5Util {

    private static final Logger logger = LoggerFactory.getLogger(MD5Util.class);

    private static final int BUFFER_LENGTH = 4096;

    private static final char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String encode(String s) {
        return encode(s.getBytes());
    }

    public static String encode(byte data[]) {
        try {
            byte[] btInput = data;
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = HEX_DIGITS[byte0 >>> 4 & 0xf];
                str[k++] = HEX_DIGITS[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
    }

    public static String encode(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return encode(fis);
        } catch (FileNotFoundException e) {
            logger.error("", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        }
        return null;
    }

    public static String encode(InputStream input) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buffer[] = new byte[BUFFER_LENGTH];
        int readLength = 0;
        try {
            while ((readLength = input.read(buffer)) != -1) {
                baos.write(buffer, 0, readLength);
            }
            return encode(baos.toByteArray());
        } catch (IOException e) {
            logger.error("", e);
        }
        return null;
    }
}
