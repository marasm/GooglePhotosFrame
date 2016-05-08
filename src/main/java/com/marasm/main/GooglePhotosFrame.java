/**
 * 
 */
package com.marasm.main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Panel;
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

import javax.imageio.ImageIO;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.marasm.services.DeviceAuthService;
import com.marasm.services.ImageQueue;
import com.marasm.util.AppProperties;
import com.marasm.valueobjects.DeviceCodeResponseVO;
import com.marasm.valueobjects.PhotoDisplayVO;

/**
 * @author mkorotkovas
 *
 */
public class GooglePhotosFrame
{
  
  public static void main(String[] args)
  {
    try
    {
      String version = AppProperties.getProperty(AppProperties.APP_VERSION_PROP);
      int slideShowDelaySeconds = Integer.valueOf(AppProperties.getProperty(
        AppProperties.SLIDE_SHOW_DELAY_PROP)).intValue();
      boolean useProxy = Boolean.valueOf(AppProperties.getProperty(AppProperties.USE_PROXY_PROP));
      System.out.println("Starting Google Photos Frame v" + version);
      System.out.println("Slideshow Delay (s): " + slideShowDelaySeconds);
      
      if (useProxy)
      {
        System.setProperty("http.proxyHost",  AppProperties.getProperty(AppProperties.HTTP_PROXY_HOST_PROP));
        System.setProperty("http.proxyPort",  AppProperties.getProperty(AppProperties.HTTP_PROXY_PORT_PROP));
        System.setProperty("https.proxyHost", AppProperties.getProperty(AppProperties.HTTPS_PROXY_HOST_PROP));
        System.setProperty("https.proxyPort", AppProperties.getProperty(AppProperties.HTTPS_PROXY_PORT_PROP));
      }
      
      
      //========================Authenticate with Google APIs================================//
      DeviceAuthService service = new DeviceAuthService();
      if (service.hasStoredCredentials())
      {
        System.out.println("Found stored credentials");
      }
      else
      {
        System.out.println("Stored credentials NOT found. Will request new.");
        DeviceCodeResponseVO deviceCodeVO = service.getDeviceAuthCode();
        
        System.out.println("Url=" + deviceCodeVO.getVerificationUrl());
        System.out.println("Code=" + deviceCodeVO.getUserCode());
        
        long startTime = System.currentTimeMillis();
        TokenResponse token = null;
        while(System.currentTimeMillis() <= startTime + (1000 * deviceCodeVO.getExpiresIn()))
        {
          System.out.println("Waiting for Google authorization");
          Thread.sleep(deviceCodeVO.getInterval() * 1000);
          
          token = service.getAccessToken(deviceCodeVO);
          if (token != null) break;
        }
        if (token == null)
        {
          System.out.println("Timed out wating for device authorization");
        }
        else
        {
          System.out.println("Success! access token is: " + token.getAccessToken());
        }
        service.storeAccessTokenData(token);
      }
      
      
      //=======================Setup display============================//
      
      Dimension screenSize =  Toolkit.getDefaultToolkit().getScreenSize();
      
      Frame mainFrame = new Frame("Demo");
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
      
      ImagePanel imagePanel = new GooglePhotosFrame().new ImagePanel();
//      imagePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
      imagePanel.setLayout(null);
      imagePanel.setSize(mainFrame.getSize());
      mainFrame.add(imagePanel);
      mainFrame.setVisible(true);
      
      
      ImageQueue imgQueue = new ImageQueue(screenSize);
      Thread imgQueueThread = new Thread(imgQueue);
      imgQueueThread.start();
      Thread.sleep(5000);//let the first image load into the queue
      
      while(true)
      {
        PhotoDisplayVO photo = imgQueue.getNextPhotoEntry();
        if (photo != null)
        {
          System.out.println("Showing Image: " + photo.getUrl());
          imagePanel.setImage(photo); 
        }
        else
        {
          System.out.println("Image queue is empty. Waiting for it to be populated.");
        }
        if (imgQueue.getCancelled())
        {
          throw new Exception("Image task was cancelled. Quitting.");
        }
        Thread.sleep(slideShowDelaySeconds * 1000);
      }
      
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(1);
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
      textLbl.setBounds(0, (int)(screenSize.getHeight()-100), (int)screenSize.getWidth(), 25);
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
