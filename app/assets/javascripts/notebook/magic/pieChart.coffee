define([
    'observable'
    'knockout'
    'd3'
    'dimple'
], (Observable, ko, d3, dimple) ->
  (dataO, container, options) ->
    w = options.width||600
    h = options.height||400

    count = d3.select(container).append("p")
    count .selectAll("span .nrow")
          .data([options.nrow])
          .enter()
          .append("span")
          .attr("class", "nrow")
            .text( (d) ->
              d + " items"
            )
    displayShown = (c) =>
      shown = count.selectAll("span.shown")
                    .data([c])
      shown.remove()
      shown.enter()
            .append("span")
            .attr("class", "shown")
            .attr("style", "color: red")
            .text( (d) ->
              if options.nrow > d
                "(showing " +  d + ")"
              else
                ""
            )
    displayShown(options.shown)

    svg = d3.select(container).append("svg:svg").attr("width", w+"px").attr("height", h+"px").attr("id", "pie"+@genId)
    chart = new dimple.chart(svg, @dataInit)
    chart.setBounds(w*(100/600), h*(30/400), w *(380/600), h*(360/400))

    chart.addMeasureAxis("p", options.p)
    chart.addSeries(options.series, dimple.plot.pie)
    chart.addLegend(w*(20/600), h*(30/400), w*(60/600), h*(350/400), "left");
    chart.draw()

    dataO.subscribe( (newData) =>
      displayShown(newData.length)
      chart.data = newData
      chart.draw(1000)
    )
)