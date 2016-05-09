/**
 * 
 */
package com.marasm.gpf.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marasm.util.StringUtil;

/**
 * @author makorotkovas
 *
 */
public class GPFLogger
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
      return LoggerFactory.getLogger("rootLogger");
    }
    else
    {
      return LoggerFactory.getLogger(loggerName);
    }
  }
  
  
  public static void log(LogLevel inLogLevel, String inMsg)
  {
    Logger logger = getLogger();
    switch (inLogLevel)
    {
      case DEBUG:
        logger.debug(inMsg);
        break;
      
      case INFO:
        logger.info(inMsg);
        break;
      
      case WARNING:
        logger.warn(inMsg);
        break;

      case ERROR:
        logger.error(inMsg);
        break;
      
      default:
        logger.debug(inMsg);
        break;
    }
  }

  public static void log( LogLevel inLogLevel, String inMsg, Object... inArgs)
  {
    Logger logger = getLogger();
    switch (inLogLevel)
    {
      case DEBUG:
        logger.debug(inMsg, inArgs);
        break;
        
      case INFO:
        logger.info(inMsg, inArgs);
        break;
        
      case WARNING:
        logger.warn(inMsg, inArgs);
        break;
        
      case ERROR:
        logger.error(inMsg, inArgs);
        break;
        
      default:
        logger.debug(inMsg, inArgs);
        break;
    }
  }
  public static void log(LogLevel inLogLevel, String inMsg, Throwable inException)
  {
    Logger logger = getLogger();
    switch (inLogLevel)
    {
      case DEBUG:
        logger.debug(inMsg, inException);
        break;
        
      case INFO:
        logger.info(inMsg, inException);
        break;
        
      case WARNING:
        logger.warn(inMsg, inException);
        break;
        
      case ERROR:
        logger.error(inMsg, inException);
        break;
        
      default:
        logger.debug(inMsg, inException);
        break;
    }
  }

  
  public static void log(LogLevel inLogLevel, String inMsg, Throwable inException, Object... inArgs)
  {
    Logger logger = getLogger();
    switch (inLogLevel)
    {
      case DEBUG:
        logger.debug(inMsg, inException, inArgs);
        break;
        
      case INFO:
        logger.info(inMsg, inException, inArgs);
        break;
        
      case WARNING:
        logger.warn(inMsg, inException, inArgs);
        break;
        
      case ERROR:
        logger.error(inMsg, inException, inArgs);
        break;
        
      default:
        logger.debug(inMsg, inException, inArgs);
        break;
    }
  }
}
