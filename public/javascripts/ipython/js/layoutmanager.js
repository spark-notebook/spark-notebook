//----------------------------------------------------------------------------
//  Copyright (C) 2008-2011  The IPython Development Team
//
//  Distributed under the terms of the BSD License.  The full license is in
//  the file COPYING, distributed as part of this software.
//----------------------------------------------------------------------------

//============================================================================
// Layout
//============================================================================

var IPython = (function (IPython) {

    var LayoutManager = function () {
        this.bind_events();
    };


    LayoutManager.prototype.bind_events = function () {
    	var that = this;
        $(window).resize($.proxy(this.do_resize,this));
        $('div#expand_header > a').click($.proxy(this.toggle_header, this));
    };


    LayoutManager.prototype.do_resize = function () {
        var win = $(window);
        var w = win.width();
        var h = win.height();
        var header_height;
        if ($('div#header').css('display') === 'none') {
            header_height = 0;
        } else {
            header_height = $('div#header').outerHeight(true);
        }
        var menubar_height;
        if ($('div#header').css('display') === 'none') {
        	menubar_height = 0;
        } else {
        	menubar_height = $('div#menubar_container').outerHeight(true);
        }

        var app_height = h-header_height-menubar_height;  // content height

        $('div#notebook_panel').height(app_height);  // content+padding+border height
    };

    LayoutManager.prototype.toggle_header = function() {
   	    $('div#expand_header').toggle();
    	$('div#header').toggle();
   	    $('div#menubar_container').toggle();
        this.do_resize();
    } 
    
    IPython.LayoutManager = LayoutManager;

    return IPython;

}(IPython));
