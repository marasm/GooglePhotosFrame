/**
 * 
 */
package com.marasm.gpf.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.Util;

import com.marasm.util.StringUtil;

/**
 * @author makorotkovas
 *
 */
public class AppLogger
{
  private static String loggerName;
  
  
  public static void initLogger(String inLoggerName)
  {
    loggerName = inLoggerName;
  }
  
  
  private static Logger getLogger()
  {
    if (StringUtil.isEmpty(loggerName))
    {
      return LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }
    else
    {
      return LoggerFactory.getLogger(loggerName);
    }
  }
  
  
  public static void log(LogLevel inLogLevel, String inMsg)
  {
    Logger logger = getLogger(); 
    Class<?> clazz = Util.getCallingClass();
    String logStr = "[" + clazz.getSimpleName() + "] - " + inMsg;
    
    switch (inLogLevel)
    {
      case DEBUG:
        logger.debug(logStr);
        break;
      
      case INFO:
        logger.info(logStr);
        break;
      
      case WARNING:
        logger.warn(logStr);
        break;

      case ERROR:
        logger.error(logStr);
        break;
      
      default:
        logger.debug(logStr);
        break;
    }
  }

  public static void log( LogLevel inLogLevel, String inMsg, Object... inArgs)
  {
    Logger logger = getLogger();
    Class<?> clazz = Util.getCallingClass();
    String logStr = "[" + clazz.getSimpleName() + "] - " + inMsg;
    switch (inLogLevel)
    {
      case DEBUG:
        logger.debug(logStr, inArgs);
        break;
        
      case INFO:
        logger.info(logStr, inArgs);
        break;
        
      case WARNING:
        logger.warn(logStr, inArgs);
        break;
        
      case ERROR:
        logger.error(logStr, inArgs);
        break;
        
      default:
        logger.debug(logStr, inArgs);
        break;
    }
  }
  public static void log(LogLevel inLogLevel, String inMsg, Throwable inException)
  {
    Logger logger = getLogger();
    Class<?> clazz = Util.getCallingClass();
    String logStr = "[" + clazz.getSimpleName() + "] - " + inMsg;
    switch (inLogLevel)
    {
      case DEBUG:
        logger.debug(logStr, inException);
        break;
        
      case INFO:
        logger.info(logStr, inException);
        break;
        
      case WARNING:
        logger.warn(logStr, inException);
        break;
        
      case ERROR:
        logger.error(logStr, inException);
        break;
        
      default:
        logger.debug(logStr, inException);
        break;
    }
  }

  
  
}
