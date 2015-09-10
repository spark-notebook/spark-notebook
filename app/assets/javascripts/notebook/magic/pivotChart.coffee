define([
    'jquery'
    'underscore'
    'observable'
    'knockout'
    'd3'
    'c3'
    'pivot'
], ($, _, Observable, ko, d3, c3, pivot) ->
  (dataO, container, options) ->
    require(['c3', 'pivotC3'],
    (c3, pivotC3) =>
      w = options.width||600
      h = options.height||400

      derivers = $.pivotUtilities.derivers;
      renderers = $.extend($.pivotUtilities.renderers, $.pivotUtilities.c3_renderers)
      derivedAttributes = _.mapObject(options.derivedAttributes,
                                  (val, key) ->
                                    eval("var _f_ = " + val)
                                    _f_
                                )
      refresh = (options) ->
        $(".pvtUi path.c3-shape.c3-line").css("fill", "transparent")
        $(".pvtUi").css("width", "100%")
      rendererOptions = {
                          c3: {
                            size: {
                              height: h
                              width: w
                            }
                          }
                        }
      window.c3 = c3


      plotThat = (data) =>
        $(container).pivotUI(data, {
          renderers: renderers
          derivedAttributes: derivedAttributes
          onRefresh: refresh
          rendererOptions: rendererOptions
        })

      dataO.subscribe( (newData) =>
        plotThat(newData)
      )
      plotThat(@dataInit)
    )
)