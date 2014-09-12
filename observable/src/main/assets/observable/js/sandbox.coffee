define([
    'observable'
    'knockout'
    'd3'
    'js!static/topojson.js'
], (Observable, ko, d3, tj) ->
  (elem, onData, extension) ->
    idxf = (idx) -> (d) -> d[idx]
    xf = idxf(0)
    yf = idxf(1)
    m =
      t: 4
      r: 4
      b: 15
      l: 30

    svg = d3.select(elem)

    w = Number(svg.attr('width'))
    h = Number(svg.attr('height'))

    svg.append('svg:g')
        .attr('class', 'x axis')
        .attr("transform", "translate(0, #{ h - m.b + 2 })")

    svg.append('svg:g')
        .attr('class', 'y axis')
        .attr("transform", "translate(#{ m.l - 2 })")

    dataO = Observable.makeObservableArray(@dataId)
    dataO.subscribe( (data) =>
      #eval(@onData)
      onData(data)
    )
    dataO(@dataInit)

    #eval(@extension)
    extension(@)
)
