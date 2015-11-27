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

      get_cell = () ->
        # get cell object, like done in get_cell_elements in notebook.js
        cell_dom_element = $(container).parents(".cell").not('.cell .cell')[0]
        $(cell_dom_element).data("cell")

      get_saved_pivot_state = () ->
        saved_state = get_cell().metadata.presentation?.pivot_chart_state
        JSON.parse(saved_state || "{}") || {}

      extract_pivot_state = (pivotConfig) ->
        # based on http://nicolas.kruchten.com/pivottable/examples/onrefresh.html
        pivotState = JSON.parse(JSON.stringify(pivotConfig));
        # delete some values which are functions
        delete pivotState["aggregators"]
        delete pivotState["renderers"]
        delete pivotState["derivedAttributes"]
        # delete some bulky default values
        delete pivotState["rendererOptions"]
        delete pivotState["localeStrings"]
        JSON.stringify(pivotState, undefined, 2)

      save_pivot_state = (state) ->
        cell = get_cell()
        cell.metadata.presentation = {} if not cell.metadata.presentation
        cell.metadata.presentation.pivot_chart_state = state

      refresh = (options) ->
        $(".pvtUi path.c3-shape.c3-line").css("fill", "transparent")
        $(".pvtUi").css("width", "100%")
        save_pivot_state(extract_pivot_state(options))

      rendererOptions = {
                          c3: {
                            size: {
                              height: h
                              width: w
                            }
                          }
                        }
      window.c3 = c3

      pivotOptions = get_saved_pivot_state()
      # console.log("saved_pivot_state=", pivotOptions)

      pivotOptions.renderers = renderers
      pivotOptions.derivedAttributes = derivedAttributes
      pivotOptions.onRefresh = refresh
      pivotOptions.rendererOptions = rendererOptions

      plotThat = (data) =>
        $(container).pivotUI(data, pivotOptions)

      dataO.subscribe( (newData) =>
        plotThat(newData)
      )
      plotThat(@dataInit)
    )
)