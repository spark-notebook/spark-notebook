// Adastyx
define([
  'base/js/namespace',
  'jquery',
  'base/js/utils',
  'base/js/dialog',
], function(IPython, $, utils, dialog) {
  "use strict";

  var ProjectList = function (selector, options) {
    this.selector = selector;
    this.enabled = options.sbt_project_gen_enabled === "true" || utils.get_body_data("sbt_project_gen_enabled") === "true";
    console.log("is Sbt project generation enabled:" + this.enabled);
    if (this.enabled) {
      this.element = $(selector);
      this.style();
      this.bind_events();
    }
    options = options || {};
    this.options = options;
    this.contents = options.contents;
    this.base_url = options.base_url || utils.get_body_data("baseUrl");
    this.log_refresher = null;
  };

  ProjectList.prototype.style = function () {
    //$('#project_list').addClass('list_container');
    //$('#project_toolbar').addClass('list_toolbar');
    //$('#project_list_info').addClass('toolbar_info');
    //$('#project_buttons').addClass('toolbar_buttons');
  };


  ProjectList.prototype.bind_events = function () {
    var that = this;
    $('#refresh_project_list').click(function () {
      that.load_list();
    });
    $("#projects_logs_refresh").change(function() {
      that.stop_refreshing_logs();
      that.refresh_logs();
    });
  };

  ProjectList.prototype.load_list = function () {
    if (!this.enabled)
      return;
    var settings = {
      processData : false,
      cache : false,
      type : "GET",
      dataType : "json",
      success : $.proxy(this.load_list_success, this),
      error : utils.log_ajax_error
    };
    var url = utils.url_join_encode(this.base_url, 'projects');
    $.ajax(url, settings);
  };


  ProjectList.prototype.clear_list = function () {
    this.element.children('.list_item').remove();
  };

  ProjectList.prototype.load_list_success = function (data, status, xhr) {
    this.clear_list();
    var len = data.length;
    for (var i=0; i<len; i++) {
      var element = $('<div/>');
      var item = new ProjectItem(element, this.options, this);
      item.update_state(data[i]);
      element.data('item', item);
      this.element.append(element);
    }
  };

  ProjectList.prototype.stop_refreshing_logs = function() {
    if (this.log_refresher && this.log_refresher.interval) {
      clearInterval(this.log_refresher.interval);
    }
  };

  ProjectList.prototype.start_refreshing_logs = function(current) {
    this.stop_refreshing_logs();
    this.log_refresher = {
      item: current
    };
    this.refresh_logs();
  };

  ProjectList.prototype.refresh_logs = function() {
    var p = parseInt($("#projects_logs_refresh").val()*1000);
    var that = this;
    var r = function() {
      that.log_refresher.item.load(that.log_refresher.tpe);
    };
    var i = setInterval(function() { r(); }, p);
    this.log_refresher.interval = i;
  };

  var ProjectItem = function (element, options, projects) {
    this.element = $(element);
    this.base_url = options.base_url || utils.get_body_data("baseUrl");
    this.notebook_path = options.notebook_path || utils.get_body_data("notebookPath");
    this.new_notebook = options.new_notebook;

    this.data = null;
    this.cache = {
      job: []
    };
    this.style();
    this.projects = projects;
    var that = this;
    $('#projects_logs_lines').on("change", function (e) {
      that.update_log_area();
    });

  };

  ProjectItem.prototype.style = function () {
    this.element.addClass('list_item').addClass("row");
  };

  ProjectItem.prototype.update_state = function (data) {
    this.data = data;
    this.create();
  };

  ProjectItem.prototype.load = function () {
    var settings = {
      processData : false,
      cache : false,
      type : "GET",
      dataType : "json",
      success : $.proxy(this.load_success(), this),
      error : utils.log_ajax_error
    };
    var purl = utils.url_join_encode(this.base_url, 'projects');
    var url = utils.url_join_encode(purl, this.data.project);
    var c = this.cache.job;
    url = utils.url_join_encode(url, "job");
    url += "?from="+c.length;
    $.ajax(url, settings);
  };

  ProjectItem.prototype.load_success = function() {
    var that = this;
    return function (data, status, xhr) {
      that.cache.job = that.cache.job.concat(_.map(data.log, utils.fixConsole));
      var n = "Job";
      var title = $("<h4></h4>").text("Logging for " + that.data.project + ": " + n);
      var pre = $("<div></div>").css("height", "300px")
                                .addClass("logcontent")
                                .css("background-color", "#fffffa")
                                .css("overflow", "auto")
                                .css("width", "100%")
      var log = $("<div></div>").append(title).append(pre);
      $("#projects_logs_panel").empty().append(log);
      that.update_log_area();
    };
  };

  ProjectItem.prototype.update_log_area = function() {
    var ta = $("#projects_logs_panel div.logcontent");
    if (!ta.length) return;

    var c = this.cache.job;

    var nbLogLines = parseInt($('#projects_logs_lines').val());
    if (!c || !c.length) {
      ta.html("<strong>No Logs!</strong>");
    } else {
      ta.html(_.map(c.slice(-nbLogLines), function(t) { return "<div>"+t+"</div>"; } ).join("\n"));
    }
  };

  ProjectItem.prototype.create = function () {
    var that = this;


    var project_col = $('<div/>').addClass('project_col col-xs-3').text(this.data.project);

    var job_col = $('<div/>')
      .addClass('job_col col-xs-8').append(
        '<span class="logs-refresh-button sbt-project-action"><a href="#"><i class="fa fa-refresh"></i> see build logs</a></span>'
      );

    if (this.data.zip_archive_url) {
      job_col.append(" <a href='" + this.data.zip_archive_url + "' class='sbt-project-action'><span class='fa fa-download' aria-hidden='true'></span>download zip</a>&nbsp;&nbsp;&nbsp;");
    }

    if (this.data.git_repo_https_dir) {
      job_col.append(" <a href='" + this.data.git_repo_https_dir + "' target='_blank' class='sbt-project-action'><i class='fa fa-code-fork' aria-hidden='true'></i>git repo</a>&nbsp;&nbsp;&nbsp;");
    }

    if (this.data.build_status) {
      var boostrap_color = this.data.build_status;
      project_col.addClass("text-" + boostrap_color);
      job_col.find("a").addClass("text-" + boostrap_color);
    }

    var action_col = $('<div/>').addClass('action_col col-xs-1').text("...");

    job_col.find(".logs-refresh-button").click(function(e) {
      e.preventDefault();
      that.load();
      that.projects.start_refreshing_logs(that);
    });


    this.element.empty()
      .append(project_col)
      .append(job_col)
      .append(action_col);
  };


  IPython.ProjectList = ProjectList;
  IPython.ProjectItem = ProjectItem;

  return {
    'ProjectList': ProjectList,
    'ProjectItem': ProjectItem
  };

});
