package authentication;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


public class PBKDF2 {
  private static final int ITERATION = 10000; //
  private static final int KEYLENGTH = 512;
  

  
  public static String hashPassword( final String  password, final String salt) {

	  char[] passwordCharArray = password.toCharArray();
	  byte[] saltBytes = salt.getBytes();
      try {
          SecretKeyFactory skf = SecretKeyFactory.getInstance( "PBKDF2WithHmacSHA512" );
          PBEKeySpec spec = new PBEKeySpec( passwordCharArray, saltBytes,
        		                            ITERATION, KEYLENGTH );
          SecretKey key = skf.generateSecret( spec );
          byte[] res = key.getEncoded( );
          return AuthUtil.byteToHexString(res);
          
      } catch ( NoSuchAlgorithmException | InvalidKeySpecException e ) {
          e.printStackTrace();
          return null;
      }
  }
  

}
