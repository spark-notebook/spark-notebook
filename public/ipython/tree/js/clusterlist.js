// Copyright (c) IPython Development Team.
// Distributed under the terms of the Modified BSD License.

define([
  'base/js/namespace',
  'jquery',
  'base/js/utils',
  'base/js/dialog',
  'underscore',
  'wizard'
], function(IPython, $, utils, dialog, _, wizard) {
  "use strict";

  var ClusterList = function (selector, options) {
    this.selector = selector;
    if (this.selector !== undefined) {
      this.element = $(selector);
      this.style();
      this.bind_events();
    }
    options = options || {};
    this.options = options;
    this.contents = options.contents;
    this.new_notebook = options.new_notebook;
    this.base_url = options.base_url || utils.get_body_data("baseUrl");
    this.notebook_path = options.notebook_path || utils.get_body_data("notebookPath");

    this.load_profiles();
  };

  ClusterList.prototype.style = function () {
    $('#cluster_list').addClass('list_container');
    $('#cluster_toolbar').addClass('list_toolbar');
    $('#cluster_list_info').addClass('toolbar_info');
    $('#cluster_buttons').addClass('toolbar_buttons');
  };


  ClusterList.prototype.bind_events = function () {
    var that = this;
    $('#refresh_cluster_list').click(function () {
      that.load_list();
    });
    $('#refresh_add_cluster').click(function () {
      that.add_cluster();
    });
  };


  ClusterList.prototype.load_profiles = function () {
    var settings = {
      processData : false,
      cache : false,
      type : "GET",
      dataType : "json",
      success : $.proxy(this.load_profiles_success, this),
      error : utils.log_ajax_error
    };
    var url = utils.url_join_encode(this.base_url, 'profiles');
    $.ajax(url, settings);
  };

  ClusterList.prototype.load_profiles_success = function (data, status, xhr) {
    this.profiles = data;
  };


  ClusterList.prototype.add_cluster = function () {
    var that = this;

    dialog.configure({
      name: "New Name",
      profiles: that.profiles,
      template: {
        name: "New Name",
        profile: "Profile ID",
        template: {
          customLocalRepo: null,
          customRepos: null,
          customDeps: null,
          customImports: null,
          customSparkConf: null
        }
      },
      //template: ,
      callback: function (clusterConf) {
        var settings = {
          data : JSON.stringify(clusterConf),
          processData : false,
          cache : false,
          type : "POST",
          dataType : "json",
          success : $.proxy(that.add_cluster_success, that),
          error : utils.log_ajax_error
        };
        var url = utils.url_join_encode(that.base_url, 'clusters');
        $.ajax(url, settings);
      },
      keyboard_manager: that.keyboard_manager
    });
  };
  //删除一个集群
  ClusterList.prototype.delete_cluster = function () {
      var url = utils.url_join_encode(that.base_url, 'delCluster');
      $.ajax(url, "sjk","{}");
  };

  ClusterList.prototype.add_cluster_success = function (data, status, xhr) {
    var element = $('<div/>');
    var item = new ClusterItem(element, this.options);
    item.update_state(data);
    element.data('item', item);
    this.element.append(element);
  };

  ClusterList.prototype.load_list = function () {
    var settings = {
      processData : false,
      cache : false,
      type : "GET",
      dataType : "json",
      success : $.proxy(this.load_list_success, this),
      error : utils.log_ajax_error
    };
    var url = utils.url_join_encode(this.base_url, 'clusters');
    $.ajax(url, settings);
  };


  ClusterList.prototype.clear_list = function () {
    this.element.children('.list_item').remove();
  };

  ClusterList.prototype.load_list_success = function (data, status, xhr) {
    this.clear_list();
    var len = data.length;
    for (var i=0; i<len; i++) {
      var element = $('<div/>');
      var item = new ClusterItem(element, this.options, this);
      item.update_state(data[i]);
      element.data('item', item);
      this.element.append(element);
    }
  };


  var ClusterItem = function (element, options, clusters) {
    this.element = $(element);
    this.base_url = options.base_url || utils.get_body_data("baseUrl");
    this.notebook_path = options.notebook_path || utils.get_body_data("notebookPath");
    this.new_notebook = options.new_notebook;

    this.data = null;
    this.style();
    this.clusters = clusters;
  };

  ClusterItem.prototype.style = function () {
    this.element.addClass('list_item').addClass("row");
  };

  ClusterItem.prototype.update_state = function (data) {
    this.data = data;
    this.create();
  };


  ClusterItem.prototype.create = function () {
    var that = this;
    var profile_col = $('<div/>').addClass('profile_col col-xs-2').text(this.data.profile);
    var name_col = $('<div/>').addClass('name_col col-xs-2').text(this.data.name);

    var local_repo_col = $('<div/>').addClass('local_repo_col col-xs-2').text(this.data.template.customLocalRepo);

    var showItemListAndPopup = function(name, title, list, format, style, size) {
      var name_col = $('<div/>').addClass('name_col col-xs-'+(size||1)).text(""+_.size(list));
      if (_.size(list) > 0) {
        name_col.popover({
          html: true,
          placement: "bottom",
          trigger: "hover",
          title: title,
          //delay: { show: 500, hide: 100 },
          content: function() {
            var ul = $("<ul style='list-style-type: "+(style||"disc")+";'></ul>");
            _.each(list, function(item) {
              var li = $("<li></li>").append((format || _.identity)(item.trim()));
              ul.append(li);
            });
            return ul;
          }
        });
      }
      return name_col;
    };

    var repos_col = showItemListAndPopup("repos", "Repositories", that.data.template.customRepos);

    var deps_col = showItemListAndPopup("deps", "Dependencies", that.data.template.customDeps, function(item) {
      var content = $("<span></span>");
      var t = item;
      if (_.size(item.match(/^-.*/)) > 0) {
        content.append($('<i class="fa fa-minus"></i>'));
        t = item.substring(1).trim();
      } else {
        content.append($('<i class="fa fa-plus"></i>'));
        if (_.size(item.match(/^\+.*/)) > 0) {
          t = item.substring(1).trim();
        }
      }
      content.append($('<span></span>').text(" " + t));
      return content;
    },
    "none");

    var imports_col = showItemListAndPopup ("imports", "Imports", that.data.template.customImports, function(item) { return item.trim().replace(new RegExp("import"), "").trim()});

    var sparkConf = _.chain(that.data.template.customSparkConf).pairs().map(function(item) { return item[0] + " → " + item[1]; }).value();
    var spark_conf_repo_col = showItemListAndPopup("spark_conf", "Spark Conf", sparkConf, _.identity, "disc", 2);

    var create_button = $('<button/>').addClass("btn btn-default btn-xs").text("Create");
    var delete_button = $('<button/>').addClass("btn btn-default btn-xs").text("Delete");
    var action_col = $('<div/>').addClass('action_col col-xs-1').append(
      $("<span/>").addClass("item_buttons btn-group").append(
        create_button
      )
    ).append(
      $("<span/>").addClass("item_buttons btn-group").append(
          delete_button
      )
    );

    this.element.empty()
      .append(profile_col)
      .append(name_col)
      .append(local_repo_col)
      .append(repos_col)
      .append(deps_col)
      .append(imports_col)
      .append(spark_conf_repo_col)
      .append(action_col);
    create_button.click(function (e) {
      dialog.configure({
          //name: that.data.name,
          profiles: that.clusters.profiles,
          template: that.data.template,
          callback: function (clusterConf) {
            var conf = _.extend(clusterConf, clusterConf.template);
            delete conf.template;
            that.new_notebook.new_notebook("spark", conf);
          },
          name: that.data.name,
          profile: that.data.profile,
          keyboard_manager: that.keyboard_manager
        });
    });

    delete_button.click(function (e) {
        var settings = {
            processData : false,
            data : "name="+that.data.name,
            cache : false,
            type : "GET",
            dataType : "json",
            success : function () {
                that.load_sessions();
            },
            error : utils.log_ajax_error
        };
        alert(that.data.name)
        var url = utils.url_join_encode(that.base_url, 'delCluster');
        $.ajax(url, settings);
    });
  };


  // For backwards compatability.
  IPython.ClusterList = ClusterList;
  IPython.ClusterItem = ClusterItem;

  return {
    'ClusterList': ClusterList,
    'ClusterItem': ClusterItem
  };
});
