package authentication;

import org.apache.commons.lang3.RandomStringUtils;

public class AuthUtil {
	
	public static String generateSalt() {
		return RandomStringUtils.randomAlphanumeric(128);
	}
	
	public static String byteToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b:bytes) {
			sb.append(Character.forDigit((b >> 4) & 0xF, 16));
			sb.append(Character.forDigit(b & 0xF, 16));
		}
		return sb.toString();

	}
}
