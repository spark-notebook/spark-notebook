define([
  'base/js/namespace',
  'jquery',
  'base/js/utils',
  'base/js/dialog',
], function(IPython, $, utils, dialog) {
  "use strict";

  var DockerList = function (selector, options) {
    this.selector = selector;
    if (this.selector !== undefined) {
      this.element = $(selector);
      this.style();
      this.bind_events();
    }
    options = options || {};
    this.options = options;
    this.contents = options.contents;
    this.base_url = options.base_url || utils.get_body_data("baseUrl");
    //this.notebook_path = options.notebook_path || utils.get_body_data("notebookPath");

    this.check_ok();
  };

  DockerList.prototype.style = function () {
    //$('#cluster_list').addClass('list_container');
    //$('#cluster_toolbar').addClass('list_toolbar');
    //$('#cluster_list_info').addClass('toolbar_info');
    //$('#cluster_buttons').addClass('toolbar_buttons');
  };


  DockerList.prototype.bind_events = function () {
    var that = this;
    //$('#refresh_cluster_list').click(function () {
    //  that.load_list();
    //});
    //$('#refresh_add_cluster').click(function () {
    //  that.add_cluster();
    //});
  };

  DockerList.prototype.check_ok = function () {
    var settings = {
      processData : false,
      cache : false,
      type : "OPTIONS",
      dataType : "json",
      success : $.proxy(this.check_ok_success, this),
      error : utils.log_ajax_error,
    };
    var url = utils.url_join_encode(this.base_url, 'dockers');
    $.ajax(url, settings);
  };

  DockerList.prototype.check_ok_success = function (data, status, xhr) {
    console.log("Docker availability: ", data);
    this.enabled = data;
  };


  IPython.DockerList = DockerList;

  return {
    'DockerList': DockerList
  };

});
