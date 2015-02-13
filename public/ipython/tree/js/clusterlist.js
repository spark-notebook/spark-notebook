// Copyright (c) IPython Development Team.
// Distributed under the terms of the Modified BSD License.

define([
  'base/js/namespace',
  'jquery',
  'base/js/utils',
  'base/js/dialog',
], function(IPython, $, utils, dialog) {
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
    this.base_url = options.base_url || utils.get_body_data("baseUrl");
    this.notebook_path = options.notebook_path || utils.get_body_data("notebookPath");
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
  };


  ClusterList.prototype.load_list = function () {
    var settings = {
      processData : false,
      cache : false,
      type : "GET",
      dataType : "json",
      success : $.proxy(this.load_list_success, this),
      error : utils.log_ajax_error,
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
      var item = new ClusterItem(element, this.options);
      item.update_state(data[i]);
      element.data('item', item);
      this.element.append(element);
    }
  };


  var ClusterItem = function (element, options) {
    this.element = $(element);
    this.base_url = options.base_url || utils.get_body_data("baseUrl");
    this.notebook_path = options.notebook_path || utils.get_body_data("notebookPath");
    this.data = null;
    this.style();
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
    var profile_col = $('<div/>').addClass('profile_col col-xs-4').text(this.data.profile);
    var status_col = $('<div/>').addClass('status_col col-xs-3').text('stopped');
    var name_col = $('<div/>').addClass('name_col col-xs-3').text(this.data.name);
    var create_button = $('<button/>').addClass("btn btn-default btn-xs").text("Create");
    var action_col = $('<div/>').addClass('action_col col-xs-2').append(
      $("<span/>").addClass("item_buttons btn-group").append(
        create_button
      )
    );
    this.element.empty()
      .append(profile_col)
      .append(name_col)
      .append(status_col)
      .append(action_col);
    create_button.click(function (e) {
      dialog.conf_cluster({
          profile: that.profile,
          template: that.template,
          callback: function (conf) {
              alert("TODO: create the notebook with the configuration: " + JSON.stringify(conf))
              var settings = {
              cache : false,
              data : clusterConf,
              type : "POST",
              dataType : "json",
              success : function (data, status, xhr) {
                that.update_state(data);
              },
              error : function (xhr, status, error) {
                status_col.text("error starting cluster");
                utils.log_ajax_error(xhr, status, error);
              }
            };
            status_col.text('starting');
            var url = utils.url_join_encode(
              that.base_url,
              'clusters',
              that.data.profile,
              'start'
            );
            $.ajax(url, settings);
          },
          name: that.name,
          keyboard_manager: that.keyboard_manager
        });
    });
  };


  // For backwards compatability.
  IPython.ClusterList = ClusterList;
  IPython.ClusterItem = ClusterItem;

  return {
    'ClusterList': ClusterList,
    'ClusterItem': ClusterItem,
  };
});
