package notebook.front.third.bokeh

import io.continuum.bokeh.{Renderer, Plot}

/**
 * Created by gerrit on 15.11.14.
 */
object PlotOps {

  implicit class PlotOps(val plot: Plot) {

    def +=(renderer: Renderer): Plot = {
      plot.renderers := renderer :: plot.renderers.value
      plot
    }

    def ++=(renderers: List[Renderer]): Plot = {
      plot.renderers := renderers ++ plot.renderers.value
      plot
    }
  }

}