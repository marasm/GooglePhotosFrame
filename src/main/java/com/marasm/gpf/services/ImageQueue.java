package com.marasm.gpf.services;

import java.awt.Dimension;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.util.ServiceException;
import com.google.gdata.util.ServiceForbiddenException;
import com.marasm.gpf.util.GPFUtils;
import com.marasm.gpf.valueobjects.PhotoDisplayVO;

public class ImageQueue implements Runnable
{
  private static final int MAX_QUEUE_SIZE = 20;
  
  private List<AlbumEntry> albums = new ArrayList<AlbumEntry>();
  
  private Queue<PhotoDisplayVO> imageQueue = new ConcurrentLinkedQueue<PhotoDisplayVO>();
  
  private boolean cancelled;
  
  private Dimension screenSize;
  
  public ImageQueue(Dimension inScreenSize)
  {
    screenSize = inScreenSize;
  }
  
  @Override
  public void run()
  {
    System.out.println("Starting image queue worker");
    try
    {
      DeviceAuthService authService = new DeviceAuthService();
      PhotosService photosService = new PhotosService(authService);
      
      while (!cancelled)
      {
        if (imageQueue.size() < MAX_QUEUE_SIZE)
        {
          try
          {
            if (albums.isEmpty())
            {
              System.out.println("Album list empty. Getting a fresh list.");
              albums = photosService.getAllUserAlbums();
              if (albums == null || albums.isEmpty())
              {
                throw new IOException("Unable to get the albums");
              }
            }
            // pick a random album and a random pic from it
            AlbumEntry randomAlbum = getRandomAlbum(albums);
            System.out.println("Picked album: " + randomAlbum.getTitle().getPlainText());
            List<PhotoEntry> albumPhotos = photosService.getAlbumPhotos(randomAlbum.getGphotoId());
            if (albumPhotos.isEmpty())
            {
              System.out.println("Album " + randomAlbum.getTitle().getPlainText() + " is empty. Will pick another");
              continue;
            }
            PhotoEntry randomPhoto = getRandomPhoto(albumPhotos);
            if (randomPhoto.getMediaContents() == null || randomPhoto.getMediaContents().isEmpty())
            {
              System.out.println("Photo does not have media contents. Skipping");
              continue;
            }
            
            PhotoDisplayVO photoDisplayVO =
              new PhotoDisplayVO(GPFUtils.getSizeSpecificUrlForImage(randomPhoto.getMediaContents().get(0), screenSize),
                randomAlbum.getTitle().getPlainText(),
                randomPhoto.getExifTags() != null ? randomPhoto.getExifTags().getTime() : null);
            System.out.println("Picked photo: " + randomPhoto.getId());
            imageQueue.add(photoDisplayVO);
          }
          catch (ServiceException e)
          {
            if (e instanceof ServiceForbiddenException)// token expired
            {
              System.out.println("Access token expired. Refreshing token.");
              authService.refreshAndStoreAccessToken();
            }
            else
            {
              e.printStackTrace();
              throw e;
            }
          }
        }
        else
        {
          System.out.println("Image queue is full. Wait...");
          try
          {
            Thread.sleep(120000);
          }
          catch (InterruptedException e)
          {
            break;
          }
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.out.println("Error" + e.getMessage());
      cancelled = true;
    }
    System.out.println("Image queue stopped!");
  }
  
  private PhotoEntry getRandomPhoto(List<PhotoEntry> inAlbumPhotos)
  {
    return inAlbumPhotos.get(new SecureRandom().nextInt(inAlbumPhotos.size()));
  }
  
  private AlbumEntry getRandomAlbum(List<AlbumEntry> inAlbums)
  {
    return inAlbums.get(new SecureRandom().nextInt(inAlbums.size()));
  }
  
  public PhotoDisplayVO getNextPhotoEntry()
  {
    return imageQueue.poll();
  }
  
  public boolean getCancelled()
  {
    return cancelled;
  }
  
  public void setCancelled(boolean inCancelled)
  {
    cancelled = inCancelled;
  }
  
}