define([
    'observable'
    'knockout'
    'd3'
    'underscore'
], (Observable, ko, d3, _) ->
  (dataO, container, options) ->
    w = options.width||600
    h = options.height||400

    svg = d3.select(container).append("svg:svg").attr("width", w+"px").attr("height", h+"px").attr("id", "graph"+@genId)

    #if we want goup field (int 1..20)
    #color = d3.scale.category20()

    graph = {
      links: [],
      nodes: []
    }

    force = d3.layout.force()
                .charge(options.charge||-30)
                .linkDistance(options.linkDistance||20)
                .linkStrength(options.linkStrength||1)
                .size([w, h])
                .nodes(graph.nodes)
                .links(graph.links)
                .on("tick", () ->
                    link.attr("x1", (d) -> d.source.x )
                        .attr("y1", (d) -> d.source.y )
                        .attr("x2", (d) -> d.target.x )
                        .attr("y2", (d) -> d.target.y )

                    node.attr("cx", (d) -> d.x )
                        .attr("cy", (d) -> d.y )
                  )

    link = svg.selectAll(".link")
    node = svg.selectAll(".node")

    updateData = (data) =>
      graph.nodes.length = 0
      graph.links.length = 0
      _.each(data, (d) ->
        if d.nodeId && !_.findWhere(graph.nodes,{nodeId: d.nodeId})
          graph.nodes.push(d)
      )
      _.each(data, (g) ->
        if g.edgeId && !_.findWhere(graph.links,{edgeId: g.edgeId})
          end1Id = g.end1Id
          end2Id = g.end2Id
          delete g.end1Id
          delete g.end2Id
          # could be very very optimized
          g.source = _.find(graph.nodes, (d) -> d.nodeId == end1Id || d.nodeId == end1Id)
          g.target = _.find(graph.nodes, (d) -> d.nodeId == end2Id || d.nodeId == end2Id)

          graph.links.push(g) if (g.source && g.target)
      )

    updateView = () =>
      link = link.data(graph.links)
      link.enter().append("line")
          #.attr("class", "link")
          .style("stroke", (d) -> d.color )
          .style("stroke-opacity", ".6")
          .style("stroke-width", 1) #1 could be replaced by a function
      link.exit().remove();

      node = node.data(graph.nodes)
      node.enter().append("circle")
          #.attr("class", "node")
          .attr("r", 5)
          .style("fill", (d) -> d.color )
          .style("stroke", "#fff")
          .style("stroke-width", "1.5px")
          .call(force.drag)

      node.append("title")
          .text((d) ->
            v = _.omit(d, ["index", "weight", "x", "y", "px", "py"])
            JSON.stringify(v)
          )

    update = (data) =>
      updateData(data)
      updateView()
      force.start()

    update(@dataInit)

    dataO.subscribe( (newData) =>
      update(newData)
    )
)