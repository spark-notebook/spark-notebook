define([
    'observable'
    'knockout'
    'd3'
    'dimple'
], (Observable, ko, d3, dimple) ->
  (dataO, container, options) ->
    id = @genId
    data = @dataInit
    columns = options.headers

    count = d3.select(container).append("p")
    count .selectAll("span .nrow")
          .data([options.nrow])
          .enter()
          .append("span")
          .attr("class", "nrow")
            .text( (d) ->
              d + " items"
            )
    count .selectAll("span .shown")
          .data([options.shown])
          .enter()
          .append("span")
          .attr("class", "shown")
          .attr("style", "color: red")
            .text( (d) ->
              console.log("d is " + d)
              if options.nrow != d
                " (Only " +  d + " first items are shown)"
              else
                ""
            )

    table = d3.select(container).append("table")
              .attr("style", "width: "+(options.width||600)+"px")
              .attr("id", "table"+id)
    thead = table.append("thead")
    tbody = table.append("tbody")

    #append the header row
    thead.append("tr")
        .selectAll("th")
        .data(columns)
        .enter()
        .append("th")
          .text( (column) -> column )

    #create a row for each object in the data
    rows = tbody.selectAll("tr")
                .data(data)
                .enter()
                .append("tr")

    #create a cell in each row for each column
    cells = rows.selectAll("td")
                .data( (row) ->
                  columns.map( (column) ->
                    { column: column, value: row[column] }
                  )
                )
                .enter()
                .append("td")
                .attr("style", "font-family: Courier") #sets the font style
                  .html( (d) -> d.value )

    dataO.subscribe( (newData) =>
      console.log("tableChart: todo update")
      console.log("tableChart new data", newData)
    )
)