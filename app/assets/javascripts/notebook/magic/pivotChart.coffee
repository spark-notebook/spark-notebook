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

      window.c3 = c3

      $(container).pivotUI(@dataInit, {
        renderers: renderers,
        derivedAttributes: _.mapObject(options.derivedAttributes,
                                  (val, key) ->
                                    eval("var _f_ = " + val)
                                    _f_
                                )
            # {
            #    "Age Bin": derivers.bin("Age", 10),
            #    "Gender Imbalance": function(mp) {
            #        return mp["Gender"] == "Male" ? 1 : -1;
            #    }
            # },
        onRefresh: (options) -> $(".pvtUi path.c3-shape.c3-line").css("fill", "transparent"),
        rendererOptions: {
          c3: {
            size: {
              width: w*0.85
            }
          }
        }
      })

      dataO.subscribe( (newData) =>
        container.pivotUI(newData, {
          renderers: renderers
        })
      )
    )
)