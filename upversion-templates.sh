minorflag=false
if [ "$1" = "minor" ]; then
    minorflag=true
fi

packages=(./app/views/notifications/*/)

for i in "${!packages[@]}"; do
    packages[$i]=${packages[$i]:26:4}
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

echo $previouspackageversion
echo $newpackageversion

mkdir -p ./app/views/notifications/${newpackageversion}
mkdir -p ./test/views/notifications/${newpackageversion}

cp -r ./app/views/notifications/${previouspackageversion}/. ./app/views/notifications/${newpackageversion}
cp -r ./test/views/notifications/${previouspackageversion}/. ./test/views/notifications/${newpackageversion}