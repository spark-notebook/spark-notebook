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

      var updateSparkUiLink = function(sparkUi){
        if (sparkUi != "") {
          var sparkUiLink = $("<a href='" + sparkUi + "' id='spark-ui-link' target='_blank'>open SparkUI</a>")
          $("#spark-ui-link-container").html(sparkUiLink);
        }
      };

      var allJobsProgressBar = $('\
          <div class="progress">\
            <div id="jobs-progress-done" class="progress-bar progress-bar-success" role="progressbar" aria-valuenow="" \
                 aria-valuemin="0" aria-valuemax="100">\
            </div>\
            <div id="jobs-progress-pending" class="progress-bar progress-bar-warning" role="progressbar" aria-valuenow="" \
                 aria-valuemin="0" aria-valuemax="100">\
            </div>\
        </div>');
      $('#all-jobs-progress-bar').html(allJobsProgressBar);

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

        var perCellId = _.groupBy(jobsProgress, 'cell_id');
        _.each(perCellId, function(jobs, cell_id){
          var completedTasks = sum(_.pluck(jobs, 'completed_tasks'));
          var totalTasks = sum(_.pluck(jobs, 'total_tasks'));
          var cellProgress = Math.min(Math.floor(completedTasks * 100.0 / totalTasks), 100);
          if (cell_id) {
            var cell = $(cells[cell_id]).data("cell");
            var cellProgressBar = $(cells[cell_id]).find('div.progress-bar');
            cellProgressBar.css("width", Math.max(cellProgress, 5) + "%");
            if (cellProgress == 100) {
              cellProgressBar.removeClass("active").removeClass("progress-bar-striped");
              if (cell) cell.hideCancelCellBtn();
            } else {
              cellProgressBar.addClass("active").addClass("progress-bar-striped");
              if (cell) cell.addCancelCellBtn();
            }
          }
        });

        // update all jobs progress bar
        var completedTasks = sum(_.pluck(jobsProgress, 'completed_tasks'));
        var totalTasks = sum(_.pluck(jobsProgress, 'total_tasks'));
        var totalProgressPercent = Math.floor(100.0 * completedTasks / Math.max(totalTasks, 1))

        $('#jobs-progress-done').css('width', totalProgressPercent + '%');
        $('#jobs-progress-pending').css('width', (100 - totalProgressPercent) + '%');
      });
    });
  });
});