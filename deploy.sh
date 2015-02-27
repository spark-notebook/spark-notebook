sbt distAll && ~/opt/s3cmd-master/s3cmd put -P target/*deb s3://spark-notebook/deb/ &&  ~/opt/s3cmd-master/s3cmd put -P target/universal/*zip s3://spark-notebook/zip/ && sbt dockerPublishAll
