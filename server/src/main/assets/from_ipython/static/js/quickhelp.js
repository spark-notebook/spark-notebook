//----------------------------------------------------------------------------
//  Copyright (C) 2008-2011  The IPython Development Team
//
//  Distributed under the terms of the BSD License.  The full license is in
//  the file COPYING, distributed as part of this software.
//----------------------------------------------------------------------------

//============================================================================
// QuickHelp button
//============================================================================

var IPython = (function (IPython) {

    var QuickHelp = function (selector) {
    };

    /* Bindings is an array of objects with properties 'key' and 'desc' */
    QuickHelp.prototype.show_keyboard_shortcuts = function (bindings) {
        // toggles display of keyboard shortcut dialog
        var that = this;
        if ( this.shortcut_dialog ){
            // if dialog is already shown, close it
            this.shortcut_dialog.dialog("close");
            this.shortcut_dialog = null;
            return;
        }
        var dialog = $('<div/>');
        this.shortcut_dialog = dialog;
        var shortcuts = bindings;
        for (var i=0; i<shortcuts.length; i++) {
            dialog.append($('<div>').
                append($('<span/>').addClass('shortcut_key').html(shortcuts[i].key.replace(/,/g,''))).
                append($('<span/>').addClass('shortcut_descr').html(' : ' + shortcuts[i].desc))
            );
        };
        dialog.dialog({
        	title: 'Keyboard shortcuts', 
        	closeOnEscape: true, 
        	closeText: '', 
        	close: function() {
        		$(this).dialog('destroy').remove();
        		that.shortcut_dialog = null;
        		}
        });
    };

    // Set module variables
    IPython.QuickHelp = QuickHelp;

    return IPython;

}(IPython));
