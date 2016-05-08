echo Getting latest version from GitHub...
git pull

echo clean up logs
rm -f gfp.log

echo Running the app
gradle execute > gpf.log