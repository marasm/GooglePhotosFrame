/**
 * 
 */
package com.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

/**
 * @author MAKorotkovas
 *
 */
public class TestImage extends Panel
{
  private static final long serialVersionUID = 1L;

  private int x = 0;
  private int y = 0;

  private BufferedImage img;
  
  
  public TestImage(BufferedImage inImg)
  {
    img = inImg;
  }
  
  
  public static void main(String[] args) throws Exception
  {
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
    
    TestImage imagePanel = new TestImage(ImageIO.read(new File("/Users/mkorotkovas/Downloads/cat.jpg")));
    imagePanel.setSize(mainFrame.getSize());
    imagePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    mainFrame.add(imagePanel);
    
    mainFrame.setVisible(true);
    imagePanel.moveIn(MoveDirection.LEFT_TO_RIGHT);
  }
  
  public void moveIn(MoveDirection inDirection) throws InterruptedException
  {
    for (int xPos = (0-img.getWidth()); xPos <= 0; xPos = xPos + 4)
    {
      System.out.println("current xPos = " + xPos);
      x = xPos;
      repaint();
      Thread.sleep(10);
    }
  }

  
  @Override
  public void paint(Graphics inG)
  {
    super.paint(inG);
    inG.drawImage(img, x, y, this);
  }
  
  public enum MoveDirection
  {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP;
  }
  
    
}