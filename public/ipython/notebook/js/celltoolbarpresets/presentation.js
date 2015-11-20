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
            // we check that the presentation namespace exist and create it if needed
            if (cell.metadata.presentation === undefined) {
                cell.metadata.presentation = {};
            }
            // set the value
            cell.metadata.presentation.cell_width = value;

            cell.render();
        },
        //geter
        function (cell) {
            var ns = cell.metadata.presentation;
            // if the presentation namespace does not exist return `undefined`
            // (will be interpreted as `false` by checkbox) otherwise
            // return the value
            return (ns === undefined) ? undefined : ns.cell_width;
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
