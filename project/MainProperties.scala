object MainProperties extends BuildConf {
  val fileName = ".main.build.conf"

  val organization = getString("main.organization", "io.kensu")
  val name         = getString("main.name", "spark-notebook")
}