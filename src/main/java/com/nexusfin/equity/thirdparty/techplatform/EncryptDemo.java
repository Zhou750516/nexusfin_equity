package com.example.helloworld.encrypt;

import com.alibaba.fastjson.JSONObject;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.apache.commons.lang3.StringUtils;
import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description 加签、加密、验签、解密 demo
 * @Date 2020/12/28
 * @Version V1.0
 */
public class EncryptDemo {

    private static String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQD1J+plY+T5IKFKH3osuumP4ttT45ekC/n7g/Qcw4Xph9VMkipLRpyZBgmCAoF3JN8y562/7T8iHIHC7RSkmN5JmDVYdjC3KdaOkcZKrhNXxwGPqF8pHNblvVqnDoIHhHncmqtRNbVnxQZ8FvYClkbEyNYhs7Vv+HB7Zv3mtxVMjQIDAQAB";
    private static String PRIVATE_KEY = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAPUn6mVj5PkgoUofeiy66Y/i21Pjl6QL+fuD9BzDhemH1UySKktGnJkGCYICgXck3zLnrb/tPyIcgcLtFKSY3kmYNVh2MLcp1o6RxkquE1fHAY+oXykc1uW9WqcOggeEedyaq1E1tWfFBnwW9gKWRsTI1iGztW/4cHtm/ea3FUyNAgMBAAECgYByuWwyaG4Fu6Re+J4HAq08PXBgMJiNF2Gztwtszn8DSyKhQ6FxOqkb3zxhz+RgKiwdOVgRAehSgT6kRqgrYrJamdqeDxEUb3R05bM8NohL8eYwnTkiCuOSmUQZZkZoCEb7iMV4kxxY7F0miwD1L3xLHU+0OMKxvI8Xqt4NndibDQJBAPxiV7nnDWl/lSFfA7ygGoK/Rd2cgBJj4mDpmDmYl0W1u09mFFmGJAauDgxpm40x+oG9BwYPJf0bGtQmEXKp82sCQQD4qw/yXBTGSKoSmt0NecvVxBuahAd0qYIDrhyxeKgw0V7KMqMmaraJWTcbZ9T0t+oPDFY/k2uRmd9VmiKL4LXnAkEA3n8UN5+qA0emgTTyZmf+9yUIwsYlYhEMkcNdn+kV8y6zYtvrNME9IWZwnCC+MUvOPHIYrw7LvscWb9DfsOaC4QJBANSjKqK7XMRllJEOPiJs2QG1jUkmx11aKaRN+ZlErMX5802B2YNbUzXFxVq5AiA1OH+ftAzvWdeKtbB0ekAUalcCQFHsiYh1VEj9SB8qERScx8CNNcnu9r1IaqyA4j2PNv4p6Pl8QDo2DddOt9FwNWJ2AK+4fQqLn2nd38i6VD2Sbvo=";
    private static String AES_KEY = "65674EC3EBCFA7372A933F737BC7E669";

    public static void main(String[] args) {
        //设置基础参数
        Integer pid_1 = 2000;
        String channelId_1 = "bairong";
        JSONObject data_1 = JSONObject.parseObject("{\"id\":100,\"value\":\"哈哈哈\"}");
        Long timestamp_1 = System.currentTimeMillis();
        //aes加密
        String dataAES_1 = encryptAES(AES_KEY, data_1.toString());
        //设置签名参数
        Map<String, String> signParam_1 = new HashMap<>();
        signParam_1.put("channelId", channelId_1);
        signParam_1.put("data", dataAES_1);
        signParam_1.put("pid", pid_1.toString());
        signParam_1.put("timestamp", timestamp_1.toString());
        //生成签名
        String sign = Encrypt.signSHA1(signParam_1, PRIVATE_KEY);
        JSONObject requestParam = new JSONObject();
        requestParam.put("channelId", channelId_1);
        requestParam.put("pid", pid_1);
        requestParam.put("data", dataAES_1);
        requestParam.put("sign", sign);
        requestParam.put("timestamp", timestamp_1);
        //最终的请求参数
        System.out.println(requestParam.toJSONString());

        //解密参数
        Integer pid_2 = requestParam.getInteger("pid");
        String channelId_2 = requestParam.getString("channelId");
        String dataAES_2 = requestParam.getString("data");
        Long timestamp_2 = requestParam.getLong("timestamp");
        //检查签名
        Map<String, String> signParam_2 = new HashMap<>();
        signParam_2.put("pid", pid_2.toString());
        signParam_2.put("channelId", channelId_2.toString());
        signParam_2.put("timestamp", timestamp_2.toString());
        signParam_2.put("data", dataAES_2);
        boolean flag = Encrypt.checkSignSHA1(signParam_2, requestParam.getString("sign"), PUBLIC_KEY);
        if (!flag) {
            System.out.println("签名错误");
            return;
        }
        System.out.println("签名正确");
        //aes解密
        JSONObject data_2 = JSONObject.parseObject(decryptAES(AES_KEY, dataAES_2));
        System.out.println("解密后的data:" + data_2.toString());

    }


    /**
     * ras加签、验签
     */
    static class Encrypt {
        /**
         * 加签
         *
         * @param signParams
         * @param privateKey
         * @return
         */
        public static String signSHA1(Map<String, String> signParams, String privateKey) {
            return initSign("SHA1withRSA", signParams, privateKey);
        }

        /**
         * 验签
         *
         * @param signParams
         * @param sign
         * @param publicKey
         * @return
         */
        public static boolean checkSignSHA1(Map<String, String> signParams, String sign, String publicKey) {
            return rsa256CheckContent("SHA1withRSA", signParams, sign, publicKey);
        }

        private static boolean rsa256CheckContent(String signType, Map<String, String> signParams, String sign, String publicKey) {
            try {
                String content = getSignContent(signParams);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                byte[] encodedKey = Base64.decode(publicKey);
                PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
                Signature signature = Signature.getInstance(signType);
                signature.initVerify(pubKey);
                signature.update(content.getBytes("utf-8"));
                boolean bverify = signature.verify(Base64.decode(sign));
                return bverify;
            } catch (Exception var10) {
                var10.printStackTrace();
                return false;
            }
        }

        private static String initSign(String signType, Map<String, String> signParams, String privateKey) {
            try {
                String content = getSignContent(signParams);
                PrivateKey priKey = getPrivateKeyFromPKCS8(privateKey);
                Signature signature = Signature.getInstance(signType);
                signature.initSign(priKey);
                signature.update(content.getBytes("utf-8"));
                byte[] signed = signature.sign();
                return new String(Base64.encode(signed));
            } catch (Exception var7) {
                System.out.println("生成签名异常:" + var7.getMessage());
                var7.printStackTrace();
                return null;
            }
        }

        public static String getSignContent(Map<String, String> sortedParams) {
            StringBuffer content = new StringBuffer();
            List<String> keys = new ArrayList(sortedParams.keySet());
            //对key进行排序
            Collections.sort(keys);
            int index = 0;
            for (int i = 0; i < keys.size(); ++i) {
                String key = keys.get(i);
                String value = sortedParams.get(key);
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank((value))) {
                    content.append((index == 0 ? "" : "&") + key + "=" + value);
                    ++index;
                }
            }

            return content.toString();
        }

        private static PrivateKey getPrivateKeyFromPKCS8(String priKey) {
            PrivateKey privateKey = null;
            if (StringUtils.isBlank(priKey)) {
                return privateKey;
            } else {
                try {
                    PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec((new BASE64Decoder()).decodeBuffer(priKey));
                    KeyFactory keyf = KeyFactory.getInstance("RSA");
                    privateKey = keyf.generatePrivate(priPKCS8);
                } catch (Exception var4) {
                    System.out.println("私钥解析错误:" + var4.getMessage());
                }

                return privateKey;
            }
        }
    }

    /**
     * aes加密
     *
     * @param base64Key
     * @param text
     * @return
     */
    public static String encryptAES(String base64Key, String text) {
        try {
            byte[] key = parseHexStr2Byte(base64Key);
            SecretKeySpec sKeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(1, sKeySpec);
            byte[] encryptBytes = cipher.doFinal(text.getBytes("utf-8"));
            return parseByte2HexStr(encryptBytes);
        } catch (Exception var6) {
            System.out.println("AES加密错误:" + var6.getMessage());
            return null;
        }
    }

    /**
     * aes解密
     *
     * @param base64Key
     * @param text
     * @return
     */
    public static String decryptAES(String base64Key, String text) {
        try {
            byte[] key = parseHexStr2Byte(base64Key);
            SecretKeySpec sKeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(2, sKeySpec);
            byte[] decryptBytes = cipher.doFinal(parseHexStr2Byte(text));
            return new String(decryptBytes, "UTF-8");
        } catch (Exception var6) {
            System.out.println("AES解密错误:" + var6.getMessage());
            return null;
        }
    }

    private static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1) {
            return null;
        } else {
            byte[] result = new byte[hexStr.length() / 2];

            for (int i = 0; i < hexStr.length() / 2; ++i) {
                int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
                int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
                result[i] = (byte) (high * 16 + low);
            }

            return result;
        }
    }

    public static String parseByte2HexStr(byte[] buf) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < buf.length; ++i) {
            String hex = Integer.toHexString(buf[i] & 255);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }

            sb.append(hex.toUpperCase());
        }

        return sb.toString();
    }
}
