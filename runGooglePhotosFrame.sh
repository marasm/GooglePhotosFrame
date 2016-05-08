echo Getting latest version from GitHub...
git pull
git submodule update --recursive --init

echo clean up logs
rm -f gfp.log

echo Running the app
gradle execute > gpf.log
sleep 30