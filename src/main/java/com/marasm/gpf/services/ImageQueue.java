package com.marasm.gpf.services;

import java.awt.Dimension;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.util.ServiceForbiddenException;
import com.marasm.gpf.util.GPFUtils;
import com.marasm.gpf.valueobjects.PhotoDisplayVO;

public class ImageQueue implements Runnable
{
  private static final int MAX_QUEUE_SIZE = 20;
  private static final int FULL_QUEUE_WAIT_TIME = 2 * 60 * 1000;//2 minutes
  private static final int INITIAL_ERROR_RETRY_INTERVAL = 1 * 1000;//1 second
  private static final int MAX_ERROR_RETRY_INTERVAL = 5 * 60 * 1000;//5 minutes
  private static final int MAX_TIME_ALLOWED_FOR_ERROR_RECOVERY = 30 * 60 * 1000;//30 minutes
  
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
      ExponentialBackOff backoff = new ExponentialBackOff.Builder()
                                                         .setInitialIntervalMillis(INITIAL_ERROR_RETRY_INTERVAL)
                                                         .setMaxIntervalMillis(MAX_ERROR_RETRY_INTERVAL)
                                                         .setMaxElapsedTimeMillis(MAX_TIME_ALLOWED_FOR_ERROR_RECOVERY)
                                                         .build();
      boolean inErrorRecovery = false;
      
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
            
            //all operations succeeded reser error flag
            inErrorRecovery = false;
          }
          catch (Exception e)
          {
            if (!inErrorRecovery)
            {
              inErrorRecovery = true;
              backoff.reset();
            }
            long waitTime = backoff.nextBackOffMillis();
            if (waitTime != BackOff.STOP)
            {
              if (e instanceof ServiceForbiddenException)// token expired ==> refresh
              {
                System.out.println("Access token expired. Refreshing token.");
                try
                {
                  authService.refreshAndStoreAccessToken();
                  continue;
                }
                catch (Exception refreshExc) 
                {
                  refreshExc.printStackTrace(); //just log will retry after a pause
                }
              }
              Thread.sleep(waitTime);
              continue;
            }
            else 
            {
              System.out.println("all retries failed and backoff time limit exceeded == unable to recover :(");
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
            Thread.sleep(FULL_QUEUE_WAIT_TIME);
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