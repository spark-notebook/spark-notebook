define([
    'jquery',
    'underscore',
    'tauCharts',
    'taucharts_root/tauCharts.color-brewer',
    'taucharts_root/plugins/tauCharts.legend',
    'taucharts_root/plugins/tauCharts.annotations',
    'taucharts_root/plugins/tauCharts.export',
    'taucharts_root/plugins/tauCharts.layers',
    'taucharts_root/plugins/tauCharts.quick-filter',
    'taucharts_root/plugins/tauCharts.tooltip',
    'taucharts_root/plugins/tauCharts.trendline',
], ($, _, tauCharts, brewer, legend, annotations, exporter, layers, filter, tip, trend) ->
  (dataO, container, options) ->
    h = options.height||400

    chart_container = $("<div>").addClass("custom-tau-chart").attr("height", h+"px")
    chart_container.attr("id", "custom-tau-chart-"+@genId).appendTo(container)

    try
      eval(options.js) # should create the `chartOptions` var
    catch error
      alert("Error when evaluating chartOptions (see console for the error)")
      console.log(error)

    chartOptions.data = @dataInit

    chart = new tauCharts.Chart(chartOptions)
    chart.renderTo("#" + chart_container.attr("id"))

    dataO.subscribe( (newData) =>
      chart.setData(newData)
    )
)
