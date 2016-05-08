/**
 * 
 */
package com.marasm.services;

import static com.marasm.constants.GPFConstants.API_ACCESS_SCOPE;
import static com.marasm.constants.GPFConstants.CREDENTIALS_DATA_STORE;
import static com.marasm.constants.GPFConstants.DEFAULT_USER;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.marasm.exceptions.TokenExpiredException;
import com.marasm.util.StringUtil;
import com.marasm.valueobjects.DeviceCodeResponseVO;

/**
 * @author mkorotkovas
 *
 */
public class DeviceAuthService extends GoogleApiBaseService
{
  
  
  
  public boolean hasStoredCredentials() throws IOException
  {
    return getStoredCredentials() != null;
  }
  
  public StoredCredential refreshAndStoreAccessToken() throws IOException 
  {
    StoredCredential curCredentials = getStoredCredentials();
    System.out.println("Refreshing Access Token");
    System.out.println("Current Token: " + curCredentials.getAccessToken());
    System.out.println("Refresh Token: " + curCredentials.getRefreshToken());
    
    Map<String, String> postParamMap = new HashMap<String, String>();
    postParamMap.put("client_id", getClientId());
    postParamMap.put("client_secret", getClientSecret());
    postParamMap.put("refresh_token", curCredentials.getRefreshToken());
    postParamMap.put("grant_type", "refresh_token");
    
    TokenResponse token;
    try
    {
      token = sendPostRequest("https://www.googleapis.com/oauth2/v4/token", 
        postParamMap, TokenResponse.class);
      //refresh token does not get returned on refres call, set it manually
      token.setRefreshToken(curCredentials.getRefreshToken()); 
      
      return storeAccessTokenData(token);
    }
    catch (TokenExpiredException e)
    {
      throw new IOException(e.getMessage());
    }
    
  }
  
  public StoredCredential getStoredCredentials() throws IOException
  {
    FileDataStoreFactory credStoreFactory = new FileDataStoreFactory(
      new File(System.getProperty("user.home"),".googlePhotosFrame/"));
    DataStore<StoredCredential> storedCredentials = credStoreFactory.getDataStore(CREDENTIALS_DATA_STORE);
    StoredCredential cred = storedCredentials.get(DEFAULT_USER);
    
    if (cred != null)
    {
      System.out.println("Retrieved stored credentials");
      System.out.println("Access Token: " + cred.getAccessToken());
      System.out.println("Refresh Token: " + cred.getRefreshToken());
      
      if (StringUtil.isEmpty(cred.getAccessToken()) || StringUtil.isEmpty(cred.getRefreshToken()))
      {
        throw new IOException("Stored Credentials invalid");
      }
    }
    else
    {
      System.out.println("No stored credential found");
    }
    
    return cred;
  }
  
  public TokenResponse getAccessToken(DeviceCodeResponseVO inDeviceCodeResponseVO) throws IOException
  {
    Map<String, String> postParamMap = new HashMap<String, String>();
    postParamMap.put("client_id", getClientId());
    postParamMap.put("client_secret", getClientSecret());
    postParamMap.put("code", inDeviceCodeResponseVO.getDeviceCode());
    postParamMap.put("grant_type", "http://oauth.net/grant_type/device/1.0");
    
    try
    {
      TokenResponse res = sendPostRequest("https://www.googleapis.com/oauth2/v4/token", 
        postParamMap, TokenResponse.class);
      return res;
    }
    catch (Exception e)
    {
      if (!e.getMessage().contains("400"))//400 is expected
      {
        throw new IOException("Error getting token: " + e.getMessage());
      }
      else
      {
        System.out.println("Error response: " + e.getMessage());
        return null;
      }
    }
    
  }
  
  public DeviceCodeResponseVO getDeviceAuthCode() throws IOException
  {
    Map<String, String> postParamMap = new HashMap<String, String>();
    postParamMap.put("client_id", getClientId());
    postParamMap.put("scope", API_ACCESS_SCOPE);
    
    DeviceCodeResponseVO respVO;
    try
    {
      respVO = sendPostRequest("https://accounts.google.com/o/oauth2/device/code",
        postParamMap, 
        DeviceCodeResponseVO.class);
    }
    catch (TokenExpiredException e)
    {
      throw new IOException(e.getMessage());
    }
    
    return respVO;
  }
  
  public StoredCredential storeAccessTokenData(TokenResponse inTokenResponse) throws IOException
  {
    FileDataStoreFactory credStoreFactory = new FileDataStoreFactory(
      new File(System.getProperty("user.home"),".googlePhotosFrame/"));
    
    GoogleCredential cred = new GoogleCredential.Builder()
      .setClientSecrets(getClientId(), getClientSecret())
      .setJsonFactory(new JacksonFactory())
      .setTransport(new NetHttpTransport())
      .build().setFromTokenResponse(inTokenResponse);
    
    StoredCredential res = new StoredCredential(cred);
    System.out.println("Storing credentials");
    System.out.println("Access Token: " + res.getAccessToken());
    System.out.println("Refresh Token: " + res.getRefreshToken());
    credStoreFactory.getDataStore(CREDENTIALS_DATA_STORE).set(DEFAULT_USER, res);
    
    return res;
  }
}
