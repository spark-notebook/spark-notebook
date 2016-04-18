
import com.typesafe.sbt.packager.archetypes.ServerLoader.{SystemV, Upstart, Systemd}

object DebianProperties extends BuildConf {
  val fileName      = ".debian.build.conf"

  val maintainer    = getString("debian.maintainer", "Andy Petrella <noootsab@data-fellas.guru>")

  val serverLoading = getString("debian.server_loading", "Systemd") match {
	case "Systemd" => Systemd
	case "SystemV" => SystemV
	case "Upstart" => Upstart
  }

}
