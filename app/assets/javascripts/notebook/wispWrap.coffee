define([
  'jquery'
  'observable'
  'knockout'
  'highcharts'
  'highchartsMore'
], ($, Observable, ko, highcharts, hmore) ->
  (dataO, container, options) ->
    options = options || {}
    w = options.width || "400px"
    h = options.height || "200px"
    chartElement = $('<div style="min-width: '+w+'; height: '+h+'; margin: 0 auto"></div>')
    $(container).append(chartElement)
    #console.log("wisp data")
    #console.log(@dataInit)
    data = @dataInit[0]
    data.chart.width = parseInt(w)
    data.chart.height = parseInt(h)
    $(chartElement).highcharts(data)

    dataO.subscribe( (data) =>
      data = data[0]
      console.log("Update highcharts")
      console.log(data)
      data.chart.width = parseInt(w)
      data.chart.height = parseInt(h)
      $(chartElement).empty()
      $(chartElement).highcharts(data)
    )
)