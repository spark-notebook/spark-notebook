define([
    'observable'
    'knockout'
    'd3'
    'js!static/topojson.js'
], (Observable, ko, d3, tj) ->
  (data, svg, m) ->
    idxf = (idx) -> (d) -> d[idx]
    xf = idxf(0)
    yf = idxf(1)

    xScale   = d3.scale.linear()
                   .domain([d3.min(data, xf), d3.max(data, xf)])
                   .range([m.l, m.w - m.r])
    yScale   = d3.scale.linear()
                   .domain([d3.min(data, yf), d3.max(data, yf)])
                   .range([m.h - m.b, m.t])

    line = d3.svg.line()
      .x( (d) -> xScale(xf(d)) )
      .y( (d) -> yScale(yf(d)) )

    l = svg.selectAll('path.dataline').data([data])
    l.transition().attr('d', line)
    l.enter().append('path')
      .attr('class', 'dataline')
      .attr("d", line)
      .attr("stroke", "black") #should be in a css/less, but this is only an example


    xAxis = d3.svg.axis()
      .scale(xScale)
      .tickSize(-m.h, 0, 0)

    svg.select('g.x.axis').transition().call(xAxis)

    yAxis = d3.svg.axis()
      .scale(yScale)
      .orient('left')
      .tickSize(-m.w, 0, 0)

    svg.select('g.y.axis').transition().call(yAxis)
)
