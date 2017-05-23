package com.datafellas.g3nerator.model

import java.nio.file.Path

//TODO Use type for artifactory, now: (pull, push) → (user, password)
case class DependencyConfig(resolverCloudera: String,
                            resolverBintrayDataFellasMvn: String,
                            resolverBintraySparkPackagesMvn: String,
                            jdkHome: Path,
                            sbtHome: Path,
                            dockerBaseImage: String,
                            resolverSbtPlugin: String,
                            artifactory: Option[((String, String), Option[(String, String)])] = None
                    )



