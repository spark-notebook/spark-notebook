define([
    'observable'
    'knockout'
    'd3'
    'dimple'
], (Observable, ko, d3, dimple) ->
  (dataO, container, options) ->
    w = options.width||600
    h = options.height||400
    svg = d3.select(container).append("svg:svg").attr("width", w+"px").attr("height", h+"px").attr("id", "bar"+@genId)
    chart = new dimple.chart(svg, @dataInit)
    chart.setBounds(w*(50/600), h*(20/400), w *(540/600), h*(350/400))
    chart.addCategoryAxis("x", options.x)
    chart.addMeasureAxis("y", options.y)
    chart.addSeries(null, dimple.plot.bar)
    chart.draw()

    dataO.subscribe( (newData) =>
      chart.data = newData
      chart.draw(1000)
    )
)