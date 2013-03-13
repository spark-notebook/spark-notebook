package com.bwater.notebook
package server

import java.net.URLEncoder

/**
 * Global variables available to all templates
 */

object LayoutTools {
  def static_url(path: String) = "/static/" + path

  // Items that will eventually move into the app
  val base_project_url = "/"
  val base_kernel_url = "/"
  val base_observable_url = "observable" // TODO: Ugh...
  val read_only = false
  val mathjax_url = "" // http://cdn.mathjax.org/mathjax/latest/MathJax.js"

  def encode(text: String) = URLEncoder.encode(text, "UTF-8")
  
}
