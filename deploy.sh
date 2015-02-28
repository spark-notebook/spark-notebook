#!/bin/bash

function zips() {
  V=$1

  echo "building zips"
  sbt -Dscala.version=$V distZips

  echo "deploying zips"
  ~/opt/s3cmd-master/s3cmd put -P target/universal/*zip s3://spark-notebook/zip/

  echo "purging zips"
  sbt -Dscala.version=$V clean
}

function debs() {
  V=$1

  echo "building debs"
  sbt -Dscala.version=$V distDebs

  echo "deploying debs"
  ~/opt/s3cmd-master/s3cmd put -P target/*deb s3://spark-notebook/deb/

  echo "purging debs"
  sbt -Dscala.version=$V clean
}

function dockers() {
  V=$1

  echo "deploying docker"
  sbt -Dscala.version=$V dockerPublishAll

  echo "purging debs"
  sbt -Dscala.version=$V clean
}

function all() {
  V=$1

  zips    $V
  debs    $V
  dockers $V
}

function main() {
  echo "Scala 2.10"
  all "2.10.4"

  echo "===================="

  echo "Scala 2.11"
  all "2.11.2"
}

main