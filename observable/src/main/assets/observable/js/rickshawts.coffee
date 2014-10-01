define([
    'observable'
    'knockout'
    'd3'
    'rickshaw'
], (Observable, ko, d3, Rickshaw) ->
  #(data, container) =>
  (dataO, container) ->
    gElmt = $("<div class='graph'></div>")
    $(container).append(gElmt)
    sElmt = $("<div class='slider'></div>")
    $(container).append(sElmt)
    #series = {
    #  color: 'steelblue',
    #  data: @dataInit
    #}
    series = @dataInit
    graph = new Rickshaw.Graph({
        element: gElmt.get(0),
        width: 580,
        height: 250,
        series: series
    })
    slider = new Rickshaw.Graph.RangeSlider({
      graph: graph,
      element: $(container).find(".slider").get(0)
    });
    dataO.subscribe( (data) =>
      console.dir("DATAAAAA")
      console.dir(data)
      #graph.configure({series: data})
      graph.series.forEach((x, i) -> x.data = data[i].data)
      #series.data = data
      graph.render()
    )
    graph.render()
)
