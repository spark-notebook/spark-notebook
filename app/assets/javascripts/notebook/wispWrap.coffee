define([
    'observable'
    'knockout'
], (Observable, ko) ->
  (dataO, container, options) ->
    chartElement = $('<div style="min-width: 400px; height: 400px; margin: 0 auto"></div>')
    $(container).append(chartElement)
    console.log("wisp data")
    console.log(data)
    $(chartElement).highcharts( @dataInit )

    dataO.subscribe( (data) =>
      console.error(data)
    )
)