define([
    'observable'
    'knockout'
    'd3'
    'dimple'
], (Observable, ko, d3, dimple) ->
  (dataO, container, options) ->
    w = options.width||600
    h = options.height||400

    svg = d3.select(container).append("svg:svg").attr("width", w+"px").attr("height", h+"px").attr("id", "scatter"+@genId)
    chart = new dimple.chart(svg, @dataInit)
    chart.setBounds(w*(50/600), h*(20/400), w *(540/600), h*(350/400))

    #<painful> → https://github.com/PMSI-AlignAlytics/dimple/issues/140
    order = []
    n = @dataInit.length-1
    for i in [0..n]
      order.push(@dataInit[i][options.x]) if (order.indexOf(@dataInit[i][options.x]) == -1)
    x = chart.addCategoryAxis("x", options.x)
    x.addOrderRule(order)
    #</painful>
    y = chart.addMeasureAxis("y", options.y)

    chart.addSeries([options.y], dimple.plot.bubble)
    chart.draw()

    dataO.subscribe( (newData) =>
      chart.data = newData
      chart.draw(1000)
    )
)