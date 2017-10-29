# GooglePhotosFrame
Application to display photos from Google Photos as a slideshow in full screen.

## Setup the autostart of the app
add the following line to the ``~/.config/lxsession/LXDE-pi/autostart`` file:

```
@lxterminal --command=/home/pi/googlePhotoFrame/runGooglePhotosFrame.sh
```

## Auto-Hide mouse cursor
install unclutter

```
apt-get install unclutter
```

add the following line to the ``~/.config/lxsession/LXDE-pi/autostart`` file:

```
@unclutter -idle 0
```