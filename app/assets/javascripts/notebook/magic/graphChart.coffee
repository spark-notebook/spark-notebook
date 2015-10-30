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

    force = d3.layout.force()
                .charge(options.charge||-30)
                .linkDistance(options.linkDistance||20)
                .linkStrength(options.linkStrength||1)
                .size([w, h])


    graph = {
      links: [],
      nodes: []
    }

    _.each(@dataInit, (d) -> graph.nodes.push(d) if d.nodeId != undefined)
    _.each(@dataInit, (g) ->
      if (g.edgeId)
        end1Id = g.end1Id
        end2Id = g.end2Id
        delete g.end1Id
        delete g.end2Id
        # could be very very optimized
        g.source = _.find(graph.nodes, (d) -> d.nodeId == end1Id || d.nodeId == end1Id)
        g.target = _.find(graph.nodes, (d) -> d.nodeId == end2Id || d.nodeId == end2Id)
        graph.links.push(g)
    )

    force
        .nodes(graph.nodes)
        .links(graph.links)
        .start()

    link = svg.selectAll(".link")
                .data(graph.links)
              .enter().append("line")
                #.attr("class", "link")
                .style("stroke", (d) -> d.color )
                .style("stroke-opacity", ".6")
                .style("stroke-width", 1) #1 could be replaced by a function

    node = svg.selectAll(".node")
                .data(graph.nodes)
              .enter().append("circle")
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

    force.on("tick", () ->
      link.attr("x1", (d) -> d.source.x )
          .attr("y1", (d) -> d.source.y )
          .attr("x2", (d) -> d.target.x )
          .attr("y2", (d) -> d.target.y )

      node.attr("cx", (d) -> d.x )
          .attr("cy", (d) -> d.y )
    )

    dataO.subscribe( (newData) =>
      console.warn("graph update todo")
    )
)