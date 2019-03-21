#!/bin/bash

echo "Please enter the tag. I.e. v0.0.1:"
read tag

mkdir -p "release_notes/releases/$tag"
echo "Generating release notes for $tag"

cat release_notes/*.txt > release_notes/releases/$tag/"$tag-$(date +'%d-%m-%Y')".txt
rm -rf release_notes/*.txt
