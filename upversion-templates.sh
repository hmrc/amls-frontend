minor=false
if [ "$1" = "minor" ]; then
    minor=true
fi
echo $minor

packages=(./app/views/notifications/*/)

for i in "${!packages[@]}"; do
    packages[$i]=${packages[$i]:26:4}
done

echo $packages

majorversion=${packages[0]:1:1}

echo $majorversion

minorversion=${packages[0]:3:1}

echo $minorversion

if $minor; then
    ((minorversion++))
    echo $minorversion
else
    ((majorversion++))
    minorversion=0
    echo $majorversion
fi

newpackageversion=v${majorversion}m${minorversion}

echo $newpackageversion