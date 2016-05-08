/**
 * 
 */
package com.marasm.gpf.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author mkorotkovas
 *
 */
public class SecretProperties
{
  public static final String CLIENT_ID = "client.id";
  public static final String CLIENT_SECRET = "client.secret";
  
  private static Properties secretProperties = new Properties();
  

  public static String getProperty(String inPropName) throws IOException
  {
    checkPopulateProps();
    return secretProperties.getProperty(inPropName);
  }
  
  private static void checkPopulateProps() throws IOException
  {
    if (secretProperties.isEmpty())
    {
      secretProperties.load(new FileInputStream(
        System.getProperty("user.home") + "/.googlePhotosFrame/secret.properties"));
    }
  }
  
  
}
