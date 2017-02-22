require(["jquery", "jquery.gridster", "jquerysticky"], function($, gridster, jquerysticky) {
  $( document ).ready(function() {
    $("#menubar-container").stick_in_parent({
      parent: "body",
      bottoming: false,
    });

    $("#notebook-panels").stick_in_parent({
        parent: "body",
        bottoming: false,
        // used to position the sidebar below the menu
        // FIXME: we shouldnt hardcode 33px, but this returns too-low height if page is scrolled before it's fully loaded
        offset_top: Math.max($("#menubar-container").outerHeight(), 33)
    });

    $('a#toggle-sidebar').click(function(){
      // expand the notebook when sidebar is hidden
      $('#notebook').toggleClass('col-md-9').toggleClass('col-md-12');
      $('#sidebar').toggleClass('hidden');
      $("#notebook-panels").trigger("sticky_kit:recalc");
    });

    // start with sidebar hidden for now
    $('a#toggle-sidebar').click();
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
      // FIXME: disabled as seem to freeze UI, fix or remove the feature
      self.checkClash = ko.observable(false);
      self.definitions = {};
      self.definitions.data = ko.observableArray([]);
      self.clearDefinitions = function() {
        self.definitions.data.remove(function(e) { return true });
      };
      self.findDirty = function(v) {
        if (!v) return [];
        var defs = self.definitions.data();
        var findDirtyInDef = function(def, v) {
          if (def.refs) {
            return _.find(def.refs, function(r) { return r === v.name });
          } else return [];
        };
        var findDirtyInDefs = function(defs, v) {
          return _.filter(defs, function(def) { return findDirtyInDef(def, v)});
        }
        var catchThemAll = function(vs, acc) {
          if (!vs || !vs.length) return acc;
          else {
            var dirty = _.flatten(_.map(vs, function(v) { return findDirtyInDefs(defs, v); }));
            return catchThemAll(dirty, _.flatten([acc, dirty]));
          }
        };
        return catchThemAll([v], []);
      };
      self.addDefinition = function(def) {
        var finder = function(e) { return e.name == def.name;};
        var existing = _.find(self.definitions.data(), finder);
        if (existing && self.checkClash() === true) {
          if (existing.cell != def.cell) {
            self.hightlight(existing, "danger", true);
          } else {
            var dirty = self.findDirty(def);
            _.each(dirty, function(d) {
              var cell = self.findCell(d).attr("data-dirty", true);
              if (!cell.find(".validate-output").length){
                var validate = $('<small><a href="#" class="btn btn-xs btn-success validate-output">Ignore</a></small>');
                validate.find("a.validate-output").click(function(e) {
                  e.preventDefault();
                  e.stopPropagation();
                  validate.remove();
                  cell.removeClass("alert").removeClass("alert-warning");
                });
                cell.find("div.pull-right.text-info").append(validate);
                self.hightlight(d, "warning", false, false);                
              } 
            });
          }
        }
        self.definitions.data.remove(finder);
        self.definitions.data.push(def);
        // update the counter inside the sidebar "vars" tab
        $('#defined-terms-count').text(self.definitions.data().length);
      };
      self.findCell = function(def) {
        var cell = $("div.cell[data-cell-id='"+def.cell+"']");
        return cell;
      };
      self.hightlight = function(def, level, scroll, timeout) {
        var cell = self.findCell(def);
        var level = level || "info";
        var scroll = scroll === true || scroll === undefined;
        var timeout = (timeout === false ? false : (timeout || 800));
        cell.addClass("alert alert-"+level);

        if (scroll) $('#site').scrollTo(cell, { duration: 300, offsetTop: 200 });

        if (timeout) {
          setTimeout(function() {
            cell.removeClass("alert alert-"+level)
          }, timeout);
        }
      };
      self.hightlightCell = function() {
        self.hightlight(this);
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
      // clear old progressbar
      $('#all-jobs-progress-bar').html('');
      // disable link to Spark UI
      $('#link-to-spark-ui').addClass('disabled').find('a').attr('href', '#');
    });

    events.on('kernel_ready.Kernel', function(e, c) {
      var kernel = c.kernel;
      console.debug("kernel", kernel);

      kernel.events.on("new.Definition", function(e, c) {
        console.debug("new def", c);
        if (c.term || c.type) {
          model.addDefinition({name: c.term || c.type, type: c.tpe, cell: c.cell, refs: c.references});
        }
      });
    });
  }
});
