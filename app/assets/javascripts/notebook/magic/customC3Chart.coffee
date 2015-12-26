define([
    'jquery',
    'observable'
    'underscore'
    'd3'
    'c3'
], ($, Observable, _, d3, c3) ->
  (dataO, container, options) ->
    # set height
    h = options.height||400

    chart_container = $("<div>").addClass("custom-c3-chart").attr("height", h+"px")
    chart_container.attr("id", "custom-c3-chart-"+@genId).appendTo(container)

    try
      eval(options.js) # should create the `chartOptions` var
    catch error
      alert("Error when evaluating chartOptions (see console for the error)")
      console.log(error)

    console.log("chartOptions", chartOptions)
    chartOptions.bindto = "#" + chart_container.attr("id")
    chartOptions.data = {} if not chartOptions.data
    chartOptions.data.json = @dataInit

    chartOptions.data.keys = { value: options.headers } if not chartOptions.data.keys or not chartOptions.data.keys.value

    chart = c3.generate(chartOptions)

    dataO.subscribe( (newData) =>
      chartOptions.data.json = newData#_.filter(newData, (obj) -> !_.findWhere(currentData, obj))
      #chartOptions.data.unload = true
      chart.load(chartOptions.data)
    )
)
