define([
    'observable'
    'knockout'
    'd3'
    'radarChart'
], (Observable, ko, d3, radar) ->
  (dataO, container, options) ->
    w = options.width||600
    h = options.height||400

    chart_container = $("<div>")
    thisId = "radar"+this.genId
    chart_container.attr("id", thisId).appendTo(container)

    chart = radar.chart().config({w: w, h: h})

    svg = d3.select("#"+thisId).append('svg')
                                  .attr('width', w)
                                  .attr('height', h)

    shapeData = (d) ->
      data = []
      _.each(
        d,
        (v) ->
          data.push({
            className: v[options.classCol],
            axes: _.map(options.axisCols, (c) -> {axis: c, value: v[c]})
          })
      )
      data

    svg.append('g').datum(shapeData(@dataInit)).call(chart)

    #radar.draw("#"+thisId, data, {w: w, h: h})

    dataO.subscribe( (newData) =>
      svg.datum(shapeData(newData)).call(chart)
    )
)