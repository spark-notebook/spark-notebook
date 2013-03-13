//----------------------------------------------------------------------------
//  Copyright (C) 2008-2011  The IPython Development Team
//
//  Distributed under the terms of the BSD License.  The full license is in
//  the file COPYING, distributed as part of this software.
//----------------------------------------------------------------------------

//============================================================================
// Notebook
//============================================================================

var IPython = (function (IPython) {

    var utils = IPython.utils;

    var Notebook = function (selector) {
        this.read_only = IPython.read_only;
        this.element = $(selector);
        this.element.scroll();
        this.element.data("notebook", this);
        this.next_prompt_number = 1;
        this.kernel = null;
        this.clipboard = null;
        this.paste_enabled = false;
        this.autosave_timer = false;
        this.msg_cell_map = {};
        this.io_cell_map = {};
        this.metadata = {};
        this.saved_worksheets = {};
        this.control_key_active = false;
        this.notebook_id = null;
        this.notebook_name = null;
        this.notebook_name_blacklist_re = /[\/\\]/;
        this.nbformat = 3 // Increment this when changing the nbformat
        
        this.style();
        this.create_elements();
        this.bind_events();
        this.set_tooltipontab(true);
        this.set_smartcompleter(true);
        this.set_timebeforetooltip(500);
        this.auto_save_time = 15000;
        this.auto_save_timestamp = new Date(0);
        this.user_save_timestamp = new Date(0);
    };


    Notebook.prototype.style = function () {
        $('div#notebook').addClass('border-box-sizing');
    };


    Notebook.prototype.create_elements = function () {
        // We add this end_space div to the end of the notebook div to:
        // i) provide a margin between the last cell and the end of the notebook
        // ii) to prevent the div from scrolling up when the last cell is being
        // edited, but is too low on the page, which browsers will do automatically.
        var that = this;
        var end_space = $('<div/>').addClass('end_space').height("30%");
        end_space.dblclick(function (e) {
            if (that.read_only) return;
            var ncells = that.ncells();
            that.insert_cell_below('code',ncells-1);
        });
        this.element.append(end_space);
        $('div#notebook').addClass('border-box-sizing');
    };


    Notebook.prototype.bind_events = function () {
        var that = this;

        this.element.bind('collapse_pager', function () {
            var app_height = $('div#main_app').height(); // content height
            var splitter_height = $('div#pager_splitter').outerHeight(true);
            var new_height = app_height - splitter_height;
            that.element.animate({height : new_height + 'px'}, 'fast');
        });

        this.element.bind('expand_pager', function () {
            var app_height = $('div#main_app').height(); // content height
            var splitter_height = $('div#pager_splitter').outerHeight(true);
            var pager_height = $('div#pager').outerHeight(true);
            var new_height = app_height - pager_height - splitter_height; 
            that.element.animate({height : new_height + 'px'}, 'fast');
        });

        $(window).bind('beforeunload', function () {
            if (that.autosave_timer && ! that.read_only) {
                that.auto_save_notebook(true);
            };
//            // Null is the *only* return value that will make the browser not
//            // pop up the "don't leave" dialog.
            return null;
        });

    };


    Notebook.prototype.scroll_to_bottom = function () {
        this.element.animate({scrollTop:this.element.get(0).scrollHeight}, 0);
    };


    Notebook.prototype.scroll_to_top = function () {
        this.element.animate({scrollTop:0}, 0);
    };


    // Cell indexing, retrieval, etc.

    Notebook.prototype.get_cell_elements = function () {
        return this.element.children("div.cell");
    };


    Notebook.prototype.get_cell_element = function (index) {
        var result = null;
        var e = this.get_cell_elements().eq(index);
        if (e.length !== 0) {
            result = e;
        }
        return result;
    };


    Notebook.prototype.ncells = function (cell) {
        return this.get_cell_elements().length;
    };


    // TODO: we are often calling cells as cells()[i], which we should optimize
    // to cells(i) or a new method.
    Notebook.prototype.get_cells = function () {
        return this.get_cell_elements().toArray().map(function (e) {
            return $(e).data("cell");
        });
    };


    Notebook.prototype.get_cell = function (index) {
        var result = null;
        var ce = this.get_cell_element(index);
        if (ce !== null) {
            result = ce.data('cell');
        }
        return result;
    }


    Notebook.prototype.get_next_cell = function (cell) {
        var result = null;
        var index = this.find_cell_index(cell);
        if (index !== null && index < this.ncells()) {
            result = this.get_cell(index+1);
        }
        return result;
    }


    Notebook.prototype.get_prev_cell = function (cell) {
        var result = null;
        var index = this.find_cell_index(cell);
        if (index !== null && index > 1) {
            result = this.get_cell(index-1);
        }
        return result;
    }

    Notebook.prototype.find_cell_index = function (cell) {
        var result = null;
        this.get_cell_elements().filter(function (index) {
            if ($(this).data("cell") === cell) {
                result = index;
            };
        });
        return result;
    };


    Notebook.prototype.index_or_selected = function (index) {
        var i;
        if (index === undefined || index === null) {
            i = this.get_selected_index();
            if (i === null) {
                i = 0;
            }
        } else {
            i = index;
        }
        return i;
    };


    Notebook.prototype.get_selected_cell = function () {
        var index = this.get_selected_index();
        return this.get_cell(index);
    };


    Notebook.prototype.is_valid_cell_index = function (index) {
        if (index !== null && index >= 0 && index < this.ncells()) {
            return true;
        } else {
            return false;
        };
    }

    Notebook.prototype.get_selected_index = function () {
        var result = null;
        this.get_cell_elements().filter(function (index) {
            if ($(this).data("cell").selected === true) {
                result = index;
            };
        });
        return result;
    };


    Notebook.prototype.cell_for_msg = function (msg_id, cell_map) {
        var cell_id = cell_map[msg_id];
        var result = null;
        this.get_cell_elements().filter(function (index) {
            cell = $(this).data("cell");
            if (cell.cell_id === cell_id) {
                result = cell;
            };
        });
        return result;
    };

    Notebook.prototype.clear_cell_msgs = function (cell_id, cell_map) {
            Object.keys(cell_map).forEach(function(key) {
               if (cell_map[key] == cell_id) delete cell_map[key];
            });
        };


    // Cell selection.

    Notebook.prototype.select = function (index) {
        if (index !== undefined && index >= 0 && index < this.ncells()) {
            sindex = this.get_selected_index()
            if (sindex !== null && index !== sindex) {
                this.get_cell(sindex).unselect();
            };
            var cell = this.get_cell(index)
            cell.select();
            if (cell.cell_type === 'heading') {
                $([IPython.events]).trigger('selected_cell_type_changed.Notebook',
                    {'cell_type':cell.cell_type,level:cell.level}
                );
            } else {
                $([IPython.events]).trigger('selected_cell_type_changed.Notebook',
                    {'cell_type':cell.cell_type}
                );
            };
        };
        return this;
    };


    Notebook.prototype.select_next = function () {
        var index = this.get_selected_index();
        if (index !== null && index >= 0 && (index+1) < this.ncells()) {
            this.select(index+1);
        };
        return this;
    };


    Notebook.prototype.select_prev = function () {
        var index = this.get_selected_index();
        if (index !== null && index >= 0 && (index-1) < this.ncells()) {
            this.select(index-1);
        };
        return this;
    };


    // Cell movement

    Notebook.prototype.move_cell_up = function (index) {
        var i = this.index_or_selected();
        if (i !== null && i < this.ncells() && i > 0) {
            var pivot = this.get_cell_element(i-1);
            var tomove = this.get_cell_element(i);
            if (pivot !== null && tomove !== null) {
                tomove.detach();
                pivot.before(tomove);
                this.select(i-1);
            };
        };
        this.set_dirty();
        return this;
    };


    Notebook.prototype.move_cell_down = function (index) {
        var i = this.index_or_selected();
        if (i !== null && i < (this.ncells()-1) && i >= 0) {
            var pivot = this.get_cell_element(i+1);
            var tomove = this.get_cell_element(i);
            if (pivot !== null && tomove !== null) {
                tomove.detach();
                pivot.after(tomove);
                this.select(i+1);
            };
        };
        this.set_dirty();
        return this;
    };


    Notebook.prototype.sort_cells = function () {
        // This is not working right now. Calling this will actually crash
        // the browser. I think there is an infinite loop in here...
        var ncells = this.ncells();
        var sindex = this.get_selected_index();
        var swapped;
        do {
            swapped = false;
            for (var i=1; i<ncells; i++) {
                current = this.get_cell(i);
                previous = this.get_cell(i-1);
                if (previous.input_prompt_number > current.input_prompt_number) {
                    this.move_cell_up(i);
                    swapped = true;
                };
            };
        } while (swapped);
        this.select(sindex);
        return this;
    };

    // Insertion, deletion.

    Notebook.prototype.delete_cell = function (index) {
        var i = this.index_or_selected(index);
        if (this.is_valid_cell_index(i)) {
            var ce = this.get_cell_element(i);
            ce.remove();
            if (i === (this.ncells())) {
                this.select(i-1);
            } else {
                this.select(i);
            };
            this.set_dirty();
        };
        return this;
    };


    Notebook.prototype.insert_cell_below = function (type, index) {
        // type = ('code','html','markdown')
        // index = cell index or undefined to insert below selected
        index = this.index_or_selected(index);
        var cell = null;
        if (this.ncells() === 0 || this.is_valid_cell_index(index)) {
            if (type === 'code') {
                cell = new IPython.CodeCell(this);
                cell.set_input_prompt();
            } else if (type === 'markdown') {
                cell = new IPython.MarkdownCell(this);
            } else if (type === 'html') {
                cell = new IPython.HTMLCell(this);
            } else if (type === 'raw') {
                cell = new IPython.RawCell(this);
            } else if (type === 'heading') {
                cell = new IPython.HeadingCell(this);
            };
            if (cell !== null) {
                if (this.ncells() === 0) {
                    this.element.find('div.end_space').before(cell.element);
                } else if (this.is_valid_cell_index(index)) {
                    this.get_cell_element(index).after(cell.element);
                };
                cell.render();
                this.select(this.find_cell_index(cell));
                this.set_dirty();
                return cell;
            };
        };
        return cell;
    };


    Notebook.prototype.insert_cell_above = function (type, index) {
        // type = ('code','html','markdown')
        // index = cell index or undefined to insert above selected
        index = this.index_or_selected(index);
        var cell = null;
        if (this.ncells() === 0 || this.is_valid_cell_index(index)) {
            if (type === 'code') {
                cell = new IPython.CodeCell(this);
                cell.set_input_prompt();
            } else if (type === 'markdown') {
                cell = new IPython.MarkdownCell(this);
            } else if (type === 'html') {
                cell = new IPython.HTMLCell(this);
            } else if (type === 'raw') {
                cell = new IPython.RawCell(this);
            } else if (type === 'heading') {
                cell = new IPython.HeadingCell(this);
            };
            if (cell !== null) {
                if (this.ncells() === 0) {
                    this.element.find('div.end_space').before(cell.element);
                } else if (this.is_valid_cell_index(index)) {
                    this.get_cell_element(index).before(cell.element);
                };
                cell.render();
                this.select(this.find_cell_index(cell));
                this.set_dirty();
                return cell;
            };
        };
        return cell;
    };


    Notebook.prototype.to_code = function (index) {
        var i = this.index_or_selected(index);
        if (this.is_valid_cell_index(i)) {
            var source_element = this.get_cell_element(i);
            var source_cell = source_element.data("cell");
            if (!(source_cell instanceof IPython.CodeCell)) {
                target_cell = this.insert_cell_below('code',i);
                var text = source_cell.get_text();
                if (text === source_cell.placeholder) {
                    text = '';
                }
                target_cell.set_text(text);
                source_element.remove();
                this.set_dirty();
            };
        };
    };


    Notebook.prototype.to_markdown = function (index) {
        var i = this.index_or_selected(index);
        if (this.is_valid_cell_index(i)) {
            var source_element = this.get_cell_element(i);
            var source_cell = source_element.data("cell");
            if (!(source_cell instanceof IPython.MarkdownCell)) {
                target_cell = this.insert_cell_below('markdown',i);
                var text = source_cell.get_text();
                if (text === source_cell.placeholder) {
                    text = '';
                };
                // The edit must come before the set_text.
                target_cell.edit();
                target_cell.set_text(text);
                source_element.remove();
                this.set_dirty();
            };
        };
    };


    Notebook.prototype.to_html = function (index) {
        var i = this.index_or_selected(index);
        if (this.is_valid_cell_index(i)) {
            var source_element = this.get_cell_element(i);
            var source_cell = source_element.data("cell");
            var target_cell = null;
            if (!(source_cell instanceof IPython.HTMLCell)) {
                target_cell = this.insert_cell_below('html',i);
                var text = source_cell.get_text();
                if (text === source_cell.placeholder) {
                    text = '';
                };
                // The edit must come before the set_text.
                target_cell.edit();
                target_cell.set_text(text);
                source_element.remove();
                this.set_dirty();
            };
        };
    };


    Notebook.prototype.to_raw = function (index) {
        var i = this.index_or_selected(index);
        if (this.is_valid_cell_index(i)) {
            var source_element = this.get_cell_element(i);
            var source_cell = source_element.data("cell");
            var target_cell = null;
            if (!(source_cell instanceof IPython.RawCell)) {
                target_cell = this.insert_cell_below('raw',i);
                var text = source_cell.get_text();
                if (text === source_cell.placeholder) {
                    text = '';
                };
                // The edit must come before the set_text.
                target_cell.edit();
                target_cell.set_text(text);
                source_element.remove();
                this.set_dirty();
            };
        };
    };


    Notebook.prototype.to_heading = function (index, level) {
        level = level || 1;
        var i = this.index_or_selected(index);
        if (this.is_valid_cell_index(i)) {
            var source_element = this.get_cell_element(i);
            var source_cell = source_element.data("cell");
            var target_cell = null;
            if (source_cell instanceof IPython.HeadingCell) {
                source_cell.set_level(level);
            } else {
                target_cell = this.insert_cell_below('heading',i);
                var text = source_cell.get_text();
                if (text === source_cell.placeholder) {
                    text = '';
                };
                // The edit must come before the set_text.
                target_cell.set_level(level);
                target_cell.edit();
                target_cell.set_text(text);
                source_element.remove();
                this.set_dirty();
            };
            $([IPython.events]).trigger('selected_cell_type_changed.Notebook',
                {'cell_type':'heading',level:level}
            );
        };
    };


    // Cut/Copy/Paste

    Notebook.prototype.enable_paste = function () {
        var that = this;
        if (!this.paste_enabled) {
            $('#paste_cell').removeClass('ui-state-disabled')
                .on('click', function () {that.paste_cell();});
            $('#paste_cell_above').removeClass('ui-state-disabled')
                .on('click', function () {that.paste_cell_above();});
            $('#paste_cell_below').removeClass('ui-state-disabled')
                .on('click', function () {that.paste_cell_below();});
            this.paste_enabled = true;
        };
    };


    Notebook.prototype.disable_paste = function () {
        if (this.paste_enabled) {
            $('#paste_cell').addClass('ui-state-disabled').off('click');
            $('#paste_cell_above').addClass('ui-state-disabled').off('click');
            $('#paste_cell_below').addClass('ui-state-disabled').off('click');
            this.paste_enabled = false;
        };
    };


    Notebook.prototype.cut_cell = function () {
        this.copy_cell();
        this.delete_cell();
    }

    Notebook.prototype.copy_cell = function () {
        var cell = this.get_selected_cell();
        this.clipboard = cell.toJSON();
        this.enable_paste();
    };


    Notebook.prototype.paste_cell = function () {
        if (this.clipboard !== null && this.paste_enabled) {
            var cell_data = this.clipboard;
            var new_cell = this.insert_cell_above(cell_data.cell_type);
            new_cell.fromJSON(cell_data);
            old_cell = this.get_next_cell(new_cell);
            this.delete_cell(this.find_cell_index(old_cell));
            this.select(this.find_cell_index(new_cell));
        };
    };


    Notebook.prototype.paste_cell_above = function () {
        if (this.clipboard !== null && this.paste_enabled) {
            var cell_data = this.clipboard;
            var new_cell = this.insert_cell_above(cell_data.cell_type);
            new_cell.fromJSON(cell_data);
        };
    };


    Notebook.prototype.paste_cell_below = function () {
        if (this.clipboard !== null && this.paste_enabled) {
            var cell_data = this.clipboard;
            var new_cell = this.insert_cell_below(cell_data.cell_type);
            new_cell.fromJSON(cell_data);
        };
    };


    // Split/merge

    Notebook.prototype.split_cell = function () {
        // Todo: implement spliting for other cell types.
        var cell = this.get_selected_cell();
        if (cell.is_splittable()) {
            texta = cell.get_pre_cursor();
            textb = cell.get_post_cursor();
            if (cell instanceof IPython.CodeCell) {
                cell.set_text(texta);
                var new_cell = this.insert_cell_below('code');
                new_cell.set_text(textb);
            } else if (cell instanceof IPython.MarkdownCell) {
                cell.set_text(texta);
                cell.render();
                var new_cell = this.insert_cell_below('markdown');
                new_cell.edit(); // editor must be visible to call set_text
                new_cell.set_text(textb);
                new_cell.render();
            } else if (cell instanceof IPython.HTMLCell) {
                cell.set_text(texta);
                cell.render();
                var new_cell = this.insert_cell_below('html');
                new_cell.edit(); // editor must be visible to call set_text
                new_cell.set_text(textb);
                new_cell.render();
            };
        };
    };


    Notebook.prototype.merge_cell_above = function () {
        var index = this.get_selected_index();
        var cell = this.get_cell(index);
        if (index > 0) {
            upper_cell = this.get_cell(index-1);
            upper_text = upper_cell.get_text();
            text = cell.get_text();
            if (cell instanceof IPython.CodeCell) {
                cell.set_text(upper_text+'\n'+text);
            } else if (cell instanceof IPython.MarkdownCell || cell instanceof IPython.HTMLCell) {
                cell.edit();
                cell.set_text(upper_text+'\n'+text);
                cell.render();
            };
            this.delete_cell(index-1);
            this.select(this.find_cell_index(cell));
        };
    };


    Notebook.prototype.merge_cell_below = function () {
        var index = this.get_selected_index();
        var cell = this.get_cell(index);
        if (index < this.ncells()-1) {
            lower_cell = this.get_cell(index+1);
            lower_text = lower_cell.get_text();
            text = cell.get_text();
            if (cell instanceof IPython.CodeCell) {
                cell.set_text(text+'\n'+lower_text);
            } else if (cell instanceof IPython.MarkdownCell || cell instanceof IPython.HTMLCell) {
                cell.edit();
                cell.set_text(text+'\n'+lower_text);
                cell.render();
            };
            this.delete_cell(index+1);
            this.select(this.find_cell_index(cell));
        };
    };


    // Cell collapsing and output clearing

    Notebook.prototype.collapse = function (index) {
        var i = this.index_or_selected(index);
        this.get_cell(i).collapse();
    };


    Notebook.prototype.expand = function (index) {
        var i = this.index_or_selected(index);
        this.get_cell(i).expand();
    };


    Notebook.prototype.toggle_output = function (index) {
        var i = this.index_or_selected(index);
        this.get_cell(i).toggle_output();
    };
    
    Notebook.prototype.toggle_input = function (index) {
        var i = this.index_or_selected(index);
        this.get_cell(i).toggle_input();
    };

    Notebook.prototype.set_timebeforetooltip = function (time) {
        this.time_before_tooltip = time;
    };


    Notebook.prototype.set_tooltipontab = function (state) {
        this.tooltip_on_tab = state;
    };


    Notebook.prototype.set_smartcompleter = function (state) {
        this.smart_completer = state;
    };


    Notebook.prototype.clear_all_output = function () {
        var ncells = this.ncells();
        var cells = this.get_cells();
        for (var i=0; i<ncells; i++) {
            if (cells[i] instanceof IPython.CodeCell) {
                cells[i].clear_output(true,true,true);
                // Make all In[] prompts blank, as well
                // TODO: make this configurable (via checkbox?)
                cells[i].set_input_prompt();
            }
        };
    };

    Notebook.prototype.all_cell_visibility = function(input, output) {
    	var ncells = this.ncells();
        var cells = this.get_cells();
        for (var i=0; i<ncells; i++) {
            if (cells[i] instanceof IPython.CodeCell) {
                cells[i].show_sections(input, output);
            }
        };
    }


    // Other cell functions: line numbers, ...

    Notebook.prototype.cell_toggle_line_numbers = function() {
        this.get_selected_cell().toggle_line_numbers();
    };

    // Kernel related things

    Notebook.prototype.start_kernel = function () {
        this.kernel = new IPython.Kernel();
        this.kernel.start(this.notebook_id, $.proxy(this.kernel_started, this));
    };


    Notebook.prototype.restart_kernel = function () {
        var that = this;
        var dialog = $('<div/>');
        dialog.html('Do you want to restart the current kernel?  You will lose all variables defined in it.');
        $(document).append(dialog);
        dialog.dialog({
            resizable: false,
            modal: true,
            closeOnEscape: true,
            title: "Restart kernel or continue running?",
            closeText: '',
            close: function() {$(this).dialog('destroy').remove();},
            buttons : {
                "Restart": function () {
                    that.kernel.restart($.proxy(that.kernel_started, that));
                    that.clear_all_output();
                    that.all_cell_visibility(true);
                    $(this).dialog('close');
                },
                "Continue running": function () {
                    $(this).dialog('close');
                }
            }
        });
    };


    Notebook.prototype.kernel_started = function () {
        console.log("Kernel started: ", this.kernel.kernel_id);
        var ncells = this.ncells();
        var cells = this.get_cells();
        for (var i=0; i<ncells; i++) {
           cells[i].element.removeClass("running");
        };
        this.kernel.shell_channel.onmessage = $.proxy(this.handle_shell_reply,this);
        this.kernel.iopub_channel.onmessage = $.proxy(this.handle_iopub_reply,this);
    	/* TODO: Either use AMD for all .js, or statically load observable... */
        curl(['observable'], function(obs) {obs.start();}) //TODO: don't really want to start this if there is no observable content on the page... 
    };


    Notebook.prototype.handle_shell_reply = function (e) {
        reply = $.parseJSON(e.data);
        var header = reply.header;
        var content = reply.content;
        var msg_type = header.msg_type;
        var msg_id = reply.parent_header.msg_id
        var cell = this.cell_for_msg(msg_id, this.msg_cell_map);
        if (msg_type === "execute_reply") {
            cell.set_input_prompt(content.execution_count);
            cell.element.removeClass("running");
            delete this.msg_cell_map[msg_id]
        } else if (msg_type === "complete_reply") {
            cell.finish_completing(content.matched_text, content.matches);
            delete this.msg_cell_map[msg_id]
        } else if (msg_type === "object_info_reply"){
            rep = reply.content;
            if(rep.found) {
                cell.finish_tooltip(rep, reply.parent_header.msg_id);
            }
            delete this.msg_cell_map[msg_id]
        } else if (msg_type === "observable_update") {
            this.update_observable(reply.content);
        } else {
          //console.log("unknown reply:"+msg_type);
        }
        // when having a rely from object_info_reply,
        // no payload so no need to handle it
        if(typeof(content.payload)!='undefined') {
            var payload = content.payload || [];
            this.handle_payload(cell, payload);
        }
    };


    Notebook.prototype.handle_payload = function (cell, payload) {
        var l = payload.length;
        for (var i=0; i<l; i++) {
            if (payload[i].source === 'IPython.zmq.page.page') {
                if (payload[i].text.trim() !== '') {
                    IPython.pager.clear();
                    IPython.pager.expand();
                    IPython.pager.append_text(payload[i].text);
                }
            } else if (payload[i].source === 'IPython.zmq.zmqshell.ZMQInteractiveShell.set_next_input') {
                var index = this.find_cell_index(cell);
                var new_cell = this.insert_cell_below('code',index);
                new_cell.set_text(payload[i].text);
                this.set_dirty();
            }
        };
    };


    Notebook.prototype.handle_iopub_reply = function (e) {
        reply = $.parseJSON(e.data);
        var content = reply.content;
        // console.log(reply);
        var msg_type = reply.header.msg_type;
        var msg_id = reply.parent_header.msg_id
        var cell = this.cell_for_msg(msg_id, this.io_cell_map);
        if (msg_type !== 'status' && !cell){
            // message not from this notebook, but should be attached to a cell
            // console.log("Received IOPub message not caused by one of my cells");
            // console.log(reply);
            return;
        }
        var output_types = ['stream','display_data','pyout','pyerr','pyincomplete'];
        if (output_types.indexOf(msg_type) >= 0) {
            this.handle_output(cell, msg_type, content);
        } else if (msg_type === 'status') {
            if (content.execution_state === 'busy') {
                $([IPython.events]).trigger('status_busy.Kernel');
            } else if (content.execution_state === 'idle') {
                $([IPython.events]).trigger('status_idle.Kernel');
                delete this.io_cell_map[msg_id]
            } else if (content.execution_state === 'dead') {
                this.handle_status_dead();
            };
        } else if (msg_type === 'clear_output') {
            cell.clear_output(content.stdout, content.stderr, content.other);
        };
    };


    Notebook.prototype.handle_status_dead = function () {
        var that = this;
        this.kernel.stop_channels();
        var dialog = $('<div/>');
        dialog.html('The kernel has died, would you like to restart it? If you do not restart the kernel, you will be able to save the notebook, but running code will not work until the notebook is reopened.');
        $(document).append(dialog);
        dialog.dialog({
            resizable: false,
            modal: true,
            closeOnEscape: true,
            title: "Dead kernel",
            close: function() {$(this).dialog('destroy').remove();},
            buttons : {
                "Restart": function () {
                    that.start_kernel();
                    $(this).dialog('close');
                },
                "Continue running": function () {
                    $(this).dialog('close');
                }
            }
        });
    };


    Notebook.prototype.handle_output = function (cell, msg_type, content) {
        var json = {};
        json.output_type = msg_type;
        if (msg_type === "stream") {
            json.text = content.data;
            json.stream = content.name;
        } else if (msg_type === "display_data") {
            json = this.convert_mime_types(json, content.data);
        } else if (msg_type === "pyout") {
            json.prompt_number = content.execution_count;
            json = this.convert_mime_types(json, content.data);
        } else if (msg_type === "pyerr") {
            json.prompt_number = content.execution_count;
            json.ename = content.ename;
            json.evalue = content.evalue;
            json.traceback = content.traceback;
        } else if (msg_type === "pyincomplete") {
            // We don't need anything special here (the json.output_type
            // is what's used to determine that a pyincomplete happened).
            // Including this branch in case we decide to do clever things
            // in the future, like highlight the first unmatched paren.
        };
        // append with dynamic=true
        cell.append_output(json, true);
        //this.set_dirty();
    };


    Notebook.prototype.convert_mime_types = function (json, data) {
        if (data['text/plain'] !== undefined) {
            json.text = data['text/plain'];
        };
        if (data['text/html'] !== undefined) {
            json.html = data['text/html'];
        };
        if (data['image/svg+xml'] !== undefined) {
            json.svg = data['image/svg+xml'];
        };
        if (data['image/png'] !== undefined) {
            json.png = data['image/png'];
        };
        if (data['image/jpeg'] !== undefined) {
            json.jpeg = data['image/jpeg'];
        };
        if (data['text/latex'] !== undefined) {
            json.latex = data['text/latex'];
        };
        if (data['application/json'] !== undefined) {
            json.json = data['application/json'];
        };
        if (data['application/javascript'] !== undefined) {
            json.javascript = data['application/javascript'];
        }
        return json;    
    };


    Notebook.prototype.execute_selected_cell = function (options) {
        // add_new: should a new cell be added if we are at the end of the nb
        // terminal: execute in terminal mode, which stays in the current cell
    	// hideInput: true to hide input when execution completes
        default_options = {terminal: false, add_new: true, hideInput: true};
        $.extend(default_options, options);
        var that = this;
        var cell = this.get_selected_cell();
        var cell_index = that.find_cell_index(cell);
        if (cell instanceof IPython.CodeCell) {
            cell.clear_output(true, true, true);
            var code = cell.get_text();
            if (/\S/.test(code)) { //Only execute if there is some content...
               cell.hideInputOnResult = default_options.hideInput;
               cell.set_input_prompt('*');
               cell.element.addClass("running");
               this.clear_cell_msgs(cell.cell_id, this.msg_cell_map)
               this.clear_cell_msgs(cell.cell_id, this.io_cell_map)
               var msg_id = this.kernel.execute(cell.get_text());
               this.msg_cell_map[msg_id] = cell.cell_id;
               this.io_cell_map[msg_id] = cell.cell_id;
            }
        } else if (cell instanceof IPython.HTMLCell) {
            cell.render();
        }
        if (default_options.terminal) {
            cell.select_all();
        } else {
            if ((cell_index === (that.ncells()-1)) && default_options.add_new) {
                that.insert_cell_below('code');
                // If we are adding a new cell at the end, scroll down to show it.
                that.scroll_to_bottom();
            } else {
                that.select(cell_index+1);
            };
        };
    };

    Notebook.prototype.execute_cells_from = function (index) {
    	var i = this.index_or_selected(index);
        var ncells = this.ncells();
        for (; i<ncells; i++) {
            this.select(i);
            this.execute_selected_cell({add_new:false, hideInput: false});
        };
    };

    Notebook.prototype.execute_all_cells = function () {
        var ncells = this.ncells();
        for (var i=0; i<ncells; i++) {
            this.select(i);
            this.execute_selected_cell({add_new:false});
        };
        this.scroll_to_bottom();
    };


    Notebook.prototype.request_tool_tip = function (cell,func) {
        // Feel free to shorten this logic if you are better
        // than me in regEx
        // basicaly you shoul be able to get xxx.xxx.xxx from 
        // something(range(10), kwarg=smth) ; xxx.xxx.xxx( firstarg, rand(234,23), kwarg1=2, 
        // remove everything between matchin bracket (need to iterate)
        matchBracket = /\([^\(\)]+\)/g;
        oldfunc = func;
        func = func.replace(matchBracket,"");
        while( oldfunc != func )
        {
        oldfunc = func;
        func = func.replace(matchBracket,"");
        }
        // remove everythin after last open bracket
        endBracket = /\([^\(]*$/g;
        func = func.replace(endBracket,"");
        var re = /[a-zA-Z._]+$/g;
        var msg_id = this.kernel.object_info_request(re.exec(func));
        if(typeof(msg_id)!='undefined'){
            this.msg_cell_map[msg_id] = cell.cell_id;
            return msg_id;
        }
        return;
    };

    Notebook.prototype.complete_cell = function (cell, line, cursor_pos) {
        var msg_id = this.kernel.complete(line, cursor_pos);
        this.msg_cell_map[msg_id] = cell.cell_id;
    };

    Notebook.prototype.propagate_dom_change = function (obs_id, new_dom_value) {
        // TODO
        // this.kernel.
    };


    // Persistance and loading

    Notebook.prototype.get_user_save_timestamp = function () {
        return this.user_save_timestamp;
    }

    Notebook.prototype.get_auto_save_timestamp = function () {
        return this.auto_save_timestamp;
    }

    Notebook.prototype.get_notebook_id = function () {
        return this.notebook_id;
    };


    Notebook.prototype.get_notebook_name = function () {
        return this.notebook_name;
    };


    Notebook.prototype.set_notebook_name = function (name) {
        this.notebook_name = name;
    };


    Notebook.prototype.test_notebook_name = function (nbname) {
        nbname = nbname || '';
        if (this.notebook_name_blacklist_re.test(nbname) == false && nbname.length>0) {
            return true;
        } else {
            return false;
        };
    };

    Notebook.prototype.revert_to_last_saved = function() {
       var nb = this.toJSON();
       nb.worksheets = this.saved_worksheets;
       this.fromJSON(nb);
    }


    Notebook.prototype.fromJSON = function (data) {
        var ncells = this.ncells();
        var i;
        for (i=0; i<ncells; i++) {
            // Always delete cell 0 as they get renumbered as they are deleted.
            this.delete_cell(0);
        };
        // Save the metadata and name.
        this.metadata = data.metadata;

        if (data.metadata.user_save_timestamp === undefined) {
            // an old notebook file - we can't easily get the notebook file date, so we set the value to
            // undefined, which will cause SaveWidget.prototype.set_last_saved to show "unknown" save time
            this.user_save_timestamp = undefined;
        } else {
            var msecs = Date.parse(data.metadata.user_save_timestamp);
            if (msecs == 0) {
                this.user_save_timestamp = undefined;
            } else {
                this.user_save_timestamp = new Date(msecs);
            }
        }

        if (data.metadata.auto_save_timestamp === undefined) {
            this.auto_save_timestamp = undefined;
        } else {
            this.auto_save_timestamp = new Date(Date.parse(data.metadata.auto_save_timestamp));
        }

        this.notebook_name = data.metadata.name;

        // Only handle 1 worksheet for now.
        this.saved_worksheets = data.worksheets
        var worksheet = data.worksheets[0];
        if (data.autosaved && data.autosaved[0]) worksheet = data.autosaved[0];
        if (worksheet !== undefined) {
            var new_cells = worksheet.cells;
            ncells = new_cells.length;
            var cell_data = null;
            var new_cell = null;
            var oldSetDirty = this.set_dirty;//disable autosave
            this.set_dirty = function() {};
            for (i=0; i<ncells; i++) {
                cell_data = new_cells[i];
                // VERSIONHACK: plaintext -> raw
                // handle never-released plaintext name for raw cells
                if (cell_data.cell_type === 'plaintext'){
                    cell_data.cell_type = 'raw';
                }
                new_cell = this.insert_cell_below(cell_data.cell_type);
                new_cell.fromJSON(cell_data);
            };
            this.set_dirty = oldSetDirty; //restore autosave
        };
        if (this.ncells() === 0) {
           this.insert_cell_below('code');
        };
        this.clear_dirty();
    };


    Notebook.prototype.toJSON = function () {
        var cells = this.get_cells();
        var ncells = cells.length;
        cell_array = new Array(ncells);
        for (var i=0; i<ncells; i++) {
            cell_array[i] = cells[i].toJSON();
        };
        data = {
            // Only handle 1 worksheet for now.
            worksheets : [{cells:cell_array}],
            metadata : this.metadata
        };

        data.metadata.name = this.notebook_name;
        data.metadata.user_save_timestamp = this.user_save_timestamp;
        data.metadata.auto_save_timestamp = this.auto_save_timestamp;
        data.nbformat = this.nbformat;

        // Stripping out stdout and generated HTML, since we only want to save the expressions.
        // One reason to do this is that there seems to be some bug associated with receiving
        // PUTs above a certain size. Fix that and then maybe we can talk about saving HTML and
        // output.
        for (var i = 0; i < data.worksheets.length; i++) {
           var worksheet = data.worksheets[i];
           for (var j = 0; j < worksheet.cells.length; j++) {
              var cell = worksheet.cells[j];
              cell.outputs = [];
           }
        }
        return data;
    };

    Notebook.prototype._save_notebook_data = function(data, overwrite, success_fcn, error_fcn, sync) {
        // We do the call with settings so we can set cache to false.
        var settings = {
            processData : false,
            cache : false,
            type : "PUT",
            data : JSON.stringify(data),
            headers : {'Content-Type': 'application/json'},
            success : success_fcn,
            error : error_fcn
        };
        if (sync) settings.async = false
        $([IPython.events]).trigger('notebook_saving.Notebook');
        var force = new Boolean(overwrite)
        var url = $('body').data('baseProjectUrl') + 'notebooks/' + encodeURIComponent(this.notebook_name) + '?id=' + this.notebook_id + "&force=" + force;
        $.ajax(url, settings);
    }

    Notebook.prototype.save_notebook = function (overwrite) {
        this.clear_dirty();
        this.user_save_timestamp = new Date();
        var data = this.toJSON()
        this.saved_worksheets = data.worksheets;
        this._save_notebook_data(data, overwrite, $.proxy(this.save_notebook_success,this), $.proxy(this.save_notebook_error,this));
    };

    Notebook.prototype.auto_save_notebook = function (sync) {
        this.clear_dirty();
        this.auto_save_timestamp = new Date();
        var data = this.toJSON();
        data.autosaved = data.worksheets
        data.worksheets = this.saved_worksheets
        this._save_notebook_data(data, true, $.proxy(this.auto_save_notebook_success,this), $.proxy(this.auto_save_notebook_error,this), sync);
    };

    Notebook.prototype.set_dirty = function() {
       if (! this.autosave_timer && this.auto_save_time > 0)
          this.autosave_timer = setTimeout($.proxy(this.auto_save_notebook, this), this.auto_save_time);
    }

    Notebook.prototype.clear_dirty = function() {
       if (this.autosave_timer) {
          clearTimeout(this.autosave_timer);
          this.autosave_timer = undefined;
       }
    }


    Notebook.prototype.load_notebook = function (notebook_name, notebook_id) {
        var that = this;
        this.notebook_id = notebook_id;
        // We do the call with settings so we can set cache to false.
        var settings = {
            processData : false,
            cache : false,
            type : "GET",
            dataType : "json",
            success : $.proxy(this.load_notebook_success,this),
            error : $.proxy(this.load_notebook_error,this)
        };
        $([IPython.events]).trigger('notebook_loading.Notebook');
        var url = $('body').data('baseProjectUrl') + 'notebooks/' + encodeURIComponent(notebook_name) + '?id=' + this.notebook_id;
        $.ajax(url, settings);
    };

    

    Notebook.prototype.save_notebook_success = function (data, status, xhr) {
        $([IPython.events]).trigger('notebook_saved.Notebook');
    };

    Notebook.prototype.auto_save_notebook_success = function (data, status, xhr) {
        $([IPython.events]).trigger('notebook_auto_saved.Notebook');
    };


    Notebook.prototype.save_notebook_error = function (xhr, status, error_msg) {
        if (xhr.status === 409) {
           var that = this
           msg = "Notebook named " + this.notebook_name + " already exists, overwrite?";
           var dialog = $('<div/>');
           dialog.html(msg);
           this.element.append(dialog);
           dialog.dialog({
              resizable: false,
              modal: true,
              title: "Error saving notebook",
              closeText: "",
              closeOnEscape: true,
              close: function() {$(this).dialog('destroy').remove();},
              buttons : {
                 "OK": function () {
                    that.save_notebook(true);
                    $(this).dialog('close');
                 },
                 "Cancel": function () {
                    $(this).dialog('close');
                    $([IPython.events]).trigger('notebook_save_failed.Notebook');
                 }
              },
              width: 400
           });
        } else {
           $([IPython.events]).trigger('notebook_save_failed.Notebook');
        }
    };

    Notebook.prototype.auto_save_notebook_error = function (data, status, xhr) {
        $([IPython.events]).trigger('notebook_save_failed.Notebook');
    };

    Notebook.prototype.load_notebook_success = function (data, status, xhr) {
        this.fromJSON(data);
        if (! this.read_only) {
            this.start_kernel();
        }
        this.select(0);
        this.scroll_to_top();
        $([IPython.events]).trigger('notebook_loaded.Notebook');
    };


    Notebook.prototype.load_notebook_error = function (xhr, textStatus, errorThrow) {
       msg = "An error occurred while loading this notebook: " + xhr.responseText;
       var dialog = $('<div/>');
       dialog.html(msg);
       this.element.append(dialog);
       dialog.dialog({
           resizable: false,
           modal: true,
           closeOnEscape: true,
           title: "Error loading notebook",
           closeText: "",
           close: function() {$(this).dialog('destroy').remove();},
           buttons : {
               "OK": function () {
                   $(this).dialog('close');
               }
           },
           width: 400
       });
    }

    IPython.Notebook = Notebook;


    return IPython;

}(IPython));

