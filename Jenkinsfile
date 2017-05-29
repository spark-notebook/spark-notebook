node {
    wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {

        stage('Init variables') {
            env.JAVA_OPTS="-Djava.net.preferIPv4Stack=true"
            env.SBT_OPTS="-XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Xmx4G -XX:MaxPermSize=4G"

            env.BUILD_OPTS="" // "-Dscala.version=2.10.5 -Dspark.version=2.0.1"
        }

        stage('SCM') {
            timeout(time: 2, unit: 'MINUTES') {
                retry(3) {
                    checkout scm
                }
            }
            step([$class: 'GitHubSetCommitStatusBuilder'])
        }

        stage('Clean') {
            sh "/usr/bin/sbt clean"
        }

        stage('Test 2.11'){
            sh "/usr/bin/sbt -Dspark.version=2.0.2 -Dscala.version=2.11.8 test"
        }

        stage('Test 2.11 spark 2.1.1'){
            sh "/usr/bin/sbt -Dspark.version=2.1.1 -Dscala.version=2.11.8 test"
        }
    }
}
