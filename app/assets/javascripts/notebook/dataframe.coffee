define([
    'observable'
    'knockout'
    'd3'
], (Observable, ko, d3) ->
  (canvas, extension) ->

    numPartitions = @numPartitions
    partitionIndexO = Observable.makeObservable(@partitionIndexId)
    dataO = Observable.makeObservableArray(@dataId)
    schema = @dfSchema

    d3.select(canvas)
      .append "div"
      .attr
        "class": "df-cmd-panel"
      .call (div) ->
        div.append "a"
        .attr
          "data-bind": "click: prevPartition, visibility: hasPrevPartition"
          "href": "#"
        .text "<<"
      .call (div) ->
          div.append "input"
          .attr
              "type": "text"
              "data-bind": "value: partitionIndex"
          .text "0"
      .call (div) ->
        div.append "a"
        .attr
            "data-bind": "click: nextPartition, visibility: hasNextPartition"
            "href": "#"
        .text ">>"

    # configure table
    dt = d3.select(canvas)
      .append "div"
      .attr
        class: "df-table-panel"
      .append("table")
      .attr
        class: "df-table"

    dt.selectAll('col')
    .data(schema.fields).enter()
    .append('col')
    .attr
      class: (d, i) -> d.type

    dt.append('thead').append('tr')
      .selectAll('th')
      .data(schema.fields).enter()
      .append('th')
      .attr
          class: (d, i) -> d.type
      .text((d, i) -> d.name)

    dt.append('tbody')

    onData = (schema, data) ->
      dt
        .select('tbody')
        .selectAll('tr')
        .data(data)
        .call (tr) -> tr.enter().append('tr')
        .call (tr) ->
          tr.selectAll('td')
          .data((row, i) ->
            return schema.fields.map((c) ->
                cell = {
                  'cl': c.type
                  'value': row[c.name]
                }
                return cell
            )
          )
          .call (td) ->
            td.enter()
            .append('td')
            .attr
              class: (cell, i) -> cell.cl
          .call (td) ->
            td.text((cell, i) -> cell.value)
        .call (tr) -> tr.exit().remove()

      dt.classed("disabled", false)

    dataO.subscribe( (data) =>
      onData(schema, data)
    )

    partitionIndexO.subscribe( (i) =>
      dt.classed("disabled", true)
    )

    # configure paging controls
    partitionIndex = ko.pureComputed(
      read: -> partitionIndexO() + 1
      write: (value) ->
        newIndex = (if typeof value == 'string' then parseInt(value) else value) - 1
        if newIndex in [0...numPartitions]
          partitionIndexO(newIndex)
    )

    ko.bindingHandlers.visibility = {
      update: (element, valueAccessor) ->
        visible = ko.utils.unwrapObservable(valueAccessor())
        d3.select(element).style('visibility', if visible then 'visible' else 'hidden')
    }

    ko.applyBindings({
      hasNextPartition: ko.computed(-> partitionIndexO() < numPartitions - 1)
      nextPartition:  -> partitionIndex(partitionIndex() + 1)
      hasPrevPartition: ko.computed(-> partitionIndexO() > 0)
      prevPartition:  -> partitionIndexO(partitionIndexO() - 1)
      partitionIndex: partitionIndex
    }, canvas)

    partitionIndexO(0)

    extension(@, dt)
)
