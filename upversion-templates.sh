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

echo "Creating new template version $newpackageversion"

mkdir ./app/views/notifications/${newpackageversion}
cp -r ./app/views/notifications/${previouspackageversion}/. ./app/views/notifications/${newpackageversion}

echo "Copied directory ./app/views/notifications/..."

mkdir ./test/views/notifications/${newpackageversion}
cp -r ./test/views/notifications/${previouspackageversion}/. ./test/views/notifications/${newpackageversion}

echo "Copied directory ./test/views/notifications/..."

viewunittestfilesfornewpackageversion=(./test/views/notifications/${newpackageversion}/*)

for i in "${!viewunittestfilesfornewpackageversion[@]}"; do
    viewunittestfile=${viewunittestfilesfornewpackageversion[$i]}
    sed -i '' "s/${previouspackageversion}/${newpackageversion}/g" $viewunittestfile
done

echo "Updated package for new directory ./test/views/notifications/$newpackageversion"

mkdir ./app/services/notifications/${newpackageversion}
cp -r ./app/services/notifications/${previouspackageversion}/. ./app/services/notifications/${newpackageversion}

echo "Copied directory ./app/services/notifications/..."

mkdir ./test/services/notifications/${newpackageversion}
cp -r ./test/services/notifications/${previouspackageversion}/. ./test/services/notifications/${newpackageversion}

echo "Copied directory ./test/services/notifications/..."

servicefilesfornewpackageversion=(./app/services/notifications/${newpackageversion}/*)

for i in "${!servicefilesfornewpackageversion[@]}"; do
    servicefile=${servicefilesfornewpackageversion[$i]}
    sed -i '' "s/${previouspackageversion}/${newpackageversion}/g" $servicefile
done

echo "Updated package for new directory ./app/services/notifications/$newpackageversion"

serviceunittestfilesfornewpackageversion=(./test/services/notifications/${newpackageversion}/*)

for i in "${!serviceunittestfilesfornewpackageversion[@]}"; do
    serviceunittestfile=${serviceunittestfilesfornewpackageversion[$i]}
    sed -i '' "s/${previouspackageversion}/${newpackageversion}/g" $serviceunittestfile
done

echo "Updated package for new directory ./test/services/notifications/$newpackageversion"

mkdir ./conf/notifications/${newpackageversion}
cp -r ./conf/notifications/${previouspackageversion}/. ./conf/notifications/${newpackageversion}

echo "Copied checksums files in ./conf/notifications/..."

sed -i '' "s/${previouspackageversion}\"/${previouspackageversion}\",\"${newpackageversion}\"/g" ./test/NotificationsCheckSumSpec.scala

echo "Added $newpackageversion to ./test/NotificationsCheckSumSpec.scala"
echo "TODO: checksums for services will have to be updated"

echo "Upversion complete"