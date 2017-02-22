// Copyright (c) IPython Development Team.
// Distributed under the terms of the Modified BSD License.

define([
    'jquery',
    'notebook/js/celltoolbar',
], function ($, celltoolbar) {
    "use strict";

    var CellToolbar = celltoolbar.CellToolbar;
    var presentation_preset = [];

    var select_width = CellToolbar.utils.select_ui_generator([
            ["Full", "12"],
            ["3/4", "9"],
            ["2/4", "6"],
            ["1/4", "3"],
        ],
        // setter
        function (cell, value) {
            cell.set_cell_width(value);
        },
        //geter
        function (cell) {
          return cell.get_cell_width();
        },
        "Cell width");

    var register = function (notebook) {
        CellToolbar.register_callback('presentation.select', select_width);
        presentation_preset.push('presentation.select');

        CellToolbar.register_preset('Width (when multi-column)', presentation_preset, notebook);
        console.log('Cell width preset registered.');
    };
    return {'register': register};
});
