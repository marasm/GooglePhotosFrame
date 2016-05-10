echo Getting latest version from GitHub...
git pull
git submodule update --recursive --checkout --force --init


echo disabling screen saver
xset s noblank 
xset s off 
xset -dpms

echo Running the app
gradle execute 
sleep 30