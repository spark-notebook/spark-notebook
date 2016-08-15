define([
    'jquery',
    'underscore',
    'plotly',
], ($, _, Plotly) ->
  (dataO, container, options) ->

    formatData = (dataPoints, dataSources, dataOptions) ->
        splitBy = dataOptions.splitBy
        delete dataOptions.splitBy
        data = if splitBy? then {} else {unk: {}}
        for point in dataPoints
            traceName = if splitBy? then point[splitBy] else "unk"
            if traceName not of data
                data[traceName] = {}
            trace = data[traceName]
            for field, source of dataSources
                if typeof source == "object"
                    if field not of trace
                        trace[field] = {}
                    for subfield, subsource of source
                        if subfield not of trace[field]
                            trace[field][subfield] = []
                        trace[field][subfield].push(point[subsource])
                else
                    if field not of trace
                        trace[field] = []
                    trace[field].push(point[source])
        # TODO add options per trace
        for traceName, trace of data
            if splitBy?
                trace['name'] = traceName
            for option, value of dataOptions#[traceName]
                if typeof value == "object"
                    trace[option] = {}
                    for suboption, subvalue of value
                        trace[option][suboption] = subvalue
                else
                    trace[option] = value

        (value for own prop, value of data)

    h = options.height||400

    chart_container = $("<div>").addClass("custom-plotly-chart").attr("height", h+"px")
    chart_container.attr("id", "custom-plotly-chart-"+@genId).appendTo(container)

    try
      eval(options.js) # should create `layout`,'dataOptions' and 'dataSources' vars
    catch error
      alert("Error when evaluating layout (see console for the error)")
      console.log(error)

    dataPoints = @dataInit

    plotlyData = formatData(dataPoints, dataSources, dataOptions)

    chart = Plotly.newPlot(chart_container.attr("id"),
                           plotlyData,
                           layout)
)
