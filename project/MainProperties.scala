object MainProperties extends BuildConf {
  val fileName = ".main.build.conf"

  val organization = getString("main.organization", "nooostab")
  val name         = getString("main.name", "spark-notebook")
}