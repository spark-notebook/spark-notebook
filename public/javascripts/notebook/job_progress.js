define([
  'jquery',
  'base/js/events',
  'knockout',
  'underscore',
  'd3',
  'dimple'
], function($, events, ko, _, d3, dimple) {
  events.on('Observable.ready', function(){
    require(['observable'], function(O) {
      var progress = O.makeObservableArray("jobsProgress");

      var pies = d3.select("#progress-pies")
      $(pies.node()).appendTo("body");
      var charts = [];
      var completed = [];
      progress.subscribe(function(ps) {
        console.log("Progress:", ps);

        var alreadyCompleted = _.intersection(completed, _.pluck(ps, "id"));
        ps = _.filter(ps, function(p) { return !_.contains(alreadyCompleted, p.id); });

        _.each(ps, function(p) { if (p.completed == 100) {completed.push(p.id); }});

        var w = 180;
        var h = w*0.75;

        var svgs = pies.selectAll("svg")
                        .data(ps, function(p){ return p.id; });

        var ex = svgs.exit();
        ex.each(function(p) {
          charts = _.reject(charts, function(c) { return c.id == p.id; });
        });
        ex.remove();

        var gsvg = svgs.enter()
                        .append("svg:svg")
                          .attr("width", w+"px")
                          .attr("height", h+"px")
                          .attr("name", function(p) { return p.name; })
                        .append("g")
        gsvg.append("title")
            .text(function(p) { return p.name; })
        gsvg
            .each(function(p) {
              d3.select(this)
                .append("text")
                  .attr("x", w*(250/600))
                  .attr("y", h*(25/400))
                  .style("text-anchor", "middle")
                  .style("font-family", "sans-serif")
                  .style("font-weight", "bold")
                  .text((p.name.length > 15)?p.name.substring(0, 15)+" [...]":p.name);
              d3.select(this)
                .append("text")
                  .attr("x", w*(250/600))
                  .attr("y", h*(50/400))
                  .style("text-anchor", "middle")
                  .style("font-family", "sans-serif")
                  .style("font-weight", "bold")
                  .attr("class", "extra")
                  .text(p.id + " [" + p.time + "]");

              var data = [ {"cl": "completed", "pc" : 0 }, {"cl":"pending", "pc": 0 } ];
              var chart = new dimple.chart(
                d3.select(this.parentNode /*get svg instead of g*/),
                data
              );
              chart.setBounds(w*(10/600), h*(50/400), w *(380/600), h*(360/400));

              chart.addMeasureAxis("p", "pc");
              chart.addSeries("cl", dimple.plot.pie);
              chart.addLegend(w*(400 /600), h*(50/400), w*(60/600), h*(350/400), "left");
              charts.push({id: p.id, chart: chart});
            });

        _.each(ps, function(p) {
          var chart = _.findWhere(charts, {id: p.id});
          if (chart) {
            d3.selectAll(chart.chart.svg[0])
                .select("text.extra")
                .attr("x", w*(250/600))
                .attr("y", h*(50/400))
                .style("text-anchor", "middle")
                .style("font-family", "sans-serif")
                .style("font-weight", "bold")
                .text(p.id + " [" + p.time + "]");

            var data = [ {"cl": "completed", "pc" : p.completed }, {"cl":"pending", "pc": (100-p.completed) } ];
            chart.chart.data = data;
            chart.chart.draw(1000);
          } else {
            console.warn("Progress: Pie chart not found", p);
          }
        });
        console.log("charts", charts);
      });
    });
  });
});