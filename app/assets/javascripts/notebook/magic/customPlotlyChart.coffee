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
        optionsByTrace = dataOptions.byTrace
        delete dataOptions.byTrace
        for traceName, trace of data
            if splitBy?
                trace['name'] = traceName
            if optionsByTrace? and optionsByTrace[traceName]?
                options = optionsByTrace[traceName]
            else
                options = dataOptions
            for option, value of options
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

    c_options = options

    plot = (data) =>
        # we re-eval the js because the newPlot function has side effects -_-"
        try
          eval(c_options.js) # should create `layout`,'dataOptions' and 'dataSources' vars
        catch error
          alert("Error when evaluating layout (see console for the error)")
          console.log(error)

        plotlyData = formatData(data, dataSources, dataOptions)

        chart = Plotly.newPlot(chart_container.attr("id"),
                               plotlyData,
                               layout,
                                {displaylogo: false})

    plot(@dataInit)

    dataO.subscribe(plot)

)
