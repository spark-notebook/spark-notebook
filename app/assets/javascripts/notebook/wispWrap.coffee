define([
    'observable'
    'knockout'
], (Observable, ko) ->
  (dataO, container, options) ->
    chartElement = $('<div style="min-width: 200px; height: 200px; margin: 0 auto"></div>')
    $(container).append(chartElement)
    console.log("wisp data")
    #console.log(@dataInit)
    data = @dataInit[0]
    data.chart.width = 400
    $(chartElement).highcharts(data)

    dataO.subscribe( (data) =>
      console.error(data)
    )
)