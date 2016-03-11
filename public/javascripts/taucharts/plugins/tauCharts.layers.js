// jscs:disable *
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
    var pluginsSDK = tauCharts.api.pluginsSDK;
    var tokens = pluginsSDK.tokens();

    function layers(xSettings) {

        var settings = _.defaults(
            xSettings || {},
            {
                title: 'Layers',
                label: 'Layer Type',
                showPanel: true,
                showLayers: true,
                mode: 'merge',
                axisWidth: 45
            });

        settings.layers.forEach(function (layer) {
            layer.guide = _.defaults(
                (layer.guide || {}),
                {
                    scaleOrient: 'left',
                    textAnchor: 'end',
                    hide: false
                });
        });

        var ELEMENT_TYPE = {
            line: 'ELEMENT.LINE',
            area: 'ELEMENT.AREA',
            dots: 'ELEMENT.POINT',
            scatterplot: 'ELEMENT.POINT',
            bar: 'ELEMENT.INTERVAL',
            'stacked-bar': 'ELEMENT.INTERVAL.STACKED'
        };

        return {

            init: function (chart) {

                this._chart = chart;

                var spec = pluginsSDK.spec(this._chart.getSpec());
                var errors = this.checkIfApplicable(spec);
                this._isApplicable = (errors.length === 0);

                spec.addTransformation('defined-only', function (data, props) {
                    var k = props.key;
                    return _(data)
                        .chain()
                        .filter(function (row) {
                            return ((row[k] !== null) && (typeof (row[k]) !== 'undefined'));
                        })
                        .value();
                });

                if (!this._isApplicable) {
                    var log = spec.getSettings('log');
                    log('[layers plugin]: is not applicable. ' + errors.join(' / '));
                    return;
                }

                this.isFacet = this.checkIsFacet(spec);
                this.primaryY = this.findPrimaryLayer(spec);
                var layersText = this.getLayersText();
                var layersGroup = this.getLayersGroup();

                var metaField = settings.label;
                this.fieldColorScale = metaField;

                spec.setSettings('excludeNull', false)
                    .setSettings('fitModel', null)
                    .addScale(metaField, {type: 'color', source: '/', dim: metaField, brewer: settings.brewer})
                    .addTransformation('slice-layer', function (data, props) {
                        var k = props.key;
                        var g = props.group;
                        if (g) {
                            return _(data).filter(function (row) {
                                var groupKey = row[g];
                                var groupVal = row[groupKey];
                                return (
                                    (groupKey)
                                    &&
                                    (groupVal !== null)
                                    &&
                                    (typeof (groupVal) !== 'undefined')
                                );
                            });
                        } else {
                            return _(data).filter(function (row) {
                                return (
                                    (row[metaField] === layersText[k])
                                    &&
                                    (row[k] !== null)
                                    &&
                                    (typeof (row[k]) !== 'undefined')
                                );
                            });
                        }
                    });

                var layersDims = [this.primaryY]
                    .concat(settings.layers)
                    .reduce(function (memo, layer) {
                        return memo.concat(layer.y);
                    }, []);

                chart.setupChartSourceModel(function (originalSources) {

                    var newDim = {};
                    newDim[metaField] = {type: 'category'};

                    var sources = {
                        '/':{
                            dims: newDim,
                            data: []
                        }
                    };

                    sources['/'].dims = _.extend(newDim, originalSources['/'].dims);
                    sources['/'].data = originalSources['/'].data.reduce(function (memo, row) {
                        return memo.concat(layersDims.map(function (layerDim) {
                            var seed = {};
                            seed[metaField] = layersText[layerDim];
                            var g = layersGroup[layerDim];
                            if (g) {
                                seed[g] = row[layerDim];
                                seed['subLayer'] = g;
                            }
                            return _.extend(seed, row);
                        }));
                    }, []);

                    return _.extend(sources, _.omit(originalSources, '/'));
                });

                if (settings.showPanel) {

                    this._container = chart.insertToRightSidebar(this.containerTemplate);
                    this._container.classList.add('applicable-true');

                    this.uiChangeEventsDispatcher = function (e) {

                        var target = e.target;
                        var selector = target.classList;

                        if (selector.contains('i-role-show-layers')) {
                            settings.showLayers = target.checked;
                        }

                        if (selector.contains('i-role-change-mode')) {
                            settings.mode = target.value;
                        }

                        this._chart.refresh();

                    }.bind(this);

                    this._container
                        .addEventListener('change', this.uiChangeEventsDispatcher, false);
                }
            },

            getLayersText: function () {
                return ([this.primaryY]
                    .concat(settings.layers)
                    .reduce(function (memo, layer) {
                        var ys = (_.isArray(layer.y) ? layer.y : [layer.y]);
                        return ys.reduce(function (state, y) {
                            state[y] = this.extractLabelForKey(layer, y);
                            return state;
                        }.bind(this), memo);

                    }.bind(this), {}));
            },

            getLayersGroup: function () {
                return ([this.primaryY]
                    .concat(settings.layers)
                    .reduce(function (memo, layer) {
                        var g = null;
                        if (_.isArray(layer.y)) {
                            g = layer.y.join(', ');
                        }

                        return _.flatten([layer.y]).reduce(function (memo, y) {
                            memo[y] = g;
                            return memo;
                        }, memo);
                    }.bind(this), {}));
            },

            checkIsFacet: function (spec) {

                return spec.unit().reduce(function (state, unit, parent) {

                    if (state) {
                        return state;
                    }

                    if (parent && (parent.type === 'COORDS.RECT') && (unit.type === 'COORDS.RECT')) {
                        state = true;
                        return state;
                    }

                    return state;

                }, false);
            },

            checkIfApplicable: function (spec) {

                return spec.unit().reduce(function (errors, unit, parent) {

                    if (parent && (parent.type !== 'COORDS.RECT')) {
                        return errors.concat('Chart specification contains non-rectangular coordinates');
                    }

                    if (parent && (parent.type === 'COORDS.RECT') && (unit.type !== 'COORDS.RECT')) {
                        // is Y axis a measure?
                        var yScale = spec.getScale(unit.y);
                        if (spec.getSourceDim(yScale.source, yScale.dim).type !== 'measure') {
                            return errors.concat('Y scale is not a measure');
                        }
                    }

                    return errors;

                }, []);
            },

            isLeafElement: function (unit, parent) {
                return ((parent) && (parent.type === 'COORDS.RECT') && (unit.type !== 'COORDS.RECT'));
            },

            isFirstCoordNode: function (unit, parent) {
                return (!parent && unit && (unit.type === 'COORDS.RECT'));
            },

            isFinalCoordNode: function (unit, parent) {
                return ((unit) && (unit.type === 'COORDS.RECT')
                    &&
                    (_.every(unit.units, function (subUnit) {
                        return subUnit.type !== 'COORDS.RECT';
                    }))
                );
            },

            buildLayersLayout: function (fullSpec) {

                return (fullSpec.regSource('$',
                    {
                        dims: {
                            x: {type: 'category'},
                            y: {type: 'category'}
                        },
                        data: [{x: 1, y: 1}]
                    })
                    .addScale('xLayoutScale', {type: 'ordinal', source: '$', dim: 'x'})
                    .addScale('yLayoutScale', {type: 'ordinal', source: '$', dim: 'y'})
                    .unit({
                        type: 'COORDS.RECT',
                        x: 'xLayoutScale',
                        y: 'yLayoutScale',
                        expression: {
                            source: '$',
                            inherit: false,
                            operator: false
                        },
                        guide: {
                            showGridLines: '',
                            x: {cssClass: 'facet-axis'},
                            y: {cssClass: 'facet-axis'}
                        }
                    }));
            },

            findPrimaryLayer: function (spec) {
                var self = this;
                var resY = spec.unit().reduce(function (memo, unit) {
                    return memo.concat(self.isFinalCoordNode(unit) ?
                        ({
                            y: spec.getScale(unit.y).dim,
                            isPrimary: true,
                            guide: unit.guide.y,
                            scaleName: unit.y
                        }) :
                        ([]));
                }, []);

                return pluginsSDK.cloneObject(resY[0]);
            },

            createPrimaryUnitReducer: function (fullSpec, currLayers, lPad, rPad) {

                var self = this;

                return function (memo, unit, parent) {

                    var isVisibleAxis = function (layer) {
                        return (layer.guide.hide !== true);
                    };

                    if (self.isFacet && self.isFirstCoordNode(unit, parent)) {
                        unit.guide.y.label = (unit.guide.y.label || {});
                        var facetLabelSeed = unit.guide.y.label._original_text || unit.guide.y.label.text;
                        unit.guide.y.label.text = [
                            facetLabelSeed
                            ,
                            _(currLayers).filter(isVisibleAxis).map(self.extractLayerLabel.bind(self)).join(', ')
                        ].join(fullSpec.getSettings('facetLabelDelimiter'));

                        if (settings.mode === 'dock') {
                            unit.guide.y.label.padding -= 15;
                            unit.guide.y.padding += 15;
                            unit.guide.y.rotate = (-90);
                            unit.guide.y.textAnchor = 'middle';
                        }
                    }

                    if (self.isLeafElement(unit, parent)) {

                        parent.units = parent.units.filter(function (pUnit) {
                            return (pUnit !== unit);
                        });
                    }

                    if (self.isFinalCoordNode(unit)) {

                        unit.guide.y.label = (unit.guide.y.label || {});

                        if (settings.mode === 'dock') {
                            unit.guide.padding.l = lPad;
                            unit.guide.padding.r = rPad;
                            unit.guide.y.hide = true;
                        }

                        if (settings.mode === 'merge') {
                            unit.guide.y.label.text = (self.isFacet ?
                                ('') :
                                _(currLayers).filter(isVisibleAxis).map(self.extractLayerLabel.bind(self)).join(', '));
                        }
                    }
                    return memo;
                };
            },

            createSecondaryUnitReducer: function (fullSpec, xLayer, lPad, rPad, totalDif, iLeft, iRight) {

                var self = this;
                var layerScaleName = self.getScaleName(xLayer.scaleName || xLayer.y);
                var layerScaleOrient = xLayer.guide.scaleOrient;
                var isGroupedY = _.isArray(xLayer.y);
                var isPrimaryLayer = (xLayer.isPrimary);

                return function (memo, unit, parent) {

                    if (self.isFacet && self.isFirstCoordNode(unit, parent)) {
                        unit.guide.y.label.text = '';
                        unit.guide.x.hide = true;
                        unit.guide.y.hide = true;
                    }

                    if (self.isLeafElement(unit, parent)) {
                        unit.type = xLayer.type ? ELEMENT_TYPE[xLayer.type] : unit.type;
                        unit.y = layerScaleName;

                        var isFullScale = (fullSpec.getScale(unit.color).dim);
                        if (isPrimaryLayer && isFullScale) {
                            // leave original color scale
                        } else {
                            unit.color = self.fieldColorScale;
                            unit.expression.operator = 'groupBy';
                            unit.expression.params = (isGroupedY) ? ['subLayer'] : [self.fieldColorScale];
                        }

                        // slice frame data
                        var params = (isGroupedY) ? {group: 'subLayer'} : {key: xLayer.y};
                        pluginsSDK
                            .unit(unit)
                            .addTransformation('slice-layer', params);
                    }

                    var isFinalCoord = self.isFinalCoordNode(unit);
                    if (isFinalCoord) {
                        unit.y = layerScaleName;
                        unit.guide.y = _.extend(unit.guide.y, (xLayer.guide || {}));
                        unit.guide.y.label = (unit.guide.y.label || {});
                        unit.guide.y.label.text = self.extractLayerLabel(xLayer);
                        unit.guide.x.hide = true;

                        if (settings.mode === 'dock') {
                            unit.guide.showGridLines = '';
                            unit.guide.padding.l = lPad;
                            unit.guide.padding.r = rPad;
                            unit.guide.y.label.textAnchor = 'end';
                            unit.guide.y.label.dock = 'right';
                            unit.guide.y.label.padding = ((layerScaleOrient === 'right') ? 1 : (-10));
                            unit.guide.y.label.cssClass = 'label inline';
                            var iKoeff = ((layerScaleOrient === 'right') ? iRight : iLeft);
                            unit.guide.y.padding += (totalDif * iKoeff);
                        }

                        if (settings.mode === 'merge') {
                            unit.guide.showGridLines = '';
                            unit.guide.y.hide = true;
                        }
                    }

                    return memo;
                };
            },

            getScaleName: function (layerY) {
                return (_.isArray(layerY)) ? layerY.join(', ') : layerY;
            },

            extractLabelForKey: function (layer, yKey) {
                var g = layer.guide || {};
                g.label = (_.isString(g.label) ? {text: g.label} : g.label);
                var l = (g.label || {});
                var keys = l.byKeys || {};

                if (_.isArray(layer.y)) {
                    return keys[yKey] || yKey;
                }

                return ((l.text) || (l._original_text) || layer.y);
            },

            extractLayerLabel: function (layer) {
                var self = this;
                var ys = (_.isArray(layer.y) ? layer.y : [layer.y]);
                return ys
                    .map(function (yKey) {
                        return self.extractLabelForKey(layer, yKey);
                    })
                    .join(', ');
            },

            onSpecReady: function (chart, specRef) {

                var self = this;

                var fullSpec = pluginsSDK.spec(specRef);

                if (!settings.showLayers || !self._isApplicable) {
                    fullSpec.unit().traverse(function (unit, parentUnit) {
                        if (self.isLeafElement(unit, parentUnit)) {
                            pluginsSDK
                                .unit(unit)
                                .addTransformation('defined-only', {key: fullSpec.getScale(unit.y).dim});
                        }
                    });
                    return;
                }

                fullSpec = settings
                    .layers
                    .reduce(function (memo, layer) {
                        var scaleName = self.getScaleName(layer.y);
                        return memo.addScale(
                            scaleName,
                            _.extend(
                                {type: 'linear', source: '/', dim: scaleName, autoScale: true},
                                (_.pick(layer.guide || {}, 'min', 'max', 'autoScale'))));
                    }, fullSpec);

                var currLayers = [this.primaryY].concat(settings.layers).sort(function (a, b) {
                    var zIndexA = a.guide.zIndex || 0;
                    var zIndexB = b.guide.zIndex || 0;
                    return (zIndexA - zIndexB);
                });

                var prevUnit = fullSpec.unit();
                var cursor;
                var gap = settings.axisWidth;

                var checkOrient = function (expectedOrient) {
                    return function (layer) {
                        var layerOrient = layer.guide.scaleOrient || 'left';
                        return ((layer.guide.hide !== true) && (layerOrient === expectedOrient));
                    };
                };

                var lCheck = checkOrient('left');
                var rCheck = checkOrient('right');

                var lPad = (currLayers.filter(lCheck).length * gap);
                var rPad = (currLayers.filter(rCheck).length * gap);

                var currUnit = self
                    .buildLayersLayout(fullSpec)
                    .addFrame({
                        key: {x: 1, y: 1},
                        units: [(cursor = (pluginsSDK
                            .unit(prevUnit.clone())))
                            .reduce(self.createPrimaryUnitReducer(fullSpec, currLayers, lPad, rPad), cursor)
                            .value()
                        ]
                    });

                var il = -1;
                var ir = -1;

                currLayers.reduce(function (specUnitObject, layer) {

                    il = (lCheck(layer) ? (il + 1) : il);
                    ir = (rCheck(layer) ? (ir + 1) : ir);

                    return specUnitObject.addFrame({
                        key: {x: 1, y: 1},
                        units: [(cursor = (pluginsSDK
                            .unit(prevUnit.clone())))
                            .reduce(self.createSecondaryUnitReducer(fullSpec, layer, lPad, rPad, gap, il, ir), cursor)
                            .value()
                        ]
                    });
                }, currUnit);
            },

            onUnitsStructureExpanded: function () {

                var self = this;

                if (self._isApplicable && (settings.mode === 'merge')) {

                    var fullSpec = pluginsSDK.spec(self._chart.getSpec());
                    var primaryY = self.primaryY.scaleName;
                    var scaleNames = _(settings.layers)
                        .map(function (layer) {
                            return self.getScaleName(layer.y);
                        })
                        .filter(function (name) {
                            return fullSpec.getScale(name);
                        })
                        .concat(primaryY);

                    var hashBounds = scaleNames.reduce(function (memo, yi) {
                            var info = self._chart.getScaleInfo(yi);
                            memo[yi] = info.domain().filter(function (n) {
                                return !isNaN(n) && _.isNumber(n);
                            });
                            return memo;
                        },
                        {});

                    var minMax = d3.extent(_(hashBounds).chain().values().flatten().value());
                    scaleNames.forEach(function (y) {
                        var yScale = fullSpec.getScale(y);
                        yScale.min = minMax[0];
                        yScale.max = minMax[1];
                        yScale.autoScale = false;
                    });
                }
            },

            // jscs:disable maximumLineLength
            containerTemplate: '<div class="graphical-report__trendlinepanel"></div>',
            template: _.template([
                '<label class="graphical-report__trendlinepanel__title graphical-report__checkbox">',
                '   <input type="checkbox"',
                '          class="graphical-report__checkbox__input i-role-show-layers"',
                '          <%= (showLayers ? "checked" : "") %>',
                '   />',
                '   <span class="graphical-report__checkbox__icon"></span>',
                '   <span class="graphical-report__checkbox__text"><%= title %></span>',
                '</label>',

                '<div>',
                '<select class="i-role-change-mode graphical-report__select graphical-report__trendlinepanel__control">',
                '   <option <%= ((mode === "dock")  ? "selected" : "") %> value="dock">' + tokens.get('Dock') + '</option>',
                '   <option <%= ((mode === "merge") ? "selected" : "") %> value="merge">' + tokens.get('Merge') + '</option>',
                '</select>',
                '</div>'
            ].join('')),
            // jscs:enable maximumLineLength

            onRender: function () {

                if (this._isApplicable && settings.showPanel) {
                    this._container.innerHTML = this.template({
                        title: settings.title,
                        mode: settings.mode,
                        showLayers: settings.showLayers
                    });
                }
            }
        };
    }

    tauCharts.api.plugins.add('layers', layers);

    return layers;
});
// jscs:enable *