/**
 * 
 */
package com.marasm.gpf.util;

import java.io.IOException;
import java.util.Properties;

import com.marasm.gpf.main.GooglePhotosFrame;

/**
 * @author mkorotkovas
 *
 */
public class AppProperties
{
  public static final String APP_VERSION_PROP = "app.version";
  public static final String SLIDE_SHOW_DELAY_PROP = "slideshow.delay";
  public static final String USE_PROXY_PROP = "use.http.proxy";
  public static final String HTTP_PROXY_HOST_PROP = "http.proxy.host";
  public static final String HTTP_PROXY_PORT_PROP = "http.proxy.port";
  public static final String HTTPS_PROXY_HOST_PROP = "https.proxy.host";
  public static final String HTTPS_PROXY_PORT_PROP = "https.proxy.port";
  public static final String SCREEN_ON_TIME_WEEKDAY_PROP = "screen.off.time.weekday";
  public static final String SCREEN_OFF_TIME_WEEKDAY_PROP = "screen.on.time.weekday";
  public static final String SCREEN_ON_TIME_WEEKEND_PROP = "screen.off.time.weekend";
  public static final String SCREEN_OFF_TIME_WEEKEND_PROP = "screen.on.time.weekend";
  
  private static Properties appProperties = new Properties();
  
  public static String getProperty(String inPropName) throws IOException
  {
    checkPopulateProps();
    return appProperties.getProperty(inPropName);
  }
  
  private static void checkPopulateProps() throws IOException
  {
    if (appProperties.isEmpty())
    {
      appProperties.load(GooglePhotosFrame.class.getResourceAsStream("/app.properties"));
    }
  }
  
  
}
