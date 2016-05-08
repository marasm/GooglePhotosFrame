echo Getting latest version from GitHub...
git pull
git submodule update --recursive --init

echo clean up logs
rm -f gfp.log

echo disabling screen saver
xset s noblank 
xset s off 
xset -dpms

echo Running the app
gradle execute > gpf.log
sleep 30