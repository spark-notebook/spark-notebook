var IPython = (function (IPython) {
	
	var KeyBindings = function(notebook, layoutMgr) {
	   this.nb = notebook;
	   this.lm = layoutMgr;
       this.bind_events();
	}
	
	KeyBindings.prototype.bind_events = function () {
		var that = this;
        curl(['js!jwerty.js!exports=jwerty'], function(kb) {
            that.bindings.forEach(function(binding) {
            	kb.key(binding.key, binding.fcn, that);
            	if (binding.alt) kb.key(binding.alt, binding.fcn, that); //Back compat bindings
            });
            kb.key('esc', false); //Prevent websocket closure
            return true;
        });
	}
	

    /* This is an array to preserve order for keyboard shortcuts dialog */
    // TODO: 'alt' key bindings here for back-compat with 0.1, remove after 0.2 is released.
	KeyBindings.prototype.bindings = [
         { key: 'ctrl+s', desc: "Save Notebook", fcn: function() {this.nb.save_notebook(true); return false;} },
         
         // Navigation
         { key: '↑', desc: "Line Up", 
        	 fcn: function(e) {
           	        if (!e.shiftKey) {
         	        var cell = this.nb.get_selected_cell();
                     if (cell.at_top() || cell.showInput === false) {
                         this.nb.select_prev();
                         return false;
                  };
         	}}},
         { key: '↓', desc: "Line Down",
         		fcn: function(e) {
         	           if (!e.shiftKey) {
         	             var cell = this.nb.get_selected_cell();
                          if (cell.at_bottom() || cell.showInput === false) {
                              this.nb.select_next();
                              return false;
                       };
         	}}},
         { key: 'ctrl+↑', desc: "Cell Up", fcn: function() {this.nb.select_prev(); return false} },
         { key: 'ctrl+↓', desc: "Cell Down", fcn: function() {this.nb.select_next(); return false} },
         
         { key: 'alt+↑', desc: "Move Cell Up", fcn: function() {this.nb.move_cell_up(); return false} },
         { key: 'alt+↓', desc: "Move Cell Down", fcn: function() {this.nb.move_cell_down(); return false} },
         { key: 'ctrl+shift+↑', desc: "Insert Above", fcn: function() {this.nb.insert_cell_above('code'); return false}, alt:'ctrl+m, a'},
         { key: 'ctrl+shift+↓', desc: "Insert Below", fcn: function() {this.nb.insert_cell_below('code'); return false}, alt:'ctrl+m, b' },
         
         { key: 'ctrl+m, x', desc: "Cut Cell", fcn: function() {this.nb.cut_cell(); return false} },
         { key: 'ctrl+m, c', desc: "Copy Cell", fcn: function() {this.nb.copy_cell(); return false} },
         { key: 'ctrl+m, v', desc: "Paste Cell", fcn: function() {this.nb.paste_cell(); return false} },
         
         { key: 'ctrl+m, y', desc: "Code Cell", fcn: function() {this.nb.to_code(); return false} },
         { key: 'ctrl+m, m', desc: "Markdown Cell", fcn: function() {this.nb.to_markdown(); return false} },
         { key: 'ctrl+m, t', desc: "Text Cell", fcn: function() {this.nb.to_raw(); return false} },
         { key: 'ctrl+m, [1-6]', desc: "Header Cell (1-6)", fcn: function(e, jc) {this.nb.to_heading(undefined, parseInt(e.keyCode - 48)); return false}},
         
         { key: 'ctrl+enter', desc: "Run Cell", fcn: function() {this.nb.execute_selected_cell(); return false} },
         { key: 'ctrl+shift+enter', desc: "Run Cell In Place", fcn: function() {this.nb.execute_selected_cell({terminal:true, hideInput: false}); return false} },
         { key: 'alt+enter', desc: "Run All", fcn: function() {this.nb.execute_all_cells(); return false} },
         { key: 'alt+shift+enter', desc: "Run From Selected", fcn: function() {this.nb.execute_cells_from(); return false} },
         
         
         { key: 'alt+o', desc: "Output Only", fcn: function() {this.nb.all_cell_visibility(false, true); return false}},
         { key: 'alt+i', desc: "Input Only", fcn: function() {this.nb.all_cell_visibility(true, false); return false}},
         { key: 'alt+a', desc: "Show All", fcn: function() {this.nb.all_cell_visibility(true, true); return false}},
         { key: 'ctrl+shift+o', desc: "Toggle Output", fcn: function() {this.nb.toggle_output(); return false} , alt:'ctrl+m, o'},
         { key: 'ctrl+shift+i', desc: "Toggle Input", fcn: function() {this.nb.toggle_input(); return false} , alt:'ctrl+m, i'},
         
         { key: 'ctrl+shift+l', desc: "Toggle Line Numbers", fcn: function() {this.nb.cell_toggle_line_numbers(); return false} , alt:'ctrl+m, l'},
         
         { key: 'ctrl+shift+t', desc: "Interrupt Kernel", fcn: function() {this.nb.kernel.interrupt(); return false}},
         { key: 'ctrl+shift+.', desc: "Restart Kernel", fcn: function() {this.nb.restart_kernel(); return false} , alt:'ctrl+m, .'},
         
         { key: 'ctrl+shift+m', desc: "Toggle Menu", fcn: function() {this.lm.toggle_header(); return false; }},
         { key: 'ctrl+shift+h', desc: "Keyboard Shortcuts", fcn: function() {IPython.quick_help.show_keyboard_shortcuts(this.bindings); return false} , alt:'ctrl+m, h'}
    ];

	
	IPython.KeyBindings = KeyBindings;

    return IPython;
}(IPython));