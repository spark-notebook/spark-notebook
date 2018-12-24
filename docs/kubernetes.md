# documentation

## Build and deploy on kubernetes

### 1. Prepare the docker image build configuration

Docker image custom build is configured with the file named `.docker.build.conf` in the root directory, an example of kubernetes compatible docker image configuration is in `.docker.build.conf.kubernetes.example`.

This simply adds `kuberctl` inside the docker image so the spark-notebook image can create executor pods.

Set the maintainer and registry in the file and rename to `.docker.build.conf`.

e.g:

```
docker {
  maintainer = "Luis Azazel Cypher"
  registry = "eu.gcr.io/spark-kubernetes-666666"
  commands = [
    { cmd = USER, arg = root },
    { cmd = RUN, arg = "apt-get update --fix-missing && apt-get install -y -t jessie-backports --no-install-recommends openjdk-8-jdk" },
    { cmd = ENV, arg = "JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64" },
    { cmd = RUN, arg = "apt-get install -y wget curl"},
    { cmd = ADD, arg = "https://storage.googleapis.com/kubernetes-release/release/v1.6.4/bin/linux/amd64/kubectl /usr/local/bin/kubectl" },
    { cmd = RUN, arg = "chmod +x /usr/local/bin/kubectl" }
  ]
}
```

### 2. Build the image locally:

```
sbt -Dwith.kubernetes -Dspark.version=2.4.0 docker:publishLocal
```
