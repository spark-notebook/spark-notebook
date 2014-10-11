#!/bin/bash

echo "Cloning spark-notebook"
if [ ! -d "spark-notebook" ]; then
  git clone https://github.com/andypetrella/spark-notebook.git
fi
cd spark-notebook

SBT_DIR="sbt"
if [ ! -d "$SBT_DIR" ]; then
  echo "Download sbt locally"
  wget https://dl.bintray.com/sbt/native-packages/sbt/0.13.6/sbt-0.13.6.tgz
  tar xvzf sbt*tgz
fi

echo "Launching Notebook"
echo "... it can take minutes. Your browser should open it if you didn't pass the '--no_browser' argument"
PARAM="server/run $@"
if [ "$1" == "dev" ]
then
  sbt/bin/sbt 'server/run --disable_security --no_browser' 2>&1 &
elif [ "$1" == "devb" ]
then
  sbt/bin/sbt 'server/run --disable_security' 2>&1 &
else 
  sbt/bin/sbt 'server/run' 2>&1 &
fi
