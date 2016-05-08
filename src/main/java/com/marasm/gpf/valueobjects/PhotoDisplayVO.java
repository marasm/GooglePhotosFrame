/**
 * 
 */
package com.marasm.gpf.valueobjects;

import java.util.Date;

/**
 * @author mkorotkovas
 *
 */
public class PhotoDisplayVO
{
  private String url;
  private String albumName;
  private Date dateTaken;
  
  public PhotoDisplayVO(String inUrl, String inAlbumName, Date inDateTaken)
  {
    url = inUrl;
    albumName = inAlbumName;
    dateTaken = inDateTaken;
  }

  public String getUrl()
  {
    return url;
  }

  public void setUrl(String inUrl)
  {
    url = inUrl;
  }

  public String getAlbumName()
  {
    return albumName;
  }

  public void setAlbumName(String inAlbumName)
  {
    albumName = inAlbumName;
  }

  public Date getDateTaken()
  {
    return dateTaken;
  }

  public void setDateTaken(Date inDateTaken)
  {
    dateTaken = inDateTaken;
  }
}
