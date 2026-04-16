package org.example.common;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;

public class AESHelper {

	private static final String sSelfKey = "t^7M5g_k2cj@P6gr";

	private static AES getAes(String key) {
		if (key == null || key.length() != 16) {
			throw new RuntimeException("密钥长度需为16位");
		}
		// 构建
		return new AES(Mode.ECB, Padding.PKCS5Padding, key.getBytes(StandardCharsets.UTF_8));
	}

	// 加密
	public static String Encrypt(String sSrc) throws Exception {

		return getAes(sSelfKey).encryptHex(sSrc, StandardCharsets.UTF_8);
	}

	// 解密
	public static String Decrypt(String sSrc) throws Exception {

		return getAes(sSelfKey).decryptStr(sSrc, StandardCharsets.UTF_8);

	}

	// 加密
	public static String EncryptByKey(String sKey, String sSrc) throws Exception {

		if (sKey == null || sKey.length() != 16) {

			throw new Exception("sKey为空或不是16位");
		}

		return getAes(sKey).encryptHex(sSrc, StandardCharsets.UTF_8);
	}

	// 解密
	public static String DecryptByKey(String sKey, String sSrc) throws Exception {

		// 判断Key是否正确
		if (sKey == null || sKey.length() != 16) {

			throw new Exception("sKey为空或不是16位");
		}

		return getAes(sKey).decryptStr(sSrc, StandardCharsets.UTF_8);

	}

	public static void main(String[] args) {
		try {
			String sSrc = "626300e23e6e6fbaa6ccabdede766c35f7a56d5dca9eee730c420c2e44910fc230fc6c393466dfe323fc56b1f7e32733e5fb02c75796cb3c7e1c707646714e28d435b325b99850b884392320ebb89ba69d63bc0a979de80656cb69d1bd746efc2d9be8b116b6bcd53ecceddb2af857839df14fb05d3773851347cf7d5b0f7491f62b09f73ce4e0ddd059d19e5f1e8ae0e0c5f6fc01fac47563f854c3b9b73df6ceddadd4d934b51329a7fadc97d5d77ec4e2e82ab1862a5758d43ed4825ab5fd4dad0ef106c8c3d39e4837cfddf8daee";
			System.out.println(AESHelper.DecryptByKey("FCiA8fNy6zjH4cHt",sSrc));
		}catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
