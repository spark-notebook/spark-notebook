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

      // setup the progress chart
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

      // process the progresses and update the chart
      var isCompleted = function(p){ return p.completed == 100 };
      var sum = function(items){ return _.reduce(items, function(memo, p){ return memo + p} , 0) };

      var olderProgresses = [];
      progress.subscribe(function(jobsProgress) {
        // redraw only if changed
        var totalCompletions = function(ps) { return sum(_.pluck(ps, 'completed')); };
        if (totalCompletions(olderProgresses) == totalCompletions(jobsProgress)) {
          return;
        }
        olderProgresses = jobsProgress;

        // collapse completed jobs into one bar
        var nCompletedJobs = _.filter(jobsProgress, isCompleted).length;
        var completedJobsInfo = {
          status: "Done",
          completed: 100,
          name: nCompletedJobs + " stages",
          time: "N/A",
          id: 0
        };

        var runningJobs = _.map(_.reject(jobsProgress, isCompleted));
        var runningJobsInfo = _.flatten(_.map(runningJobs, function(p){
          p.status = "Done";
          // part of stage that's still pending
          pPending = _.clone(p);
          pPending.status = "Pending";
          pPending.completed = 100 - p.completed;
          return [p, pPending]
        }), true);

        myChart.data = _.flatten([runningJobsInfo, completedJobsInfo]);
        myChart.draw();
      });
    });
  });
});