define([
    'observable'
    'knockout'
    'd3'
], (Observable, ko, d3) ->
  (elem, extension) ->

    numPartitions = @numPartitions
    partitionIndexO = Observable.makeObservable(@partitionIndexId)
    dataO = Observable.makeObservableArray(@dataId)
    schema = @dfSchema

    # configure table
    dt = d3.select(elem).select("table")

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

    dataO.subscribe( (data) =>
      onData(schema, data)
    )

    # configure paging controls
    nextPartition = (btn) ->
      if partitionIndexO() + 1 in [0...numPartitions]
        console.log("change to partition #{ partitionIndexO() + 1 }")
        partitionIndexO(partitionIndexO() + 1)
    prevPartition = (btn) ->
      if partitionIndexO() - 1 in [0...numPartitions]
        console.log("change to partition #{ partitionIndexO() - 1 }")
        partitionIndexO(partitionIndexO() - 1)

    ko.applyBindings({
      nextPartition:  nextPartition
      prevPartition:  prevPartition
    }, elem)

    partitionIndexO(0)

    extension(@, dt)
)
