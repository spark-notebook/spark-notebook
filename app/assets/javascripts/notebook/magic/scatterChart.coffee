define([
    'observable'
    'knockout'
    'd3'
    'c3'
], (Observable, ko, d3, c3) ->
  (dataO, container, options) ->
    w = options.width||600
    h = options.height||400

    chart_container = $("<div>").addClass("custom-c3-chart").attr("width", w+"px").attr("height", h+"px")
    chart_container.attr("id", "custom-c3-chart-"+@genId).appendTo(container)

    data = {
      type: "scatter"
    }

    prepareData = (data, ds) ->
      if options.g
        groups = _.unique(ds.map((d) -> d[options.g]))
        xs = {}
        cols = {}
        groups.forEach((g) =>
          xs[""+g] = ""+g+"_x"
          cols[""+g] = []
          cols[""+g+"_x"] = []
        )
        ds.forEach((d) =>
          cols[d[options.g]+"_x"].push(d[options.x])
          cols[d[options.g]].push(d[options.y])
        )
        data.columns = []
        _.each(cols, (c, k) =>
          c.unshift(k)
          data.columns.push(c)
        )
        data.xs = xs
      else
        xs = {}
        xs[options.x] = options.x
        data.x = options.x
        dataI = _.map(ds, (d) -> [d._1, d._2])
        dataI.unshift([options.x, options.y])
        data.rows = dataI
      data

    chart = c3.generate(
      bindto: "#" + chart_container.attr("id"),
      data: prepareData(data, @dataInit),
      axis: {
        x: {
          label: options.x,
          tick: {
              fit: false
          }
        },
        y: {
          label: options.y
        }
      }
    )

    dataO.subscribe( (newData) =>
      #chart.unload();
      chart.load(prepareData(data, newData));
    )
)