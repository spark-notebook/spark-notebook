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

    # Prepare data for C3
    # [[header1, header2, ..]
    #  [col1, col2, ..]]
    prepare_c3_data = (headers, newData) =>
      dataRows = _.map(newData, (row) ->
        _.map(headers, (column)->
          row[column]
        )
      )
      [headers].concat(dataRows)

    try
      eval(options.js) # should create the `chartOptions` var
    catch error
      alert("Error when evaluating chartOptions (see console for the error)")
      console.log(error)

    console.log("chartOptions", chartOptions)
    chartOptions.bindto = "#" + chart_container.attr("id")
    chartOptions.data = {} if not chartOptions.data
    chartOptions.data.rows = []

    chart = c3.generate(chartOptions)

    chart.load({ rows: prepare_c3_data(options.headers, @dataInit) })
    chart.flush()

    dataO.subscribe( (newData) =>
      chart.unload();
      chart.load({
        rows: prepare_c3_data(options.headers, newData)
      });
    )
)
