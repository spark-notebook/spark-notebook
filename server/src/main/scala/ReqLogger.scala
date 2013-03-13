package com.bwater.notebook
import unfiltered.request._
import unfiltered.response.Pass
import util.Logging

/**
 * Author: Ken
 */

class ReqLogger extends Logging {
    val intent:unfiltered.netty.cycle.Plan.Intent = {
      case req@GET(Path(p)) =>
        logInfo("GET " + req.uri)
        Pass
      case req@POST(Path(p) & Params(params)) =>
        logInfo("POST %s: %s".format(p, params))
        Pass
    }
}
