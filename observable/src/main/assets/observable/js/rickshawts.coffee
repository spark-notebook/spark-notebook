define([
    'observable'
    'knockout'
    'd3'
    'rickshaw'
], (Observable, ko, d3, Rickshaw) ->
  #(data, container) =>
  (dataO, container, options) ->
    gElmt = $("<div class='graph'></div>")
    $(container).append(gElmt)
    sElmt = $("<div class='slider'></div>")
    $(container).append(sElmt)

    series = null
    if options.fixed
      series = new Rickshaw.Series.FixedDuration(
        ({color: i.color, name: i.name, data: []} for i in @dataInit), # see below
        undefined, 
        {
          timeInterval: options.fixed.interval,
          maxDataPoints: options.fixed.max,
          timeBase: options.fixed.baseInSec
        }
      )
      # I suspect a bug in the setting of currentIndex and currentSize
      m = (@dataInit[0] && @dataInit[0].data && @dataInit[0].data.length) || 0
      if m > 0
        for d in [0...m]
          toadd = {}
          for o in @dataInit
            toadd[o.name] = o.data[d].y
          x = @dataInit[0].data[d].x
          series.addData(toadd, x)  

    else
      series = new Rickshaw.Series(@dataInit, undefined)

    graph = new Rickshaw.Graph({
        element: gElmt.get(0),
        width: 580,
        height: 250,
        renderer: options.renderer||"line",
        series: series
    })

    xAxis = new Rickshaw.Graph.Axis.Time({
        graph: graph
    })

    yAxis = new Rickshaw.Graph.Axis.Y( {
      graph: graph
    })

    udpate = null
    if options.update
      eval("update = "+options.update)
    else
      update = (graph, data) -> 
        m = (data[0] && data[0].data && data[0].data.length) || 0
        if m > 0
          for d in [ 0...m ]
            toadd = {}
            for o in data
              toadd[o.name] = o.data[d].y
            x = data[0].data[d].x
            graph.series.addData(toadd, x)  

    slider = new Rickshaw.Graph.RangeSlider({
      graph: graph,
      element: $(container).find(".slider").get(0)
    });

    dataO.subscribe( (data) =>
      update(graph, data)
      graph.render()
      xAxis.render()
    )

    graph.render()
    xAxis.render()
    yAxis.render();
)
