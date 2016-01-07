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
      $("#progress-pies").append($("<div id='progress-bars'/>")).append("<div id='spark-ui-link' />");

      var updateSparkUiLink = function(sparkUi){
        if (sparkUi != "") {
          var sparkUiLink = $("<a href='" + sparkUi + "' id='spark-ui-link' target='_blank'>open SparkUI</a>")
          $("#spark-ui-link-container").html(sparkUiLink);
        }
      };

      // setup the progress chart
      var svg = dimple.newSvg("#progress-bars", 275, 175); // todo resize with panel → ref to panel needed and event from panel to be listened
      var myChart = new dimple.chart(svg, []);
      var xAxis = myChart.addPctAxis("x", "percentage");
      xAxis.title = "% completed";
      xAxis.ticks = 2;
      xAxis.showGridlines = false;
      myChart.assignColor("Done", "#3a3", "#3a3", 1);
      myChart.assignColor("Pending", "#a33", "#a33", 1);
      var yAxis = myChart.addCategoryAxis("y", ["fake"]);
      yAxis.hidden = true;
      myChart.addSeries("status", dimple.plot.bar);

      // process the progresses and update the chart
      var isCompleted = function(p){ return p.completed == 100 };
      var sum = function(items){ return _.reduce(items, function(memo, p){ return memo + p} , 0) };

      var olderProgresses = [];

      function findCells(id) {
        var f = ".cell[data-cell-id"+(id?"='"+id+"'":"")+"]";
        var cells = $(IPython.notebook.element).find(f);
        return _.object(_.map(cells, function(cell) {return [$(cell).data("cell-id"), cell]}));
      }

      progress.subscribe(function(status) {
        var cells = findCells();

        var jobsProgress = status.jobsStatus;
        var sparkUi = status.sparkUi;

        updateSparkUiLink(sparkUi);

        // redraw only if changed
        var totalCompletions = function(ps) { return sum(_.pluck(ps, 'completed')); };
        if (totalCompletions(olderProgresses) == totalCompletions(jobsProgress)) {
          return;
        }
        olderProgresses = jobsProgress;

        var completedTasks = sum(_.pluck(jobsProgress, 'completed_tasks'));
        var totalTasks = sum(_.pluck(jobsProgress, 'total_tasks'));

        // collapse completed and running jobs into one bar
        var completedJobsInfo = {
          status: "Done",
          percentage: completedTasks,
          fake: ""
        };
        var runningJobsInfo = {
          status: "Pending",
          percentage: totalTasks - completedTasks,
          fake: ""
        };

        var perCellId = _.groupBy(jobsProgress, 'cell_id');

        _.each(perCellId, function(jobs, cell_id){
          var completedTasks = sum(_.pluck(jobs, 'completed_tasks'));
          var totalTasks = sum(_.pluck(jobs, 'total_tasks'));
          var cellProgress = Math.min(Math.floor(completedTasks * 100.0 / totalTasks), 100);
          if (cell_id) {
            var cellProgressBar = $(cells[cell_id]).find('.cell-progress-bar');
            cellProgressBar.css("width", Math.max(cellProgress, 5) + "%");
            if (cellProgress == 100) {
              cellProgressBar.addClass("completed")
            } else {
              cellProgressBar.removeClass("completed")
            }
          }
        });

        // redraw chart as last step, in case dimple didn't like some data :)
        myChart.data = _.flatten([runningJobsInfo, completedJobsInfo]);
        myChart.draw();
      });
    });
  });
});