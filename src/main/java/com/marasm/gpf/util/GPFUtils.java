/**
 * 
 */
package com.marasm.gpf.util;

import java.awt.Dimension;

import com.google.gdata.data.media.mediarss.MediaContent;

/**
 * @author mkorotkovas
 *
 */
public class GPFUtils
{
  public static String getSizeSpecificUrlForImage(MediaContent inMediaContent, Dimension inScreenSize)
  {
    StringBuilder res = new StringBuilder(
        inMediaContent.getUrl().substring(0, inMediaContent.getUrl().lastIndexOf("/")));
    
    res.append("/s" + getAppropriateImageSize(inMediaContent, inScreenSize));
    res.append(inMediaContent.getUrl().substring(inMediaContent.getUrl().lastIndexOf("/")));
    
    System.out.println("Size Specific URL: " + res.toString());
    return res.toString();
  }

  public static int getAppropriateImageSize(MediaContent inMediaContent, Dimension inScreenSize)
  {
    boolean isPhotoLandscape = inMediaContent.getWidth() > inMediaContent.getHeight();
    return (int)(isPhotoLandscape ? inScreenSize.getWidth() : inScreenSize.getHeight());
  }
}
