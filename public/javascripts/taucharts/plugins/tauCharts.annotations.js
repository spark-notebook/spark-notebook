(function (factory) {
    if (typeof define === 'function' && define.amd) {
        define(['tauCharts'], function (tauPlugins) {
            return factory(tauPlugins);
        });
    } else if (typeof module === 'object' && module.exports) {
        var tauPlugins = require('tauCharts');
        module.exports = factory(tauPlugins);
    } else {
        factory(this.tauCharts);
    }
})(function (tauCharts) {

    var _ = tauCharts.api._;
    var d3 = tauCharts.api.d3;

    var addToUnits = function (units, newUnit, position) {
        if (position === 'front') {
            units.push(newUnit);
        } else {
            // behind by default
            units.unshift(newUnit);
        }
    };

    function annotations(xSettings) {

        var settings = _.defaults(xSettings || {}, {});
        var textScaleName = 'annotation_text';

        return {

            init: function (chart) {
                this._chart = chart;

                var specRef = chart.getSpec();
                specRef.scales[textScaleName] = {type: 'value', dim: 'text', source: '?'};
                specRef.transformations = specRef.transformations || {};

                specRef.transformations.dataRange = function (data, metaInfo) {

                    var from = metaInfo.from;
                    var to = metaInfo.to;

                    var primaryScaleInfo = chart.getScaleInfo(metaInfo.primaryScale);

                    if ((primaryScaleInfo.scaleType === 'period')) {
                        var periodCaster = tauCharts.api.tickPeriod.get(primaryScaleInfo.period);
                        from = periodCaster.cast(new Date(metaInfo.from));
                        to = periodCaster.cast(new Date(metaInfo.to));
                    }

                    var isX0OutOfDomain = !primaryScaleInfo.isInDomain(from);
                    var isX1OutOfDomain = !primaryScaleInfo.isInDomain(to);

                    var isOutOfDomain = (primaryScaleInfo.discrete ?
                        (isX0OutOfDomain || isX1OutOfDomain) :
                        (isX0OutOfDomain && isX1OutOfDomain)
                    );

                    if (isOutOfDomain) {
                        console.log('Annotation is out of domain');
                        return [];
                    }

                    var secondaryScaleInfo = chart.getScaleInfo(metaInfo.secondaryScale);
                    var secDomain = secondaryScaleInfo.domain();
                    var boundaries = [secDomain[0], secDomain[secDomain.length - 1]];

                    var a = primaryScaleInfo.dim;
                    var b = secondaryScaleInfo.dim;

                    var leftBtm = {};
                    var leftTop = {};
                    var rghtTop = {};
                    var rghtBtm = {};

                    leftBtm[a] = from;
                    leftBtm[b] = boundaries[0];

                    leftTop[a] = from;
                    leftTop[b] = boundaries[1];

                    rghtTop[a] = to;
                    rghtTop[b] = boundaries[1];

                    rghtBtm[a] = to;
                    rghtBtm[b] = boundaries[0];

                    if (metaInfo.axis === 'x') {
                        leftTop.text = metaInfo.text;
                    } else {
                        rghtTop.text = metaInfo.text;
                    }

                    return [leftBtm, leftTop, rghtTop, rghtBtm];
                };

                specRef.transformations.dataLimit = function (data, metaInfo) {

                    var primary = metaInfo.primaryScale;
                    var secondary = metaInfo.secondaryScale;

                    var primaryScaleInfo = chart.getScaleInfo(primary);
                    var from = ((primaryScaleInfo.scaleType === 'period') ?
                        tauCharts.api.tickPeriod.get(primaryScaleInfo.period).cast(new Date(metaInfo.from)) :
                        metaInfo.from);
                    var isOutOfDomain = (!primaryScaleInfo.isInDomain(from));

                    if (isOutOfDomain) {
                        console.log('Annotation is out of domain');
                        return [];
                    }

                    var secondaryScaleInfo = chart.getScaleInfo(secondary);
                    var secDomain = secondaryScaleInfo.domain();
                    var boundaries = [secDomain[0], secDomain[secDomain.length - 1]];

                    var src = {};
                    var dst = {};

                    var a = primaryScaleInfo.dim;
                    var b = secondaryScaleInfo.dim;

                    src[a] = from;
                    src[b] = boundaries[0];

                    dst[a] = from;
                    dst[b] = boundaries[1];

                    dst.text = metaInfo.text;

                    return [src, dst];
                };
            },

            addAreaNote: function (specRef, coordsUnit, noteItem) {

                var xScale = specRef.scales[coordsUnit.x];
                var yScale = specRef.scales[coordsUnit.y];

                var axes = ((noteItem.dim === xScale.dim) ?
                    ['x', 'y'] :
                    ((noteItem.dim === yScale.dim) ?
                        ['y', 'x'] :
                        (null)));

                if (axes === null) {
                    console.log('Annotation doesn\'t match any data field');
                    return;
                }

                var from = noteItem.val[0];
                var to = noteItem.val[1];

                var annotatedArea = {
                    type: 'ELEMENT.PATH',
                    namespace: 'annotations',
                    x: coordsUnit.x,
                    y: coordsUnit.y,
                    color: 'color:default',
                    text: textScaleName,
                    expression: {
                        inherit: false,
                        operator: 'none',
                        params: [],
                        source: '/'
                    },
                    transformation: [
                        {
                            type: 'dataRange',
                            args: {
                                axis: axes[0],
                                text: noteItem.text,
                                from: from,
                                to: to,
                                primaryScale: coordsUnit[axes[0]],
                                secondaryScale: coordsUnit[axes[1]]
                            }
                        }
                    ],
                    guide: {
                        showAnchors: false,
                        cssClass: 'graphical-report__annotation-area',
                        color: {
                            fill: noteItem.color
                        },
                        text: {
                            fontColor: noteItem.color,
                            paddingX: ((axes[0] === 'x') ? 5 : -5),
                            paddingY: ((axes[0] === 'x') ? 5 : 15)
                        }
                    }
                };

                addToUnits(coordsUnit.units, annotatedArea, noteItem.position);
            },

            addLineNote: function (specRef, coordsUnit, noteItem) {

                var xScale = specRef.scales[coordsUnit.x];
                var yScale = specRef.scales[coordsUnit.y];

                var axes = ((noteItem.dim === xScale.dim) ?
                    ['x', 'y'] :
                    ((noteItem.dim === yScale.dim) ?
                        ['y', 'x'] :
                        (null)));

                if (axes === null) {
                    console.log('Annotation doesn\'t match any field');
                    return;
                }

                var text = noteItem.text;
                var from = noteItem.val;

                var annotatedLine = {
                    type: 'ELEMENT.LINE',
                    namespace: 'annotations',
                    x: coordsUnit.x,
                    y: coordsUnit.y,
                    text: textScaleName,
                    color: 'color:default',
                    expression: {
                        inherit: false,
                        operator: 'none',
                        params: [],
                        source: '/'
                    },
                    transformation: [
                        {
                            type: 'dataLimit',
                            args: {
                                from: from,
                                text: text,
                                primaryScale: coordsUnit[axes[0]],
                                secondaryScale: coordsUnit[axes[1]]
                            }
                        }
                    ],
                    guide: {
                        showAnchors: false,
                        widthCssClass: 'graphical-report__line-width-2',
                        cssClass: 'graphical-report__annotation-line',
                        color: {
                            fill: noteItem.color
                        },
                        text: {
                            fontColor: noteItem.color,
                            paddingX: ((axes[0] === 'x') ? 5 : -5),
                            paddingY: ((axes[0] === 'x') ? 5 : -5)
                        }
                    }
                };

                addToUnits(coordsUnit.units, annotatedLine, noteItem.position);
            },

            onSpecReady: function (chart, specRef) {

                var self = this;
                var units = [];
                chart.traverseSpec(specRef, function (unit) {
                    if (unit && (unit.type === 'COORDS.RECT') && (unit.units)) {
                        units.push(unit);
                    }
                });

                units.forEach(function (coordsUnit) {

                    settings.items.forEach(function (item) {

                        item.color = item.color || '#BD10E0'; // #4300FF / #FFAB00

                        if (_.isArray(item.val)) {
                            self.addAreaNote(specRef, coordsUnit, item);
                        } else {
                            self.addLineNote(specRef, coordsUnit, item);
                        }
                    });
                });
            }
        };
    }

    tauCharts.api.plugins.add('annotations', annotations);

    return annotations;
});