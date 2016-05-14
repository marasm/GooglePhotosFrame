/**
 * 
 */
package com.marasm.gpf.main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.MissingResourceException;

import javax.imageio.ImageIO;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.marasm.gpf.services.DeviceAuthService;
import com.marasm.gpf.services.ImageQueue;
import com.marasm.gpf.util.AppProperties;
import com.marasm.gpf.valueobjects.DeviceCodeResponseVO;
import com.marasm.gpf.valueobjects.PhotoDisplayVO;
import com.marasm.logger.AppLogger;
import com.marasm.logger.LogLevel;
import com.marasm.util.StringUtil;

/**
 * @author mkorotkovas
 *
 */
public class GooglePhotosFrame
{
  private static final int MAX_CONSEQUITIVE_ERRORS_TO_IGNORE = 5;
  private static boolean isScreenOn = true;
  
  public static void main(String[] args)
  {
    try
    {
      AppLogger.initLogger("appLogger");
      
      String version = AppProperties.getProperty(AppProperties.APP_VERSION_PROP);
      int slideShowDelaySeconds = Integer.valueOf(AppProperties.getProperty(
        AppProperties.SLIDE_SHOW_DELAY_PROP)).intValue();
      boolean useProxy = Boolean.valueOf(AppProperties.getProperty(AppProperties.USE_PROXY_PROP));
      AppLogger.log(LogLevel.DEBUG,"Starting Google Photos Frame v" + version);
      AppLogger.log(LogLevel.DEBUG,"Slideshow Delay (s): " + slideShowDelaySeconds);
      
      if (useProxy)
      {
        AppLogger.log(LogLevel.DEBUG, "Setting proxy parameters");
        System.setProperty("http.proxyHost",  AppProperties.getProperty(AppProperties.HTTP_PROXY_HOST_PROP));
        System.setProperty("http.proxyPort",  AppProperties.getProperty(AppProperties.HTTP_PROXY_PORT_PROP));
        System.setProperty("https.proxyHost", AppProperties.getProperty(AppProperties.HTTPS_PROXY_HOST_PROP));
        System.setProperty("https.proxyPort", AppProperties.getProperty(AppProperties.HTTPS_PROXY_PORT_PROP));
      }
      
      
      //=======================Setup display============================//
      
      Dimension screenSize =  Toolkit.getDefaultToolkit().getScreenSize();
      
      Frame mainFrame = new Frame("GooglePhotosFrame");
      mainFrame.setSize(screenSize);
      mainFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
      mainFrame.setUndecorated(true);
      mainFrame.setBackground(Color.BLACK);
      mainFrame.addWindowListener(new WindowAdapter()
      {
        @Override
        public void windowClosing(WindowEvent windowEvent){
           System.exit(0);
        } 
      });
      mainFrame.addKeyListener(new KeyAdapter()
      {
        @Override
        public void keyPressed(KeyEvent inE)
        {
          super.keyPressed(inE);
          if (inE.getKeyCode() == KeyEvent.VK_ESCAPE)
          {
            System.exit(0);
          }
        }
      });
      mainFrame.setVisible(true);
      
      
      //========================Authenticate with Google APIs================================//
      DeviceAuthService service = new DeviceAuthService();
      if (service.hasStoredCredentials())
      {
        AppLogger.log(LogLevel.DEBUG, "Found stored credentials");
      }
      else
      {
        AppLogger.log(LogLevel.WARNING, "Stored credentials NOT found. Will request new.");
        DeviceCodeResponseVO deviceCodeVO = service.getDeviceAuthCode();
        
        TextArea textArea = new TextArea();
        textArea.setText(
              "This device has not been authorized to access Google Photos yet\n"
            + "-----------------------------------------------------------\n"
            + "Go to: " + deviceCodeVO.getVerificationUrl() + " on you computer or tablet.\n"
            + "Enter the following code: " + deviceCodeVO.getUserCode());
        textArea.setForeground(Color.WHITE);
        textArea.setBackground(Color.BLACK);
        textArea.setFocusable(false);
        textArea.setEditable(false);
        textArea.setBounds((int)(screenSize.getWidth()/2)-250, (int)(screenSize.getHeight()/2)-50, 500, 100);
        mainFrame.add(textArea);
        
        Label waitingLbl = new Label();
        waitingLbl.setText("Waiting...");
        waitingLbl.setForeground(Color.WHITE);
        waitingLbl.setBounds((int)(screenSize.getWidth()/2)-40, (int)(screenSize.getHeight()/2)+50, 80, 25);
        mainFrame.add(waitingLbl);
        
        AppLogger.log(LogLevel.INFO, "Url={}", deviceCodeVO.getVerificationUrl());
        AppLogger.log(LogLevel.INFO, "Code={}", deviceCodeVO.getUserCode());
        
        long startTime = System.currentTimeMillis();
        TokenResponse token = null;
        while(System.currentTimeMillis() <= startTime + (1000 * deviceCodeVO.getExpiresIn()))
        {
          AppLogger.log(LogLevel.DEBUG, "Waiting for Google authorization");
          Thread.sleep(deviceCodeVO.getInterval() * 1000);
          
          token = service.getAccessToken(deviceCodeVO);
          if (token != null) break;
        }
        if (token == null)
        {
          throw new Exception("Timed out wating for device authorization");
        }
        AppLogger.log(LogLevel.INFO, "Success! access token is: {}", token.getAccessToken());
        service.storeAccessTokenData(token);
        mainFrame.remove(textArea);
        mainFrame.remove(waitingLbl);
      }
      
      
      
      
      ImagePanel imagePanel = new GooglePhotosFrame().new ImagePanel();
      imagePanel.setLayout(null);
      imagePanel.setSize(mainFrame.getSize());
      mainFrame.add(imagePanel);
      
      
      int errorCounter = 0;
      ImageQueue imgQueue = new ImageQueue(screenSize);
      Thread imgQueueThread = new Thread(imgQueue);
      imgQueueThread.start();
      Thread.sleep(10000);//let the first image load into the queue
      
      while(true)
      {
        try
        {
          PhotoDisplayVO photo = imgQueue.getNextPhotoEntry();
          if (photo != null)
          {
            AppLogger.log(LogLevel.DEBUG, "Showing Image: " + photo.getUrl());
            imagePanel.setImage(photo); 
            checkAndSetAppropriateDisplayMode();
          }
          else
          {
            AppLogger.log(LogLevel.DEBUG, "Image queue is empty. Waiting for it to be populated.");
          }
        }
        catch (Exception e)
        {
          AppLogger.log(LogLevel.WARNING, "Error displaying image: ", e);
          if (errorCounter >= MAX_CONSEQUITIVE_ERRORS_TO_IGNORE)
          {
            throw new Exception("Max number of errors in main in a row exceeded. Quiting.");
          }
          errorCounter++;
          continue;
        }
        
        if (imgQueue.getCancelled())
        {
          throw new Exception("Image task was cancelled. Quitting.");
        }
        errorCounter = 0; //reset count if all tasks in a loop suceeded
        Thread.sleep(slideShowDelaySeconds * 1000);
      }
      
    }
    catch (Exception e)
    {
      AppLogger.log(LogLevel.ERROR, "Error in main: " + e.getMessage(), e); 
      System.exit(1);
    }
    AppLogger.log(LogLevel.ERROR, "Reached end of main(). Normally this should not happen.");
    System.exit(1);
  }
  
  private static void checkAndSetAppropriateDisplayMode()
  {
    // TODO check the current time and either wake up or put display to sleep according to settings
    Calendar cal = Calendar.getInstance();
    boolean isWeekEnd = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
      cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
    try
    {
      String weekDayOffTimeStr = AppProperties.getProperty(AppProperties.SCREEN_OFF_TIME_WEEKDAY_PROP); 
      String weekDayOnTimeStr = AppProperties.getProperty(AppProperties.SCREEN_ON_TIME_WEEKDAY_PROP); 
      String weekEndOffTimeStr = AppProperties.getProperty(AppProperties.SCREEN_OFF_TIME_WEEKEND_PROP); 
      String weekEndOnTimeStr = AppProperties.getProperty(AppProperties.SCREEN_ON_TIME_WEEKEND_PROP);
      
      if (StringUtil.isEmpty(weekDayOnTimeStr) ||
          StringUtil.isEmpty(weekDayOffTimeStr) ||
          StringUtil.isEmpty(weekEndOnTimeStr) ||
          StringUtil.isEmpty(weekEndOffTimeStr))
      {
        throw new MissingResourceException("One or more screen control parameters are missing", 
          AppProperties.class.getName(), "screen.*.time.*");
      }
      Date weekDayOnTime = createDateBasedOnTimeString(weekDayOnTimeStr);
      Date weekDayOffTime = createDateBasedOnTimeString(weekDayOffTimeStr);
      Date weekEndOnTime = createDateBasedOnTimeString(weekEndOnTimeStr);
      Date weekEndOffTime = createDateBasedOnTimeString(weekEndOffTimeStr);
      Date curTime = cal.getTime();
      
      
      if (isWeekEnd)
      {
        if (curTime.after(weekEndOnTime) && curTime.before(weekEndOffTime))
        {
          screenOn();
        }
        else
        {
          screenOff();
        }
      }
      else
      {
        if (curTime.after(weekDayOnTime) && curTime.before(weekDayOffTime))
        {
          screenOn();
        }
        else
        {
          screenOff();
        }
      }
    }
    catch(Exception e)
    {
      AppLogger.log(LogLevel.ERROR, "Error while checking and setting screen mode: ", e);
    }
  }

  private static Date createDateBasedOnTimeString(String inTimeStr)
  {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(inTimeStr.split(":")[0]));
    cal.set(Calendar.MINUTE, Integer.valueOf(inTimeStr.split(":")[1]));
    cal.set(Calendar.SECOND, 0);
    return cal.getTime();
  }
  
  private static void screenOn() throws IOException
  {
    if (!isScreenOn)
    {
      String curDir = System.getProperty("user.dir");
      AppLogger.log(LogLevel.INFO, "Turning screen ON");
      new ProcessBuilder(curDir + "/screen.sh", "on").start();
      isScreenOn = true;
    }
  }

  private static void screenOff() throws IOException
  {
    if (isScreenOn)
    {
      String curDir = System.getProperty("user.dir");
      AppLogger.log(LogLevel.INFO, "Turning screen OFF");
      new ProcessBuilder(curDir + "/screen.sh", "off").start();
      isScreenOn = false;
    }
  }

  public class ImagePanel extends Panel
  {
    private static final long serialVersionUID = 1L;
    private final SimpleDateFormat SDF = new SimpleDateFormat("MM/dd/yyyy");
    private BufferedImage image;
    private Label textLbl;
    
    public ImagePanel()
    {
      Dimension screenSize =  Toolkit.getDefaultToolkit().getScreenSize();
      textLbl = new Label();
      textLbl.setText("Loading pictures...");
      textLbl.setForeground(Color.WHITE);
      textLbl.setBounds(0, (int)(screenSize.getHeight()-25), (int)screenSize.getWidth(), 25);
      add(textLbl);
    }
    
    @Override
    public void paint(Graphics inG)
    {
      super.paint(inG);
      Dimension screenSize =  Toolkit.getDefaultToolkit().getScreenSize();
      if (image != null)
      {
        int xOffset = 0;
        int yOffset = 0;
        
        if (image.getWidth() < screenSize.getWidth())
        {
          xOffset = (int)((screenSize.getWidth()-image.getWidth())/2);
        }
        if (image.getHeight() < screenSize.getHeight())
        {
          yOffset = (int)((screenSize.getHeight()-image.getHeight())/2);
        }
        
        inG.drawImage(image, 0 + xOffset, 0 + yOffset, this);
      }
      else
      {
        textLbl.setText("Loading pictures...");
      }
    }
    
    public void setImage(PhotoDisplayVO inImage) throws MalformedURLException, IOException
    {
      image = ImageIO.read(new URL(inImage.getUrl()));
      textLbl.setText(inImage.getAlbumName() +  
        (inImage.getDateTaken() != null ? " - (" + SDF.format(inImage.getDateTaken()) + ")" : ""));
      repaint();
    }
  }

}
