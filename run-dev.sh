#!/bin/bash

export SPARK_LOCAL_IP=localhost

# For git notebook storage to work, these env vars have to available (e.g. put in ~/.bashrc)
export SN_NOTEBOOKS_GIT_HTTPS_REPO
export SN_NOTEBOOKS_GIT_USER
export SN_NOTEBOOKS_GIT_TOKEN

RUN_MODE="$1"

if [ "$RUN_MODE" = "" ]
then
  echo "USAGE: ./run-dev.sh [local|git]"
  exit -1
fi

get_abs_filename() {
  # $1 : relative filename
  echo "$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
}

if [ "$RUN_MODE" = "git" ]
then
  DEFAULT_NB_DIR=$(get_abs_filename "$(pwd)/../sample-git-notebooks")
  export NOTEBOOKS_DIR="${NOTEBOOKS_DIR:-"$DEFAULT_NB_DIR"}"
  export CONFIG_FILE="conf/application-git-storage.conf"
else
  export NOTEBOOKS_DIR="${NOTEBOOKS_DIR:-"$(pwd)/notebooks"}"
  export CONFIG_FILE="conf/application.conf"
fi
echo "Notebooks dir: $NOTEBOOKS_DIR . Config: $CONFIG_FILE"

# "set offline := true"  \
HADOOP_CONF_DIR=/etc/hadoop/conf:/etc/hive/conf sbt \
  -Dconfig.file=${CONFIG_FILE} \
  -Dscala.version=2.10.6 \
  -Dwith.hive=true \
  -Dwith.parquet=true \
  -Dmanager.notebooks.override.sparkConf.spark.port.maxRetries=100 \
  clean run
