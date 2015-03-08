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

require([
    'base/js/namespace',
    'base/js/events',
    "jquery",
    //"widgets/js/manager",
    //"widgets/js/widget",
    //"widgets/js/widget_output"
], function(IPython, events, $/*, manager, widget, output*/) {
      events.on('kernel_ready.Kernel', function(event, content){
      console.log("Creating OutputView for notebook-bg-logs-panel");
      var kernel = content.kernel;
      kernel.widget_manager.create_model({
        model_name: 'WidgetModel',
        widget_class: 'IPython.OutputArea'//'IPython.html.widgets.widget_output.OutputView'
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
              view.output_area.clear_output(false /*wait*/);
              kernel.execute('SparkNotebookBgLog', {
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
            //setInterval(connect, 5000);
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