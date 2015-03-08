// Copyright (c) IPython Development Team.
// Distributed under the terms of the Modified BSD License.

define(function(require) {
    "use strict";

    var CodeMirror = require('codemirror/lib/codemirror');
    var IPython = require('base/js/namespace');
    var $ = require('jquery');
    var ko = require('knockout');

    /**
     * A wrapper around bootstrap modal for easier use
     * Pass it an option dictionary with the following properties:
     *
     *    - body : <string> or <DOM node>, main content of the dialog
     *            if pass a <string> it will be wrapped in a p tag and
     *            html element escaped, unless you specify sanitize=false
     *            option.
     *    - title : Dialog title, default to empty string.
     *    - buttons : dict of btn_options who keys are button label.
     *            see btn_options below for description
     *    - open : callback to trigger on dialog open.
     *    - destroy:
     *    - notebook : notebook instance
     *    - keyboard_manager: keyboard manager instance.
     *
     *  Unlike bootstrap modals, the backdrop options is set by default
     *  to 'static'.
     *
     *  The rest of the options are passed as is to bootstrap modals.
     *
     *  btn_options: dict with the following property:
     *
     *    - click : callback to trigger on click
     *    - class : css classes to add to button.
     *
     *
     *
     **/
    var modal = function (options) {

        var modal = $("<div/>")
            .addClass("modal")
            .addClass("fade")
            .attr("role", "dialog");
        var dialog = $("<div/>")
            .addClass("modal-dialog")
            .appendTo(modal);
        var dialog_content = $("<div/>")
            .addClass("modal-content")
            .appendTo(dialog);
        if(typeof(options.body) === 'string' && options.sanitize !== false){
            options.body = $("<p/>").text(options.body)
        }
        dialog_content.append(
            $("<div/>")
                .addClass("modal-header")
                .append($("<button>")
                    .attr("type", "button")
                    .addClass("close")
                    .attr("data-dismiss", "modal")
                    .attr("aria-hidden", "true")
                    .html("&times;")
                ).append(
                    $("<h4/>")
                        .addClass('modal-title')
                        .text(options.title || "")
                )
        ).append(
            $("<div/>").addClass("modal-body").append(
                options.body || $("<p/>")
            )
        );

        var footer = $("<div/>").addClass("modal-footer");

        for (var label in options.buttons) {
            var btn_opts = options.buttons[label];
            var button = $("<button/>")
                .addClass("btn btn-default btn-sm")
                .attr("data-dismiss", "modal")
                .text(label);
            if (btn_opts.click) {
                button.click($.proxy(btn_opts.click, dialog_content));
            }
            if (btn_opts.class) {
                button.addClass(btn_opts.class);
            }
            footer.append(button);
        }
        dialog_content.append(footer);
        // hook up on-open event
        modal.on("shown.bs.modal", function() {
            setTimeout(function() {
                footer.find("button").last().focus();
                if (options.open) {
                    $.proxy(options.open, modal)();
                }
            }, 0);
        });

        // destroy modal on hide, unless explicitly asked not to
        if (options.destroy === undefined || options.destroy) {
            modal.on("hidden.bs.modal", function () {
                modal.remove();
            });
        }
        modal.on("hidden.bs.modal", function () {
            if (options.notebook) {
                var cell = options.notebook.get_selected_cell();
                if (cell) cell.select();
            }
            if (options.keyboard_manager) {
                options.keyboard_manager.enable();
                options.keyboard_manager.command_mode();
            }
        });

        if (options.keyboard_manager) {
            options.keyboard_manager.disable();
        }

        options.backdrop = options.backdrop || 'static';

        return modal.modal(options);
    };

    var kernel_modal = function (options) {
        /**
         * only one kernel dialog should be open at a time -- but
         * other modal dialogs can still be open
         */
        $('.kernel-modal').modal('hide');
        var dialog = modal(options);
        dialog.addClass('kernel-modal');
        return dialog;
    };

    var edit_metadata = function (options) {
        options.name = options.name || "Cell";
        var error_div = $('<div/>').css('color', 'red');
        var message =
            "Manually edit the JSON below to manipulate the metadata for this " + options.name + "." +
            " We recommend putting custom metadata attributes in an appropriately named sub-structure," +
            " so they don't conflict with those of others.";

        var textarea = $('<textarea/>')
            .attr('rows', '13')
            .attr('cols', '80')
            .attr('name', 'metadata')
            .text(JSON.stringify(options.md || {}, null, 2));

        var dialogform = $('<div/>').attr('title', 'Edit the metadata')
            .append(
                $('<form/>').append(
                    $('<fieldset/>').append(
                        $('<label/>')
                        .attr('for','metadata')
                        .text(message)
                        )
                        .append(error_div)
                        .append($('<br/>'))
                        .append(textarea)
                    )
            );
        var editor = CodeMirror.fromTextArea(textarea[0], {
            lineNumbers: true,
            matchBrackets: true,
            indentUnit: 2,
            autoIndent: true,
            mode: 'application/json',
        });
        var modal_obj = modal({
            title: "Edit " + options.name + " Metadata",
            body: dialogform,
            buttons: {
                OK: { class : "btn-primary",
                    click: function() {
                        /**
                         * validate json and set it
                         */
                        var new_md;
                        try {
                            new_md = JSON.parse(editor.getValue());
                        } catch(e) {
                            console.log(e);
                            error_div.text('WARNING: Could not save invalid JSON.');
                            return false;
                        }
                        options.callback(new_md);
                    }
                },
                Cancel: {}
            },
            notebook: options.notebook,
            keyboard_manager: options.keyboard_manager,
        });

        modal_obj.on('shown.bs.modal', function(){ editor.refresh(); });
    };

    // TODO: merge with edit_metadata  → almost identical
    var conf_cluster = function (options) {
        options.name = options.name || "Configuration";
        var error_div = $('<div/>').css('color', 'red');
        var message =
            "Manually edit the JSON below to manipulate the configuration for this "+ options.profile + "configuration " + options.name + ".";

        var textarea = $('<textarea/>')
            .attr('rows', '13')
            .attr('cols', '80')
            .attr('name', 'conf')
            .text(JSON.stringify(options.template || {}, null, 2));

        var dialogform = div().attr('title', 'Configuration ' + options.name)
            .append(
                $('<form/>').append(
                    $('<fieldset/>').append(
                        $('<label/>')
                        .attr('for','conf')
                        .text(message)
                        )
                        .append(error_div)
                        .append($('<br/>'))
                        .append(textarea)
                    )
            );
        var editor = CodeMirror.fromTextArea(textarea[0], {
            lineNumbers: true,
            matchBrackets: true,
            indentUnit: 2,
            autoIndent: true,
            mode: 'application/json',
        });

        // preformat for
        var modal_obj = modal({
            title: "Edit " + options.name + " Configuration",
            body: dialogform,
            buttons: {
                OK: { class : "btn-primary",
                    click: function() {
                        /**
                         * validate json and set it
                         */
                        var new_conf;
                        try {
                            new_conf = JSON.parse(editor.getValue());
                        } catch(e) {
                            console.log(e);
                            error_div.text('WARNING: Could not save invalid JSON.');
                            return false;
                        }
                        options.callback(new_conf);
                    }
                },
                Cancel: {}
            }
        });

        modal_obj.on('shown.bs.modal', function(){ editor.refresh(); });
    };

    var elt = function(elt) {
        return function(options) {
            options = options || {};
            return $("<"+elt+"></"+elt+">").attr("id", options.id || "").addClass(options.clazz || "");
        }
    };
    var div = elt("div");
    var ul = elt("ul");
    var li = elt("li");
    var a = elt("a");
    var form = elt("form");
    var label = elt("label");
    var input = elt("input");
    var textarea = elt("textarea");
    var button = elt("button");
    var select = elt("select");
    var p = elt("p");
    var br = function() { return $("<br/>"); };


    var new_cluster = function (options) {
        options.name = options.name || "Cluster";
        var error_div = $('<div/>').css('color', 'red');
        var message = "Create a configuration.";

        var newClusterWizard = div();
        var navbar = div({clazz: "navbar"})
                        .append(
                            div({clazz: "navbar-inner"})
                                .append(div({clazz: "container"}).append(ul()))
                        )
                        .appendTo(newClusterWizard)
                        .find(".container ul");

        var tabs = div({clazz: "tab-content"}).appendTo(newClusterWizard);

        var pager = ul({clazz: "pager wizard"}).appendTo(tabs);
        pager.data("conf", {});
        pager.append(li({clazz: "previous first"}).append(a().attr("href", "javascript:;").text("First")))
             .append(li({clazz: "previous"}).append(a().attr("href", "javascript:;").text("Previous")))
             .append(li({clazz: "next last"}).append(a().attr("href", "javascript:;").text("Last")))
             .append(li({clazz: "next"}).append(a().attr("href", "javascript:;").text("Next")));

        var addPane = function(id, label, addContent) {
            li().append(a().attr("href", "#"+id).attr("data-toggle", "tab").text(label)).appendTo(navbar);
            var page = div({clazz: "tab-pane", id: id});
            pager.before(page);
            addContent(page);
        };



        addPane("conf_name", "Name", function(page) {
            page.addClass("active");
            page.append(
                input() .attr("type", "text")
                        .attr("placeholder", "Enter the name of this configuration")
                        .attr("data-bind", "value: name")
                        .attr("size", "100")
            );
            var NameModel = function() {
                this.name = ko.observable("");
                pager.data("conf").name =  this.name;
            };
            ko.applyBindings(new NameModel(), page.get(0));
        });


        addPane("conf_profiles", "Profiles", function(page) {
            var profilesForm = form();

            var addProfile = function(profile) {
                var value = profile.id;
                var labelText = profile.name;
                profilesForm.append(
                    label({clazz: "radio"})
                        .css("margin-left", "20px")
                        .append(
                            input() .attr("type", "radio")
                                    .attr("name", "profile")
                                    //.attr("id", "newClusterProfiles1")
                                    .attr("data-bind", "checked: profile")
                                    .attr("value", value)
                                    .data("profile", profile)
                        )
                        .append(labelText)
                );
            };

            var ProfileModel = function() {
                this.profile = ko.observable("");
                pager.data("conf").profile =  this.profile;
            };

            _.each(options.profiles, addProfile);
            ko.applyBindings(new ProfileModel(), profilesForm.get(0));
            page.append(profilesForm);
        });



        addPane("conf_local_repo", "Local Repo", function(page) {
            page.append(
                input() .attr("type", "text")
                        .attr("placeholder", "Enter path to preferred repo")
                        .attr("data-bind", "value: localRepo")
                        .attr("size", "100")
            );
            var LocalRepoModel = function() {
                this.localRepo = ko.observable("");
                pager.data("conf").local =  this.localRepo;
            };
            ko.applyBindings(new LocalRepoModel(), page.get(0));
        });


        addPane("conf_remotes", "Libraries Repos", function(page) {
            var remotes = div();
            remotes.append(
                form()  .attr("data-bind", "submit:addRemote")
                        .append("Add remote")
                        .append(
                            input() .attr("type", "text")
                                    .css("width", "500px")
                                    .attr("data-bind", 'value: remoteToAdd, valueUpdate: "afterkeydown"')
                        )
                        .append(
                            button().attr("type", "submit")
                                    .attr("data-bind", "enable: remoteToAdd().length>0")
                                    .text("Add")
                        )
            );
            remotes.append(p().text("Current remotes"));
            remotes.append(
                select().attr("multiple", "multiple")
                        .attr("height", "5")
                        .css("width", "500px")
                        .attr("data-bind", "options:allRemotes, selectedOptions:selectedRemotes")
            );

            remotes.append(
                div().append(
                        button().attr("data-bind", "click: removeSelectedRemotes, enable: selectedRemotes().length > 0")
                                .text("Remove")
                    )
            );

            var ConfigurationRemotesModel = function () {
                this.remoteToAdd = ko.observable("");
                this.allRemotes = ko.observableArray([]); // Initial remotes
                this.selectedRemotes = ko.observableArray([]); // Initial selection

                this.addRemote = function () {
                    if ((this.remoteToAdd() != "") && (this.allRemotes.indexOf(this.remoteToAdd()) < 0)) // Prevent blanks and duplicates
                        this.allRemotes.push(this.remoteToAdd());
                    this.remoteToAdd(""); // Clear the text box
                };

                this.removeSelectedRemotes = function () {
                    this.allRemotes.removeAll(this.selectedRemotes());
                    this.selectedRemotes([]); // Clear selection
                };
                pager.data("conf").remotes = this.allRemotes;
            };

            ko.applyBindings(new ConfigurationRemotesModel(), remotes.get(0));
            page.append(remotes);
        });


        addPane("conf_deps", "Dependencies", function(page) {
            var deps = div();
            deps.append(
                form()  .attr("data-bind", "submit:addDep")
                        .append("Add dep")
                        .append(
                            input() .attr("type", "text")
                                    .css("width", "500px")
                                    .attr("data-bind", 'value: depToAdd, valueUpdate: "afterkeydown"')
                        )
                        .append(
                            button().attr("type", "submit")
                                    .attr("data-bind", "enable: depToAdd().length>0")
                                    .text("Add")
                        )
            );
            deps.append(p().text("Current deps"));
            deps.append(
                select().attr("multiple", "multiple")
                        .attr("height", "5")
                        .css("width", "500px")
                        .attr("data-bind", "options:allDeps, selectedOptions:selectedDeps")
            );

            deps.append(
                div().append(
                        button().attr("data-bind", "click: removeSelectedDeps, enable: selectedDeps().length > 0")
                                .text("Remove")
                    )
            );

            var ConfigurationDepsModel = function () {
                this.depToAdd = ko.observable("");
                this.allDeps = ko.observableArray([]); // Initial deps
                this.selectedDeps = ko.observableArray([]); // Initial selection

                this.addDep = function () {
                    if ((this.depToAdd() != "") && (this.allDeps.indexOf(this.depToAdd()) < 0)) // Prevent blanks and duplicates
                        this.allDeps.push(this.depToAdd());
                    this.depToAdd(""); // Clear the text box
                };

                this.removeSelectedDeps = function () {
                    this.allDeps.removeAll(this.selectedDeps());
                    this.selectedDeps([]); // Clear selection
                };
                pager.data("conf").deps = this.allDeps;
            };

            ko.applyBindings(new ConfigurationDepsModel(), deps.get(0));
            page.append(deps);
        });


        addPane("conf_imports", "Imports", function(page) {
            var imports = div();
            imports.append(
                form()  .attr("data-bind", "submit:addImport")
                        .append("Add import")
                        .append(
                            input() .attr("type", "text")
                                    .css("width", "500px")
                                    .attr("placeholder", "you don't need to prefix with 'import'!")
                                    .attr("data-bind", 'value: importToAdd, valueUpdate: "afterkeydown"')
                        )
                        .append(
                            button().attr("type", "submit")
                                    .attr("data-bind", "enable: importToAdd().length>0")
                                    .text("Add")
                        )
            );
            imports.append(p().text("Current imports"));
            imports.append(
                select().attr("multiple", "multiple")
                        .attr("height", "5")
                        .css("width", "500px")
                        .attr("data-bind", "options:allImports, selectedOptions:selectedImports")
            );

            imports.append(
                div().append(
                        button().attr("data-bind", "click: removeSelectedImports, enable: selectedImports().length > 0")
                                .text("Remove")
                    )
            );

            var ConfigurationImportsModel = function () {
                this.importToAdd = ko.observable("");
                this.allImports = ko.observableArray([]); // Initial imports
                this.fullImports = ko.pureComputed(function() {
                    return _.map(this.allImports(), function(i) {return 'import ' + i;});
                }, this);
                this.selectedImports = ko.observableArray([]); // Initial selection

                this.addImport = function () {
                    if ((this.importToAdd() != "") && (this.allImports.indexOf(this.importToAdd()) < 0)) // Prevent blanks and duplicates
                        this.allImports.push(this.importToAdd());
                    this.importToAdd(""); // Clear the text box
                };

                this.removeSelectedImports = function () {
                    this.allImports.removeAll(this.selectedImports());
                    this.selectedImports([]); // Clear selection
                };
                pager.data("conf").imports = this.fullImports;
            };

            ko.applyBindings(new ConfigurationImportsModel(), imports.get(0));
            page.append(imports);
        });


        addPane("conf_spark", "Spark Conf", function(page) {
            var sparkConf = div();

            var idToProfile = function(id) {
                var x = _.find(options.profiles, function(v) {return v.id == id;});
                x = (x && x.template && x.template.customSparkConf)  || {};
                return JSON.stringify(x, null, 2);
            };

            //get the profile out of pager
            var profileTemplate = idToProfile(pager.data("conf").profile());

            var ta = textarea()
                .attr('rows', '13')
                .attr('cols', '80')
                .attr('name', 'conf')
                .text(profileTemplate);

            sparkConf.append(
                        error_div
                    ).append(
                        br()
                    )
                    .append(
                        ta
                    )

            var editor = CodeMirror.fromTextArea(ta[0], {
                lineNumbers: true,
                matchBrackets: true,
                indentUnit: 2,
                autoIndent: true,
                mode: 'application/json',
            });

            pager.data("conf").profile.subscribe(function(v) {
                profileTemplate = idToProfile(v);
                editor.setValue(profileTemplate);
            });

            editor.on("change", function(cm) {
                var sparkConf;
                try {
                    sparkConf = JSON.parse(cm.getValue());
                } catch(e) {
                    console.log(e);
                    //error_div.text('WARNING: Could not save invalid JSON.');
                    return false;
                }

                pager.data("conf").sparkConf = sparkConf;
            });

            pager.data("conf").sparkConfEditor = editor;
            page.append(sparkConf);
        });

        var dialogform = $('<div/>').attr('title', 'Configuration for the cluster ' + options.name)
            .append(
                newClusterWizard
            );

        // preformat for
        var modal_obj = modal({
            title: "Edit " + options.name + " Configuration",
            body: dialogform,
            buttons: {
                OK: { class : "btn-primary",
                    click: function() {
                        /**
                         * validate json and set it
                         */
                        /*var new_md;
                        try {
                            new_md = JSON.parse(editor.getValue());
                        } catch(e) {
                            console.log(e);
                            error_div.text('WARNING: Could not save invalid JSON.');
                            return false;
                        }
                        options.callback(new_md);
                        */
                        var o = {
                            "profile": pager.data("conf").profile(),
                            "name": pager.data("conf").name(),
                            "status": "stopped",
                            "template": {
                                "customLocalRepo" : pager.data("conf").local(),
                                "customRepos" : pager.data("conf").remotes(),
                                "customDeps" : pager.data("conf").deps(),
                                "customImports" : pager.data("conf").imports(),
                                "customSparkConf" : pager.data("conf").sparkConf
                            }
                        }
                        options.callback(o);
                    }
                },
                Cancel: {}
            }
        });

        newClusterWizard.bootstrapWizard({onTabShow: function(tab, navigation, index) {
            var $total = navigation.find('li').length;
            var $current = index+1;
            //var $percent = ($current/$total) * 100;
            //newClusterWizard.find('.bar').css({width:$percent+'%'});

            pager.data("conf").sparkConfEditor.refresh();
        }});
    };
    var dialog = {
        modal : modal,
        kernel_modal : kernel_modal,
        edit_metadata : edit_metadata,
        new_cluster : new_cluster,
        conf_cluster : conf_cluster
    };

    // Backwards compatability.
    IPython.dialog = dialog;

    return dialog;
});
