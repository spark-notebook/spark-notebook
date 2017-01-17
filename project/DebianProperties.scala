
import com.typesafe.sbt.packager.archetypes.ServerLoader.{SystemV, Upstart, Systemd}

object DebianProperties extends BuildConf {
  val fileName      = ".debian.build.conf"

  val maintainer    = getString("debian.maintainer", "Andy Petrella <noootsab@data-fellas.guru>")

  val daemonUser    = getString("debian.daemon_user", MainProperties.name)
  val daemonGroup   = getString("debian.daemon_group", MainProperties.name)

  val serverLoading = getString("debian.server_loading", "Systemd") match {
	case "Systemd" => Systemd
	case "SystemV" => SystemV
	case "Upstart" => Upstart
  }

}
