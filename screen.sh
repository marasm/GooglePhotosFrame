#!/bin/bash

if [ "$1" == 'on' ]; then
  #Alternative method
  #tvservice -p;
  #fbset -depth 8;
  #fbset -depth 16;
  #chvt 6;
  #chvt 7;
  
  vcgencmd display_power 1
  
  echo 'Switched Screen ON!'
fi

if [ "$1" == 'off' ]; then
  
  #tvservice -o
  
  vcgencmd display_power 0
  
  echo 'Switched Screen OFF!'
fi


