echo Getting latest version from GitHub...
git pull
git submodule update --recursive --init

echo create logs directory
mkdir logs

echo disabling screen saver
xset s noblank 
xset s off 
xset -dpms

echo Running the app
gradle execute 
sleep 30