package com.marasm.gpf.exceptions;
public class TokenExpiredException extends Exception
  {
    private static final long serialVersionUID = 1L;

    public TokenExpiredException()
    {
      super();
    }
    
    public TokenExpiredException(String inMsg)
    {
      super(inMsg);
    }
  }