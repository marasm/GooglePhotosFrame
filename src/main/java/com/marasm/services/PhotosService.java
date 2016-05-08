package com.marasm.services;

import static com.marasm.constants.GPFConstants.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gdata.client.GoogleAuthTokenFactory;
import com.google.gdata.client.Service.GDataRequestFactory;
import com.google.gdata.client.http.HttpGDataRequest;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;

public class PhotosService extends GoogleApiBaseService
{
  
  private DeviceAuthService authService;
  
  public PhotosService(DeviceAuthService inAuthService)
  {
    authService = inAuthService;
  }
  
  @SuppressWarnings("rawtypes")
  public List<PhotoEntry> getAlbumPhotos(String inAlbumId) throws IOException, ServiceException
  {
    // Auto-generated method stub
    System.out.println("Getting photos list from album: " + inAlbumId);
    PicasawebService picasaService = getPicasaService();
    URL feedUrl = new URL("https://picasaweb.google.com/data/feed/api/user/" + DEFAULT_USER + "/albumid/" + inAlbumId);
    AlbumFeed feed = picasaService.getFeed(feedUrl, AlbumFeed.class);
    List<GphotoEntry> gEntries = feed.getEntries();
    List<PhotoEntry> res = new ArrayList<PhotoEntry>();
    for (GphotoEntry e : gEntries)
    {
      res.add(new PhotoEntry(e));
    }
    return res;
  }
  
  @SuppressWarnings("rawtypes")
  public List<AlbumEntry> getAllUserAlbums() throws IOException, ServiceException
  {
    System.out.println("Getting list of albums");
    PicasawebService picasaService = getPicasaService();
    URL feedUrl = new URL("https://picasaweb.google.com/data/feed/api/user/" + DEFAULT_USER + "?kind=album&access=all");
    UserFeed myUserFeed = picasaService.getFeed(feedUrl, UserFeed.class);
    List<GphotoEntry> gEntries = myUserFeed.getEntries();
    List<AlbumEntry> res = new ArrayList<AlbumEntry>();
    for (GphotoEntry e : gEntries)
    {
      res.add(new AlbumEntry(e));
    }
    return res;
  }
  
  protected PicasawebService getPicasaService() throws IOException
  {
    StoredCredential cred = authService.getStoredCredentials();
    
    GoogleAuthTokenFactory authTokenFactory =
      new GoogleAuthTokenFactory("picasa", "com.marasm-GooglePhotosFrame-0.1", null);
    authTokenFactory.setOAuth2Credentials(
      new GoogleCredential.Builder().setClientSecrets(getClientId(), getClientSecret())
      .setJsonFactory(new JacksonFactory()).setTransport(new NetHttpTransport()).build()
      .setAccessToken(cred.getAccessToken()).setRefreshToken(cred.getRefreshToken()));
    
    GDataRequestFactory gDataFactory = new HttpGDataRequest.Factory();
    
    PicasawebService photoService = new PicasawebService("GooglePhotosFrame", gDataFactory, authTokenFactory);
    photoService.setAuthSubToken(cred.getAccessToken());
    
    System.out.println("Using access token=" + cred.getAccessToken());
    
    return photoService;
  }
}