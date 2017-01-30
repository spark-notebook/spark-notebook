#!/bin/bash
echo "processing file $1"
cat header.txt > /tmp/header.tmp
cat $1 >> /tmp/header.tmp
cat footer.txt >> /tmp/header.tmp
mv /tmp/header.tmp $1
