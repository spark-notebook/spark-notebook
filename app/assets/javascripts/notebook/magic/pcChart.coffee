define([
    'observable'
    'knockout'
    'underscore'
    'd3'
    'parcoords'
], (Observable, ko, _, d3, parcoords) ->
  (dataO, container, options) ->
    w = options.width||600
    h = options.height||400

    chart_container = $("<div>").addClass("parcoords").css("width", w+"px").css("height", h+"px")
    thisId = "pc-"+@genId
    chart_container.attr("id", thisId).appendTo(container)

    data = []

    _.map(@dataInit,
      (o) -> data.push(_.values(o))
    )

    data.unshift(options.headers)

    pc = d3.parcoords()("#"+thisId)
      .data(@dataInit)
      .composite("darker")
      .render()
      .createAxes()
      .shadows()
      .reorderable()
      .brushMode("1D-axes");

    dataO.subscribe( (newData) =>
    )
)