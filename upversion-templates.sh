#!/bin/bash

minorflag=false
if [ "$1" = "minor" ]; then
    minorflag=true
elif [ "$1" != "" ]; then
    echo "Parameters passed in are not supported; only minor flag is allowed"
    exit 1
fi

packages=(./conf/notifications/v*m*/)

for i in "${!packages[@]}"; do
    packages[$i]=${packages[$i]:21:4}
done

majorversion=0
minorversion=0

for i in "${!packages[@]}"; do
    if test ${packages[$i]:1:1} -gt $majorversion; then
        majorversion=${packages[$i]:1:1}
    fi
    if test ${packages[$i]:3:1} -gt $minorversion; then
        minorversion=${packages[$i]:1:1}
    fi
done

previouspackageversion=v${majorversion}m${minorversion}

if $minorflag; then
    ((minorversion++))
else
    ((majorversion++))
    minorversion=0
fi

newpackageversion=v${majorversion}m${minorversion}

mkdir ./app/views/notifications/${newpackageversion}
cp -r ./app/views/notifications/${previouspackageversion}/. ./app/views/notifications/${newpackageversion}

mkdir ./test/views/notifications/${newpackageversion}
cp -r ./test/views/notifications/${previouspackageversion}/. ./test/views/notifications/${newpackageversion}

viewunittestfilesfornewpackageversion=(./test/views/notifications/${newpackageversion}/*)

for i in "${!viewunittestfilesfornewpackageversion[@]}"; do
    viewunittestfile=${viewunittestfilesfornewpackageversion[$i]}
    sed -i '' "s/${previouspackageversion}/${newpackageversion}/g" $viewunittestfile
done

mkdir ./app/services/notifications/${newpackageversion}
cp -r ./app/services/notifications/${previouspackageversion}/. ./app/services/notifications/${newpackageversion}

mkdir ./test/services/notifications/${newpackageversion}
cp -r ./test/services/notifications/${previouspackageversion}/. ./test/services/notifications/${newpackageversion}

servicefilesfornewpackageversion=(./app/services/notifications/${newpackageversion}/*)

for i in "${!servicefilesfornewpackageversion[@]}"; do
    servicefile=${servicefilesfornewpackageversion[$i]}
    sed -i '' "s/${previouspackageversion}/${newpackageversion}/g" $servicefile
done

serviceunittestfilesfornewpackageversion=(./test/services/notifications/${newpackageversion}/*)

for i in "${!serviceunittestfilesfornewpackageversion[@]}"; do
    serviceunittestfile=${serviceunittestfilesfornewpackageversion[$i]}
    sed -i '' "s/${previouspackageversion}/${newpackageversion}/g" $serviceunittestfile
done

mkdir ./conf/notifications/${newpackageversion}
cp -r ./conf/notifications/${previouspackageversion}/. ./conf/notifications/${newpackageversion}

sed -i '' "s/${previouspackageversion}\"/${previouspackageversion}\",\"${newpackageversion}\"/g" ./test/NotificationsCheckSumSpec.scala