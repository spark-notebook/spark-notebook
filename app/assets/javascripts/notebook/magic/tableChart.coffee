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

    table = d3.select(container).append("table")
              .attr("style", "width: "+(options.width||600)+"px")
              .attr("id", "table"+id)
    thead = table.append("thead")
    tbody = table.append("tbody")
    tr = thead.append("tr")
    draw = (columns, data) =>

      #append the header row
      th = tr .selectAll("th")
              .data(columns)
      th.exit().remove()
      th.enter()
          .append("th")
            .text( (column) -> column )

      #create a row for each object in the data
      rows = tbody.selectAll("tr")
                  .data(data)
      rows.exit().remove()

      rows = rows.enter().append("tr")

      #create a cell in each row for each column
      cells = rows.selectAll("td")
                  .data( (row) ->
                    columns.map( (column) ->
                      { column: column, value: row[column] }
                    )
                  )
      cells.exit().remove()
      cells.enter()
            .append("td")
            .attr("style", "font-family: Courier") #sets the font style
              .html( (d) -> d.value )

    draw(columns, data)
    dataO.subscribe( (newData) =>
      draw(columns, newData)
    )
)