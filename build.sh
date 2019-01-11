#!/bin/sh

# since we only support linux and mac,
# if we are not on mac then we are on linux:
if [[ "$( uname )" == 'Darwin'* ]]
then
  sed -e 's/presets.linuxX64/presets.macosX64/' -i '' 'build.gradle'
else
  sed -e 's/presets.macosX64/presets.linuxX64/' -i '' 'build.gradle'
fi

exec gradle clean build
