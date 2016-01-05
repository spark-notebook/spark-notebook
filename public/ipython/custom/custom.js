// leave at least 2 line with only a star on it below, or doc generation fails
/**
 *
 *
 * Placeholder for custom user javascript
 * mainly to be overridden in profile/static/custom/custom.js
 * This will always be an empty file in IPython
 *
 * User could add any javascript in the `profile/static/custom/custom.js` file
 * (and should create it if it does not exist).
 * It will be executed by the ipython notebook at load time.
 *
 * Same thing with `profile/static/custom/custom.css` to inject custom css into the notebook.
 *
 * Classes and functions are available at load time and may be accessed plainly:
 *
 *     IPython.Cell.options_default.cm_config.extraKeys['Home'] = 'goLineLeft';
 *     IPython.Cell.options_default.cm_config.extraKeys['End'] = 'goLineRight';
 *
 * Instances are created later however and must be accessed using events:
 *     require([
 *        'base/js/namespace',
 *        'base/js/events'
 *     ], function(IPython, events) {
 *         events.on("app_initialized.NotebookApp", function () {
 *             IPython.keyboard_manager....
 *         });
 *     });
 *
 * __Example 1:__
 *
 * Create a custom button in toolbar that execute `%qtconsole` in kernel
 * and hence open a qtconsole attached to the same kernel as the current notebook
 *
 *    require([
 *        'base/js/namespace',
 *        'base/js/events'
 *    ], function(IPython, events) {
 *        events.on('app_initialized.NotebookApp', function(){
 *            IPython.toolbar.add_buttons_group([
 *                {
 *                    'label'   : 'run qtconsole',
 *                    'icon'    : 'icon-terminal', // select your icon from http://fortawesome.github.io/Font-Awesome/icons
 *                    'callback': function () {
 *                        IPython.notebook.kernel.execute('%qtconsole')
 *                    }
 *                }
 *                // add more button here if needed.
 *                ]);
 *        });
 *    });
 *
 * __Example 2:__
 *
 * At the completion of the dashboard loading, load an unofficial javascript extension
 * that is installed in profile/static/custom/
 *
 *    require([
 *        'base/js/events'
 *    ], function(events) {
 *        events.on('app_initialized.DashboardApp', function(){
 *            require(['custom/unofficial_extension.js'])
 *        });
 *    });
 *
 * __Example 3:__
 *
 *  Use `jQuery.getScript(url [, success(script, textStatus, jqXHR)] );`
 *  to load custom script into the notebook.
 *
 *    // to load the metadata ui extension example.
 *    $.getScript('/static/notebook/js/celltoolbarpresets/example.js');
 *    // or
 *    // to load the metadata ui extension to control slideshow mode / reveal js for nbconvert
 *    $.getScript('/static/notebook/js/celltoolbarpresets/slideshow.js');
 *
 *
 * @module IPython
 * @namespace IPython
 * @class customjs
 * @static
 */


/**

COMMENT BECAUSE spark seems wanting to package the logger all the time
which makes cluster work failing all the time


require([
  'base/js/namespace',
  'base/js/events',
  "jquery",
  "tachyon"
], function(IPython, events, $, tachyon) {
  events.on('kernel_ready.Kernel', function(event, content){
    console.log("Creating OutputView for notebook-bg-logs-panel");
    var kernel = content.kernel;
    kernel.widget_manager.create_model({
      model_name: 'WidgetModel',
      widget_class: 'IPython.OutputArea'
    }).then(
      function(model) {
        console.log('Create model success!', model);

        model.set("_view_name", "OutputView");
        model.set("_view_module", "widgets/js/widget_output");

        var view = kernel.widget_manager.create_view(model, {});
        console.log('Create view success!', view);

        view.then(function(view) {
          $("#notebook-bg-logs-panel").append(view.$el);
          function connect() {
            view.output_area.clear_output(false); // false → not wait
            kernel.execute('@transient val __avoid_non_ser_excep = SparkNotebookBgLog', {
              iopub : {
                output : function() {
                  var msg = arguments[0];
                  if (msg.header.msg_type === 'stream') {
                    //nothing
                    // so that the output stream is not shown, only the result
                    console.log(msg);
                  } else {
                    view.output_area.handle_output.apply(view.output_area, arguments);
                  }
                },
                clear_output : function() {
                  view.output_area.handle_clear_output.apply(view.output_area, arguments);
                }
            },
            });
          };
          connect();
          $("#notebook-bg-logs-button").on("click", function() {
            connect();
          });
        });

      },
      $.proxy(console.error, console)
    );
  });
});

*/

require([
    'base/js/namespace',
    'base/js/events'
], function (IPython, events) {
    events.on("app_initialized.NotebookApp", function () {
        var isReportMode = $("body[data-presentation='report']").length > 0;
        if (isReportMode) {
            // make all cells fluid, and use full width
            $("#notebook-container").removeClass("container").addClass("container-fluid");
        }
    });
    events.on("kernel_ready.Kernel", function(){
        // recompute the notebook if 'action=recompute_now' is in URL
        var url = window.location.href;
        if (url.indexOf("recompute_now") != -1) {
            console.log("Now running all cells in notebook (recompute_now)");
            IPython.notebook.execute_all_cells();
        }
    });
});

require(["jquery", "jquery.gridster"], function($, gridster) {
  var g = $("#notebook-panels");
  var scrollWidth = 15;
  var w = ($(document.body).width() - scrollWidth) * (3.0 / 12);
  var m = 10;
  var nbCols = 2;
  var b = (w - (2 * nbCols)*10)/2

  var ul = $("#notebook-panels .gridster > ul");

  var site = $("#site");
  var nb = $("#notebook");
  var sb = $("#sidebar");
  var hh = $("#header").height();
  function sticky_relocate() {
    if (nb.is(":visible")) {
      var t = nb.offset().top;
      if (t < 0) {
        sb.css("top", (Math.abs(t)+hh)+"px");
      } else if (t < hh) {
        sb.css("top", (hh-t)+"px");
      } else {
        sb.css("top", "20px");
      }
    }
  }

  site.scroll(sticky_relocate);

  ul.gridster({
    max_cols: nbCols,
    widget_margins: [m, m],
    widget_base_dimensions: [b, 250],
    max_sizex: 2,
    helper: 'clone',
    resize: {
      enabled: true
    }
  });
});


require(["jquery", "underscore", "base/js/events", "knockout"], function($, _, events, ko) {
  // http://lions-mark.com/jquery/scrollTo/
  $.fn.scrollTo = function( target, options, callback ){
      if(typeof options == 'function' && arguments.length == 2){ callback = options; options = target; }
      var settings = $.extend({
          scrollTarget  : target,
          offsetTop     : 50,
          duration      : 500,
          easing        : 'swing'
      }, options);
      return this.each(function(){
          var scrollPane = $(this);
          var scrollTarget = (typeof settings.scrollTarget == "number") ? settings.scrollTarget : $(settings.scrollTarget);
          var scrollY = (typeof scrollTarget == "number") ? scrollTarget : scrollTarget.offset().top + scrollPane.scrollTop() - parseInt(settings.offsetTop);
          scrollPane.animate({scrollTop : scrollY }, parseInt(settings.duration), settings.easing, function(){
              if (typeof callback == 'function') { callback.call(this); }
          });
      });
  };

  var td = $("#termDefinitions");
  if (!td.find("table").length) {
    function viewModel() {
      var self = this;
      self.definitions = {};
      self.definitions.data = ko.observableArray([]);
      self.addDefinition = function(def) {
        self.definitions.data.remove(function(e) { return e.name == def.name;});
        self.definitions.data.push(def);
      };
      self.hightlightCell = function() {
        var cell = $("div.cell[data-cell-id='"+this.cell+"']");
        cell.addClass("alert alert-info");

        $('#site').scrollTo(cell, { duration: 300, offsetTop: 200 });

        setTimeout(function() {
          cell.removeClass("alert alert-info")
        }, 800);
      };
    };
    var model = new viewModel();

    var tbl = $('<table style="width: 100%;" class="table table-bordered table-hover">'+
              '    <thead>'+
              '        <tr><th>Name</th><th>Type</th></tr>'+
              '    </thead>'+
              '    <tbody data-bind="foreach: definitions.data">'+
              '        <tr>'+
              '            <td data-bind="text: name, click: $parent.hightlightCell"></td>'+
              '            <td data-bind="text: type"></td>'+
              '        </tr>'+
              '    </tbody>'+
              '</table>')

    ko.applyBindings(model, tbl.get(0));
    td.append(tbl);

    events.on('kernel_ready.Kernel', function(e, c) {
      var kernel = c.kernel;
      console.log("kernel", kernel);
      kernel.events.on("new.Definition", function(e, c) {
        console.log("new def", c);
        if (c.term || c.type) {
          model.addDefinition({name: c.term || c.type, type: c.tpe, cell: c.cell});
        }
      });
    });
  }
});