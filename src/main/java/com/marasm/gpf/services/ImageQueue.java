package com.marasm.gpf.services;

import java.awt.Dimension;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.util.ServiceForbiddenException;
import com.marasm.gpf.util.AppProperties;
import com.marasm.gpf.util.GPFUtils;
import com.marasm.gpf.valueobjects.PhotoDisplayVO;
import com.marasm.logger.AppLogger;
import com.marasm.util.StringUtil;

public class ImageQueue implements Runnable
{
  private static final int MAX_QUEUE_SIZE = 10;
  private static final int MAX_IMAGES_BEFORE_ALBUM_LIST_REFRESH = 600;
  private static final int FULL_QUEUE_WAIT_TIME = 2 * 60 * 1000;//2 minutes
  private static final int INITIAL_ERROR_RETRY_INTERVAL = 1 * 1000;//1 second
  private static final int MAX_ERROR_RETRY_INTERVAL = 15 * 60 * 1000;//5 minutes
  private static final int MAX_TIME_ALLOWED_FOR_ERROR_RECOVERY = 30 * 60 * 1000;//30 minutes
  
  private List<AlbumEntry> albums = new ArrayList<AlbumEntry>();
  
  private List<String> albumsToIgnore = new ArrayList<String>();
  
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
    AppLogger.debug("Starting image queue worker");
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
      albumsToIgnore = getAlbumsToIgnore();
      int imagesProcessed = 0;
      
      while (!cancelled)
      {
        if (imageQueue.size() < MAX_QUEUE_SIZE)
        {
          try
          {
            if (albums.isEmpty() || imagesProcessed >= MAX_IMAGES_BEFORE_ALBUM_LIST_REFRESH)
            {
              AppLogger.debug("Album list empty or needs a refresh. Getting a fresh list.");
              albums = photosService.getAllUserAlbums();
              if (albums == null || albums.isEmpty())
              {
                throw new IOException("Unable to get the albums");
              }
            }
            // pick a random album and a random pic from it
            AlbumEntry randomAlbum = getRandomAlbum(albums);
            AppLogger.debug("Picked album: {}", randomAlbum.getTitle().getPlainText());
            if (albumsToIgnore.contains(randomAlbum.getTitle().getPlainText()))
            {
              AppLogger.debug("Album {} is in the list to be ignored, skipping.", 
                randomAlbum.getTitle().getPlainText());
              continue;
            }
            
            List<PhotoEntry> albumPhotos = photosService.getAlbumPhotos(randomAlbum.getGphotoId());
            if (albumPhotos.isEmpty())
            {
              AppLogger.debug("Album {} is empty. Will pick another", 
                randomAlbum.getTitle().getPlainText());
              continue;
            }
            PhotoEntry randomPhoto = getRandomPhoto(albumPhotos);
            if (randomPhoto.getMediaContents() == null || randomPhoto.getMediaContents().isEmpty())
            {
              AppLogger.debug("Photo does not have media contents. Skipping");
              continue;
            }
            
            PhotoDisplayVO photoDisplayVO =
              new PhotoDisplayVO(GPFUtils.getSizeSpecificUrlForImage(randomPhoto.getMediaContents().get(0), screenSize),
                randomAlbum.getTitle().getPlainText(),
                randomPhoto.getExifTags() != null ? randomPhoto.getExifTags().getTime() : null);
            AppLogger.debug("Picked photo: {}", randomPhoto.getGphotoId());
            imageQueue.add(photoDisplayVO);
            imagesProcessed++;
            
            //all operations succeeded reser error flag
            inErrorRecovery = false;
          }
          catch (Exception e)
          {
            AppLogger.debug("Error detected: {}. Will try to recover.", e.getMessage());
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
                AppLogger.info("Access token expired. Refreshing token.");
                try
                {
                  authService.refreshAndStoreAccessToken();
                  continue;
                }
                catch (Exception refreshExc) 
                {
                  //just log will retry after a pause
                  AppLogger.warn("Error while refreshing token. Will retry.", refreshExc); 
                }
              }
              Thread.sleep(waitTime);
              continue;
            }
            else 
            {
              AppLogger.error(
                "all retries failed and backoff time limit exceeded: unable to recover :(");
              throw e;
            }

          }
        }
        else
        {
          AppLogger.debug("Image queue is full. Wait...");
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
      AppLogger.error("Unrecoverable Error: ", e);
      cancelled = true;
    }
    AppLogger.warn("Image queue stopped!");
  }
  
  protected List<String> getAlbumsToIgnore()
  {
    List<String> res = new ArrayList<String>();
    try
    {
      String albumsCSV = AppProperties.getProperty(AppProperties.ALBUMS_TO_IGNORE_PROP);
      if (!StringUtil.isEmpty(albumsCSV))
      {
        String[] albums = albumsCSV.split(",");
        res.addAll(Arrays.asList(albums));
      }
    }
    catch (IOException e)
    {
      AppLogger.warn("Unable to get list of albums to ignore. Will use all albums.", e);
    }
    return res;
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