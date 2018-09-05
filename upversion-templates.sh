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