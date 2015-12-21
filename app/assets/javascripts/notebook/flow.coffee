define([
    'observable'
    'joint'
    'jquery'
    'd3'
    'underscore'
    'base/js/events'
], (Observable, joint, $, d3, _, events) ->
  (dataO, container, options) ->
    gId = @genId

    graph = new joint.dia.Graph

    paper = new joint.dia.Paper({
        el: $(container).find(".jointgraph"),
        width: 600,
        height: 200,
        model: graph,
        gridSize: 1
    })

    grect = (n, options) ->
      options = options || {}
      new joint.shapes.devs.Coupled({
          position: options.position || { x: 100, y: 30 },
          size: options.size || { width: 100, height: 30 },
          inPorts: options.inPorts || ['in'],
          outPorts: options.outPorts || ['out'],
          attrs: {
            '.label': {
              text: options.name || 'Element'
            }
          }
      })


    glink = (options) ->
      new joint.dia.Link({
        source: if (options.source.id)
                  options.source
                else
                  {x: 120, y: 50}
        ,
        target: if (options.target.id)
                  options.target
                else
                  {x: 220, y: 50}
      })

    # get cell object, like done in get_cell_elements in notebook.js
    cell_dom_element = $(container).parents(".cell").not('.cell .cell')[0]
    snb_cell = $(cell_dom_element).data("cell")

    html_output = (v) =>
      output = _.find(
        snb_cell.output_area.outputs,
        (o) => o.data && o.data["text/html"]
      )
      if (v)
        output.data["text/html"] = v
      else
        output.data["text/html"]

    conf = $(container).find(".configuration")
    form = $(container).find("form.configure")

    form.on("submit", (e) =>
      e.preventDefault()
      e.stopImmediatePropagation()

      newConf = form.serializeArray()
      pipeComponent = form.cell.pipeComponent
      parameters = {}
      _.each(newConf, (e) -> parameters[e.name] = e.value)
      pipeComponent.parameters = parameters
      dataO([pipeComponent])
    )
    form.find("button.remove").on("click", (e) =>
      e.preventDefault()
      e.stopImmediatePropagation()
      pipeComponent = form.cell.pipeComponent
      form.cell.remove()
      dataO([_.extend(pipeComponent, {'remove': true})])
    )
    paper.on("cell:pointerdblclick", (cellView, evt, x, y) =>
      if (! (cellView.model instanceof joint.dia.Link) )
        cell = cellView.model
        form.cell = cell
        conf.html("")
        _.each(cell.pipeComponent.parameters, (v,k) ->
          d = $("<div class='form-group'></div>")
          l = $("<label>"+k+"</label>")
          i = $("<input type='text'class='form-control' name='"+k+"'/>")
          i.val(v)
          d.append(l)
          d.append(i)
          conf.append(d)
        )
    )

    # TODO → this is not working (aims to save the last flow in the cell's output)
    save = (e) =>
      cells = graph.getCells()
      links = graph.getLinks()
      cellsLinks =  _.union(cells, links)
      d = _.map(cellsLinks, (cl) =>
        o = _.clone(cl.pipeComponent)
        o.position = cl.attributes.position
        o.size = cl.attributes.size
        o
      )
      o = $(html_output())
      sc = o.find("script")[0]
      dt = $(sc).attr("data-this")
      j = JSON.parse(dt)
      j.dataInit = d
      s = JSON.stringify(j)
      $(sc).attr("data-this", s)
      html_output(o.prop('outerHTML'))

    graph.on("change", save)

    onData = (newData) =>
      cells = graph.getCells()
      links = graph.getLinks()
      cellsLinks =  _.union(cells, links)

      _.each(newData,
        (d) -> if (_.isUndefined(d.remove))
          d.remove = false
      )
      toAdd = _.filter(newData, (d) => !d.remove && !_.contains(_.pluck(cellsLinks, "id"), d.id))

      _.each(newData, (u) ->
        c = _.findWhere(cellsLinks, {id: u.id})
        if (c)
          c.pipeComponent = u
      )

      addCells = _.map(toAdd, (d) =>
        if (d.tpe == "box")
          r = grect(d.name, d)
          r.id = d.id
          r.pipeComponent = d
          r
        else
          l = glink({
                      source: { id: d.parameters.source_id, port: d.parameters.source_port },
                      target: { id: d.parameters.target_id, port: d.parameters.target_port }
                    })
          l.id = d.id
          l.pipeComponent = d
          l.on("remove", (l) => dataO([_.extend(l.pipeComponent, {'remove': true})]))
          l.on("change:source", (l, c) =>
            if (l.get("source").id && l.get("source").id != l.pipeComponent.parameters.source)
              l.pipeComponent.parameters.source_id   = l.get("source").id
              l.pipeComponent.parameters.source_port = l.get("source").port
              dataO([l.pipeComponent])
            else
              if (l.pipeComponent.parameters.source)
                delete l.pipeComponent.parameters.source_id
                delete l.pipeComponent.parameters.source_port
                dataO([l.pipeComponent])
          )
          l.on("change:target", (l, c) =>
            if (l.get("target").id && l.get("target").id != l.pipeComponent.parameters.target)
              l.pipeComponent.parameters.target_id   = l.get("target").id
              l.pipeComponent.parameters.target_port = l.get("target").port
              dataO([l.pipeComponent])
            else
              if (l.pipeComponent.parameters.target)
                delete l.pipeComponent.parameters.target_id
                delete l.pipeComponent.parameters.target_port
                dataO([l.pipeComponent])
          )
          l
      )
      graph.addCells(addCells)


    onData(@dataInit)
    dataO(@dataInit)

    dataO.subscribe(onData)
)