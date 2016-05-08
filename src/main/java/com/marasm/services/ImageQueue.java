package com.marasm.services;

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

public class ImageQueue implements Runnable
  {
    private static final int MAX_QUEUE_SIZE = 20;
    
    private List<AlbumEntry> albums = new ArrayList<AlbumEntry>();
    private Queue<PhotoEntry> imageQueue = new ConcurrentLinkedQueue<PhotoEntry>();
    private boolean cancelled;
    

    @Override
    public void run()
    {
      System.out.println("Starting image queue worker");
      try
      {
        DeviceAuthService authService = new DeviceAuthService();
        PhotosService photosService = new PhotosService(authService);
        
        while(!cancelled)
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
              System.out.println("Picked photo: " + randomPhoto.getId());
              imageQueue.add(randomPhoto);
            }
            catch (ServiceException e)
            {
              if (e instanceof ServiceForbiddenException)//token expired
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
      catch(Exception e)
      {
        //TODO
        e.printStackTrace();
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

    public PhotoEntry getNextPhotoEntry()
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