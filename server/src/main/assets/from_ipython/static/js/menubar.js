//----------------------------------------------------------------------------
//  Copyright (C) 2008-2011  The IPython Development Team
//
//  Distributed under the terms of the BSD License.  The full license is in
//  the file COPYING, distributed as part of this software.
//----------------------------------------------------------------------------

//============================================================================
// MenuBar
//============================================================================

var IPython = (function (IPython) {

    var MenuBar = function (selector) {
        this.selector = selector;
        if (this.selector !== undefined) {
            this.element = $(selector);
            this.style();
            this.bind_events();
        }
    };


    MenuBar.prototype.style = function () {
        $('#ribbon > ul > li > a').click(function () {
            $('#ribbon .selected').removeClass('selected');
            $(this).parent().addClass('selected');
        });
    };


    MenuBar.prototype.bind_events = function () {
        //  File
        this.element.find('#new_notebook > a').click(function () {
            var url = $('body').data('baseProjectUrl')+'new'
            IPython.CSRF.postAction(url)
        });
        this.element.find('#open_notebook > a').click(function () {
            window.open($('body').data('baseProjectUrl'));
        });
        this.element.find('#rename_notebook > a').click(function () {
            IPython.save_widget.rename_notebook();
        });
        this.element.find('#copy_notebook > a').click(function () {
            var notebook_id = IPython.notebook.get_notebook_id();
            var notebook_name = IPython.notebook.get_notebook_name();
            var url = $('body').data('baseProjectUrl') + 'copy/' + encodeURIComponent(notebook_name) + '?id=' + notebook_id;
            IPython.CSRF.postAction(url)
            //window.open(url,'_newtab');
        });
        this.element.find('#save_notebook > a').click(function () {
            IPython.notebook.save_notebook(true);
        });
        this.element.find('#revert_notebook > a').click(function () {
        	IPython.notebook.revert_to_last_saved();
        });
        this.element.find('#download_ipynb > a').click(function () {
            var notebook_id = IPython.notebook.get_notebook_id();
            var notebook_name = IPython.notebook.get_notebook_name();
            var url = $('body').data('baseProjectUrl') + 'notebooks/' + encodeURIComponent(notebook_name) + '?id=' + notebook_id + '&format=json';
            IPython.CSRF.postAction(url)
            //window.open(url,'_newtab');
        });
        this.element.find('#download_py > a').click(function () {
            var notebook_id = IPython.notebook.get_notebook_id();
            var url = $('body').data('baseProjectUrl') + 'notebooks/' + notebook_id + '?format=scala';
            window.open(url,'_newtab');
        });
        this.element.find('button#print_notebook > a').click(function () {
            IPython.print_widget.print_notebook();
        });
        // Edit
        this.element.find('#cut_cell > a').click(function () {
            IPython.notebook.cut_cell();
        });
        this.element.find('#copy_cell > a').click(function () {
            IPython.notebook.copy_cell();
        });
        this.element.find('#delete_cell > a').click(function () {
            IPython.notebook.delete_cell();
        });
        this.element.find('#split_cell > a').click(function () {
            IPython.notebook.split_cell();
        });
        this.element.find('#merge_cell_above > a').click(function () {
            IPython.notebook.merge_cell_above();
        });
        this.element.find('#merge_cell_below > a').click(function () {
            IPython.notebook.merge_cell_below();
        });
        this.element.find('#move_cell_up > a').click(function () {
            IPython.notebook.move_cell_up();
        });
        this.element.find('#move_cell_down > a').click(function () {
            IPython.notebook.move_cell_down();
        });
        this.element.find('#select_previous > a').click(function () {
            IPython.notebook.select_prev();
        });
        this.element.find('#select_next > a').click(function () {
            IPython.notebook.select_next();
        });
        // View
        this.element.find('#toggle_header > a').click(function () {
            IPython.layout_manager.toggle_header();
        });
        this.element.find('#toggle_toolbar > a').click(function () {
            IPython.toolbar.toggle();
        });
        // Insert
        this.element.find('#insert_cell_above > a').click(function () {
            IPython.notebook.insert_cell_above('code');
        });
        this.element.find('#insert_cell_below > a').click(function () {
            IPython.notebook.insert_cell_below('code');
        });
        // Cell
        this.element.find('#run_cell > a').click(function () {
            IPython.notebook.execute_selected_cell();
        });
        this.element.find('#run_cell_in_place > a').click(function () {
            IPython.notebook.execute_selected_cell({terminal:true});
        });
        this.element.find('#run_all_cells > a').click(function () {
            IPython.notebook.execute_all_cells();
        });
        this.element.find('#to_code > a').click(function () {
            IPython.notebook.to_code();
        });
        this.element.find('#to_markdown > a').click(function () {
            IPython.notebook.to_markdown();
        });
        this.element.find('#to_raw > a').click(function () {
            IPython.notebook.to_raw();
        });
        this.element.find('#to_heading1 > a').click(function () {
            IPython.notebook.to_heading(undefined, 1);
        });
        this.element.find('#to_heading2 > a').click(function () {
            IPython.notebook.to_heading(undefined, 2);
        });
        this.element.find('#to_heading3 > a').click(function () {
            IPython.notebook.to_heading(undefined, 3);
        });
        this.element.find('#to_heading4 > a').click(function () {
            IPython.notebook.to_heading(undefined, 4);
        });
        this.element.find('#to_heading5 > a').click(function () {
            IPython.notebook.to_heading(undefined, 5);
        });
        this.element.find('#to_heading6 > a').click(function () {
            IPython.notebook.to_heading(undefined, 6);
        });
//        this.element.find('#clear_all_output > a').click(function () {
//            IPython.notebook.clear_all_output();
//        });
        this.element.find('#input_output_all > a').click(function () {
        	IPython.notebook.all_cell_visibility(true, true);
        });
        this.element.find('#output_only_all > a').click(function () {
            IPython.notebook.all_cell_visibility(false, true);
        });
        this.element.find('#input_only_all > a').click(function () {
        	IPython.notebook.all_cell_visibility(true, false);
        });
        // Kernel
        this.element.find('#int_kernel > a').click(function () {
            IPython.notebook.kernel.interrupt();
        });
        this.element.find('#restart_kernel > a').click(function () {
            IPython.notebook.restart_kernel();
        });
        // Help
        this.element.find('#keyboard_shortcuts > a').click(function () {
            IPython.quick_help.show_keyboard_shortcuts(IPython.keybindings.bindings);
        });
    };
    

    IPython.MenuBar = MenuBar;

    return IPython;

}(IPython));
