define([
  'jquery',
  'base/js/namespace',
  'base/js/events',
  'base/js/utils',
  'knockout',
  'underscore'
], function($, IPython, events, utils, ko, _) {
  var loaded = false;

  var load_checkpoints = function() {
    if (loaded) return;
    require(['observable'], function(O) {
      var CheckpointsModel = function() {
        var self = this;

        this.checkpoints = [];

        this.renderedCheckpoints = ko.observableArray(null);

        this.query = ko.observable(null).extend({ rateLimit: { //throttle
                                                    timeout: 1000,
                                                    method: "notifyWhenChangesStop"
                                                  }
                                                });
        this.restore = function(checkpoint) {
          IPython.notebook.restore_checkpoint_dialog(checkpoint);
        };

        this.renderCheckpoints = function(checkpoints) {
          self.checkpoints = _.map(checkpoints, function(c) {
            c.restore = function() { return self.restore(c); };
            return c;
          });  // keep original
          self.renderFilteredCheckpoints(self.query());
          //_.each(self.checkpoints, function(c) { self.renderedCheckpoints.push(c); });
        };

        this.renderFilteredCheckpoints = function(query) {
          query = query || "";
          // remove all the current beers, which removes them from the view
          self.renderedCheckpoints.removeAll();

          for(var x in self.checkpoints) {
            var c = self.checkpoints[x];
            if(c.message.toLowerCase().indexOf(query.toLowerCase()) >= 0) {
              self.renderedCheckpoints.push(c);
            }
          }
        };

        events.on('checkpoints_listed.Notebook', function (checkpoints){
          self.renderCheckpoints(IPython.notebook.checkpoints);
        });

        if (IPython.notebook.checkpoints) {
          self.renderCheckpoints(IPython.notebook.checkpoints);
        }

        this.focus = function(){
            IPython.notebook.keyboard_manager.edit_mode();
            IPython.notebook.keyboard_manager.enable();
        };
        this.blur = function(){
            IPython.notebook.keyboard_manager.command_mode();
            events.trigger('command_mode.Cell', {cell: IPython.notebook.get_selected_cell()});
        };

        this.isTyping = ko.observable(false);
        this.isTyping.subscribe(function(is) {
          if (is) {
            self.focus();
          } else {
            self.blur();
          }
        });

        this.query.subscribe(this.renderFilteredCheckpoints);
      };


      model = new CheckpointsModel();
      var checkpointsUI = $("#checkpoints-ui").get(0);
      ko.cleanNode(checkpointsUI);
      ko.applyBindings({ checkpoints: model }, checkpointsUI);
    });
  };

  if (IPython.observable_ready) load_checkpoints();
  else events.on('Observable.ready', load_checkpoints);

});