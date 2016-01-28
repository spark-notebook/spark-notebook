Spark Notebook
==============

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/andypetrella/spark-notebook?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/andypetrella/spark-notebook.svg?branch=master)](https://travis-ci.org/andypetrella/spark-notebook)

*Originally forked from the amazing [scala-notebook](https://github.com/Bridgewater/scala-notebook), almost entirely refactored for Massive Dataset Analysis using [Apache Spark](http://spark.apache.org).*

The tool allows performing [reproducible analysis](http://simplystatistics.org/2014/06/06/the-real-reason-reproducible-research-is-important/) with Scala, Apache Spark and more.

This is achieved through an interactive web-based editor that can combine Scala code, SQL queries, Markup or even JavaScript in a collaborative manner.

The Spark is available out of the box, and is simply accessed by the variable `sparkContext`.

### Quick Start

Want to try out Spark Notebook? Do these steps.

* Go to [spark-notebook.io](http://spark-notebook.io/).
* Download one of the builds (master is the latest, but unstable).
* Extract the file somewhere convenient.
* Open a terminal/command window.
* Change to the root directory of the expanded distribution.
* Execute the command `bin/spark-notebook` (*NIX) or `bin\spark-notebook` (Windows).
* Open your browser to [localhost:9000](http://localhost:9000).

For details and cofiguration options, see [Launch](details.md#launch).

**Run straight from sources (for geeks)**

Checkout the sources and run:
  
    $ sbt run

### Learn more

C'mon on to [Gitter](https://gitter.im/andypetrella/spark-notebook?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
to discuss things, to get some help, or to start contributing!

The documentation is being rewritten. Meanwhile, read the [slightly outdated docs](details.md)
about the advanced features and configuration,
the different cluster deployment options (Amazon EMR, Mesons, YARN), and to find answers to FAQs.

## Testimonials
### Skymind - The [Deeplearning4j](http://Deeplearning4j.org)

> Spark Notebook gives us a clean, useful way to mix code and prose when we demo and explain our tech to customers. The Spark ecosystem needed this.

### [Vinted.com](http://www.vinted.com)

> It allows our analysts and developers (15+ users) to run ad-hoc queries, to perform complex data analysis and data visualisations, prototype machine learning pipelines. In addition, we use it to power our BI dashboards.

## Adopters

|                 Name                  |                                        Logo                                              |                     URL                     |              Description                            |
|---------------------------------------|------------------------------------------------------------------------------------------|---------------------------------------------|-----------------------------------------------------|
|            Data Fellas                | ![Data Fellas](http://www.data-fellas.guru/assets/images/logo-wired-small.png)           | [website](http://www.data-fellas.guru)      | Mad Data Science and Scalable Computing             |
|            Agile Lab                  | ![Agile Lab](http://www.agilelab.it/wp-content/uploads/2015/02/logo1.png)                | [website](http://www.agilelab.it)           | The only Italian Spark Certified systems integrator |
|            CloudPhysics               | ![CloudPhysics](https://www.cloudphysics.com/static/uploads/2014/06/3color_bug_lg.png)   | [website](http://www.cloudphysics.com)      | DATA-DRIVEN INSIGHTS FOR SMARTER IT                 |
| Aliyun | ![Alibaba - Aliyun ECS](http://gtms02.alicdn.com/tps/i2/T1J0xIFMteXXX4dCTl-220-72.png) | [product](http://market.aliyun.com/products/56014009/jxsc000194.html?spm=5176.900004.4.1.WGc3Ei) | Spark runtime environment on ECS and management tool of Spark Cluster running on Aliyun ECS  |
| EMBL European Bioinformatics Institute | ![EMBL - EBI](http://www.ebi.ac.uk/miriam/static/main/img/EBI_logo.png) | [website](http://www.ebi.ac.uk/) | EMBL-EBI provides freely available data from life science experiments, performs basic research in computational biology and offers an extensive user training programme, supporting researchers in academia and industry.  |
| Metail | ![Metail](http://metail.wpengine.com/wp-content/uploads/2013/11/Metail_Logo1.png) | [website](http://metail.com/) | The best body shape and garment fit company in the world. To create and empower everyone’s online body identity.|
| kt NexR | ![kt NexR](http://ktnexr.com/images/main/kt_h_logo.jpg) | [website](http://ktnexr.com)| the kt NexR is one of the leading BigData company in the Korea from 2007. |
| Skymind | ![Skymind](http://skymind.io/wp-content/uploads/2015/02/logo.png) | [website](http://www.skymind.io)| At Skymind, we’re tackling some of the most advanced problems in data analysis and machine intelligence. We offer start-of-the-art, flexible, scalable deep learning for industry. |
| Amino | ![Amino](https://amino.com/static/img/amino-logo-300x75.png) | [website](http://www.Amino.com)| A new way to get the facts about your health care choices. |
| Vinted | ![Vinted](http://engineering.vinted.com/brandbook/images/logos/vinted.svg) | [website](http://www.vinted.com/)| Online marketplace and a social network focused on young women’s lifestyle. |
| Vingle | ![Vingle](https://s3.amazonaws.com/vingle-assets/Vingle_Wordmark_Red.png) | [website](https://www.vingle.net)| Vingle is the community where you can meet someone like you. |
| 47 Degrees | ![47 Degrees](http://www.47deg.com/assets/logo_148x148.png) | [website](http://www.47deg.com)| 47 Degrees is a global consulting firm and certified Typesafe & Databricks Partner specializing in Scala & Spark. |
