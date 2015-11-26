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

      // move progress node to body, to enable absolute positioning
      var pies = d3.select("#progress-pies")
      $(pies.node()).appendTo("body");

      var svg = dimple.newSvg("#progress-pies", 100, 400);
      var myChart = new dimple.chart(svg, []);

      var xAxis = myChart.addPctAxis("x", "completed");
      xAxis.title = "% completed";
      xAxis.ticks = 2;
      xAxis.showGridlines = false;

      myChart.assignColor("Done", "#3a3", "#3a3", 1);
      myChart.assignColor("Pending", "#a33", "#a33", 1);

      var yAxis = myChart.addCategoryAxis("y", ["id", "name", "time"]);
      yAxis.hidden = true;
      yAxis.addOrderRule("id");

      myChart.addSeries("status", dimple.plot.bar);

      var olderProgresses = []

      progress.subscribe(function(jobsProgress) {
        // redraw only if changed
        var totalCompletions = function(ps) { return _.reduce(ps, function(memo, p){ return memo + p.completed; }, 0); };
        if (totalCompletions(olderProgresses) == totalCompletions(jobsProgress)) {
          return;
        }
        olderProgresses = jobsProgress;

        var processedData = _.flatten(_.map(jobsProgress, function(p){
          p.status = "Done";
          if (p.completed != 100){
            pPending = _.clone(p);
            pPending.status = "Pending";
            pPending.completed = 100 - p.completed;
            return [p, pPending]
          }
          return [p]
        }), true);

        myChart.data = processedData;
        myChart.draw();
      });
    });
  });
});