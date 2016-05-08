/**
 * 
 */
package com.marasm.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.marasm.exceptions.TokenExpiredException;
import com.marasm.util.SecretProperties;

/**
 * @author mkorotkovas
 *
 */
public class GoogleApiBaseService
{
  protected String getClientSecret() throws IOException
  {
    return SecretProperties.getProperty(SecretProperties.CLIENT_SECRET);
  }
  
  protected String getClientId() throws IOException
  {
    return SecretProperties.getProperty(SecretProperties.CLIENT_ID);
  }
  
  protected <T extends GenericJson> T sendPostRequest(String inUrl, Map<String, String> inPostParamMap,
    Class<T> inResponseObjClass) throws IOException, TokenExpiredException
  {
    StringBuilder dataBuilder = new StringBuilder();
    if (inPostParamMap != null && !inPostParamMap.isEmpty())
    {
      for (Map.Entry<String, String> entry : inPostParamMap.entrySet())
      {
        if (dataBuilder.length() > 0)
        {
          dataBuilder.append("&");
        }
        dataBuilder.append(entry.getKey());
        dataBuilder.append("=");
        dataBuilder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
      }
    }
    
    URL url = new URL(inUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    connection.setRequestProperty("Content-Length", String.valueOf(dataBuilder.toString().length()));
    connection.connect();
    
    if (dataBuilder.length() > 0)
    {
      OutputStream out = connection.getOutputStream();
      out.write(dataBuilder.toString().getBytes());
      out.flush();
    }
    
    InputStream responseStream = null;
    int respCode = connection.getResponseCode();
    if (respCode == 200)
    {
      responseStream = connection.getInputStream();
      
      T respVO = JacksonFactory.getDefaultInstance().fromInputStream(responseStream, inResponseObjClass);
      return respVO;
    }
    else
      if (respCode == 401)// access token expired
      {
        throw new TokenExpiredException("Access Token Expired");
      }
      else
      {
        responseStream = connection.getErrorStream();
        GenericJson errorResp = JacksonFactory.getDefaultInstance().fromInputStream(responseStream, GenericJson.class);
        
        throw new IOException("Response Code: " + respCode + "\n" + (errorResp != null ? errorResp.toString() : ""));
      }
  }
  
  protected <T extends GenericJson> T sendGetRequest(String inUrl, Map<String, String> inParamMap,
    Class<T> inResponseObjClass) throws IOException, TokenExpiredException
  {
    StringBuilder urlParamsBuilder = new StringBuilder();
    if (inParamMap != null && !inParamMap.isEmpty())
    {
      for (Map.Entry<String, String> entry : inParamMap.entrySet())
      {
        if (urlParamsBuilder.length() > 0)
        {
          urlParamsBuilder.append("&");
        }
        else
        {
          urlParamsBuilder.append("?");
        }
        urlParamsBuilder.append(entry.getKey());
        urlParamsBuilder.append("=");
        urlParamsBuilder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
      }
    }
    
    URL url = new URL(inUrl + urlParamsBuilder.toString());
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(false);
    connection.setRequestMethod("GET");
    connection.connect();
    
    InputStream responseStream = null;
    int respCode = connection.getResponseCode();
    if (respCode == 200)
    {
      responseStream = connection.getInputStream();
      
      T respVO = JacksonFactory.getDefaultInstance().fromInputStream(responseStream, inResponseObjClass);
      return respVO;
    }
    else
      if (respCode == 401)// access token expired
      {
        throw new TokenExpiredException("Access Token Expired");
      }
      else
      {
        responseStream = connection.getErrorStream();
        GenericJson errorResp = JacksonFactory.getDefaultInstance().fromInputStream(responseStream, GenericJson.class);
        
        throw new IOException("Response Code: " + respCode + "\n" + (errorResp != null ? errorResp.toString() : ""));
      }
  }
}
