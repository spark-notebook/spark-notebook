package filters

import javax.inject.Inject

import org.pac4j.play.filters.SecurityFilter
import play.api.http.HttpFilters

class Filters @Inject()(securityFilter: SecurityFilter) extends HttpFilters {

  def filters = Seq(securityFilter)

}
