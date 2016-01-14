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
      self.clearDefinitions = function() {
        self.definitions.data.remove(function(e) { return true });
      };
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
      '            <td data-bind="text: type, click: $parent.hightlightCell"></td>'+
      '        </tr>'+
      '    </tbody>'+
      '</table>')

    ko.applyBindings(model, tbl.get(0));
    td.append(tbl);

    events.on('kernel_restarting.Kernel', function(e, c) {
      // clear sidebar on kernel restart
      // reset term definitions
      model.clearDefinitions();
      // clear old progressbar & link to spark UI
      $('#spark-ui-link-container, #all-jobs-progress-bar').html('');
    });

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
