#!/bin/bash

echo "Please enter the Jira reference and a description. I.e. AMLS-0001 'My ticket description'"
read ref desc

mkdir -p "release_notes/"
echo "Generating release note for [$ref] - $desc"
echo " + [$ref] - $desc" > "release_notes/$ref.txt"