package com.marasm.gpf.valueobjects;

import java.io.Serializable;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

/**
 * @author MAKorotkovas
 *
 */
public class DeviceCodeResponseVO extends GenericJson 
implements Serializable
{
  private static final long serialVersionUID = 1L;
  
  @Key(value="device_code")
  private String deviceCode;
  @Key(value="user_code")
  private String userCode;
  @Key(value="verification_url")
  private String verificationUrl;
  @Key(value="expires_in")
  private int expiresIn;
  @Key
  private int interval;
  
  public String getDeviceCode()
  {
    return deviceCode;
  }
  public void setDeviceCode(String inDeviceCode)
  {
    deviceCode = inDeviceCode;
  }
  public String getUserCode()
  {
    return userCode;
  }
  public void setUserCode(String inUserCode)
  {
    userCode = inUserCode;
  }
  public String getVerificationUrl()
  {
    return verificationUrl;
  }
  public void setVerificationUrl(String inVerificationUrl)
  {
    verificationUrl = inVerificationUrl;
  }
  public int getExpiresIn()
  {
    return expiresIn;
  }
  public void setExpiresIn(int inExpiresIn)
  {
    expiresIn = inExpiresIn;
  }
  public int getInterval()
  {
    return interval;
  }
  public void setInterval(int inInterval)
  {
    interval = inInterval;
  }
  
}
