define([
  'jquery',
  'knockout',
  'underscore'
], function($, ko, _) {
  root = { path: '/' }
  var TachyonModel = function() {
    var self = this;

    this.opened = ko.observable(true);

    this.size = ko.observable(200);

    this.width = ko.computed(function() {
      return self.size() + "px";
    });

    this.resize = function(data, event) {
      var sz = self.size();
      if (sz < 1000)
        self.size(sz+200);
      else
        self.size(200);
    };

    this.reload = function() {
      self.list(self.current());
    };

    this.close = function() {
      self.opened(false);
    };

    this.list = function(info) {
      var path = info.path;
      tachyonJsRoutes.controllers.TachyonProxy.ls(path).ajax({
        success: function(data) {
          self.current({ path: path });
          var newData = _.map(data, function(p) { return {path: p};});
          var dropCurrent;
          if (newData[0] == path) {
            dropCurrent = _.drop(newData, 1);
          } else {
            dropCurrent = newData;
          }
          self.paths(dropCurrent);
        },
        error: function(error){
          console.error("Cannot load tachyon data",  error)
        }
      })
    }

    this.current = ko.observable(root);

    this.fragments = ko.computed(function() {
      var path = self.current().path;
      var fs = path.split("/");
      if (_.last(fs) == "") fs.pop(); //remove trailing '/'
      var fgs = _.map(fs, function(f, i) {
        var path = (i==0)?"/":(_.take(fs, i+1).join("/"));
        var name = (i==0)?"root":f;
        return {
          path: path,
          name: name
        };
      });
      return fgs;
    });

    this.paths = ko.observableArray([]);
  }

  model = new TachyonModel();
  ko.applyBindings({ tachyon: model });
  model.list(root);
});