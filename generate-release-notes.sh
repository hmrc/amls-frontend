#!/bin/bash

mkdir -p "release_notes/releases/$1"
echo "Generating release notes for $1"

cat release_notes/*.txt > release_notes/releases/$1/"$1-$(date +'%d-%m-%Y')".txt
rm -rf release_notes/*.txt