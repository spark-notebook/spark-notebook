define([
    'observable'
    'knockout'
    'd3'
    'dimple'
    'dynatable'
], (Observable, ko, d3, dimple, dynatable) ->
  (dataO, container, options) ->
    id = @genId
    data = @dataInit
    columns = options.headers

    cont = d3.select(container).append("div").attr("class", "table")

    draw = (columns, data) =>
      cont.html("")
      table = cont.append("table")
                .attr("style", "width: "+(options.width||600)+"px")
                .attr("id", "table"+id)
                .attr("class", "table table-bordered table-hover table-striped table-condensed")
      thead = table.append("thead")
      tbody = table.append("tbody")
      tr = thead.append("tr")

      #append the header row
      th = tr .selectAll("th")
              .data(columns)

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

      $(table).dynatable()

    draw(columns, data)

    dataO.subscribe( (newData) =>
      draw(columns, newData)
    )
)