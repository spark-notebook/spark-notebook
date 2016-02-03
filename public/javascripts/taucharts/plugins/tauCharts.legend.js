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

    function ChartLegend(xSettings) {

        var settings = _.defaults(
            xSettings || {},
            {
                // add default settings here
            });

        var log10 = function (x) {
            return Math.log(x) / Math.LN10;
        };

        var doEven = function (n) {
            n = Math.round(n);
            return n % 2 ? n + 1 : n;
        };

        var isEmpty = function (x) {
            return (x === null) || (x === '') || (typeof x === 'undefined');
        };

        var createIsRowMatchInterceptor = function (dim, val) {
            return function (row) {
                var d = row[dim];
                var r = JSON.stringify(isEmpty(d) ? null : d);
                return (val === r);
            };
        };

        var _delegateEvent = function (element, eventName, selector, callback) {
            element.addEventListener(eventName, function (e) {
                var target = e.target;
                while (target !== e.currentTarget && target !== null) {
                    if (target.classList.contains(selector)) {
                        callback(e, target);
                    }
                    target = target.parentNode;
                }
            });
        };

        return {

            init: function (chart) {

                this._chart = chart;
                this._currentFilters = {};
                this._legendColorByScaleId = {};
                this._legendOrderState = {};

                var spec = this._chart.getSpec();

                var reducer = function (scaleType) {
                    return function (memo, k) {
                        var s = spec.scales[k];
                        if (s.type === scaleType && s.dim) {
                            memo.push(k);
                        }
                        return memo;
                    };
                };

                this._color = Object.keys(spec.scales).reduce(reducer('color'), []);
                this._fill = Object.keys(spec.scales).reduce(reducer('fill'), []);
                this._size = Object.keys(spec.scales).reduce(reducer('size'), []);

                var hasColorScales = (this._color.length > 0);
                var hasFillScales = (this._fill.length > 0);
                var hasSizeScales = (this._size.length > 0);

                this._assignStaticBrewersOrEx();

                if (hasColorScales || hasFillScales || hasSizeScales) {

                    switch (settings.position) {
                        case 'left':
                            this._container = this._chart.insertToLeftSidebar(this._containerTemplate);
                            break;
                        case 'right':
                            this._container = this._chart.insertToRightSidebar(this._containerTemplate);
                            break;
                        case 'top':
                            this._container = this._chart.insertToHeader(this._containerTemplate);
                            break;
                        case 'bottom':
                            this._container = this._chart.insertToFooter(this._containerTemplate);
                            break;
                        default:
                            this._container = this._chart.insertToRightSidebar(this._containerTemplate);
                            break;
                    }

                    if (hasColorScales) {
                        _delegateEvent(
                            this._container,
                            'click',
                            'graphical-report__legend__item-color',
                            function (e, currentTarget) {
                                this._toggleLegendItem(currentTarget);
                            }.bind(this));

                        _delegateEvent(
                            this._container,
                            'mouseover',
                            'graphical-report__legend__item-color',
                            function (e, currentTarget) {
                                this._highlightToggle(currentTarget, true);
                            }.bind(this)
                        );

                        _delegateEvent(
                            this._container,
                            'mouseout',
                            'graphical-report__legend__item-color',
                            function (e, currentTarget) {
                                this._highlightToggle(currentTarget, false);
                            }.bind(this)
                        );
                    }
                }
            },

            onRender: function () {
                this._clearPanel();
                this._drawColorLegend();
                this._drawSizeLegend();
            },

            // jscs:disable maximumLineLength
            _containerTemplate: '<div class="graphical-report__legend"></div>',
            _template: _.template('<div class="graphical-report__legend__wrap"><div class="graphical-report__legend__title"><%=name%></div><%=items%></div>'),
            _itemTemplate: _.template([
                '<div data-scale-id=\'<%= scaleId %>\' data-dim=\'<%= dim %>\' data-value=\'<%= value %>\' class="graphical-report__legend__item graphical-report__legend__item-color <%=classDisabled%>">',
                '<div class="graphical-report__legend__guide__wrap">',
                '<div class="graphical-report__legend__guide <%=color%>"></div>',
                '</div>',
                '<%=label%>',
                '</div>'
            ].join('')),
            _itemFillTemplate: _.template([
                '<div data-value=\'<%=value%>\' class="graphical-report__legend__item graphical-report__legend__item-color" style="padding: 6px 0px 10px 40px;margin-left:10px;">',
                '<div class="graphical-report__legend__guide__wrap" style="top:0;left:0;">',
                '   <span class="graphical-report__legend__guide" style="background-color:<%=color%>;border-radius:0"></span>',
                '   <span style="padding-left: 20px"><%=label%></span>',
                '</div>',
                '</div>'
            ].join('')),
            _itemSizeTemplate: _.template([
                '<div class="graphical-report__legend__item graphical-report__legend__item--size">',
                '<div class="graphical-report__legend__guide__wrap">',
                '<svg class="graphical-report__legend__guide graphical-report__legend__guide--size  <%=className%>" style="width: <%=diameter%>px;height: <%=diameter%>px;"><circle cx="<%=radius%>" cy="<%=radius%>" class="graphical-report__dot" r="<%=radius%>"></circle></svg>',
                '</div><%=value%>',
                '</div>'
            ].join('')),
            // jscs:enable maximumLineLength

            _clearPanel: function () {
                if (this._container) {
                    this._container.innerHTML = '';
                }
            },

            _drawSizeLegend: function () {
                var self = this;

                self._size.forEach(function (c) {
                    var firstNode = self
                        ._chart
                        .select(function (unit) {
                            return (unit.config.size === c);
                        })
                        [0];

                    if (firstNode) {

                        var guide = firstNode.config.guide || {};

                        var sizeScale = firstNode.getScale('size');

                        var domain = _(sizeScale.domain()).sortBy();

                        var title = ((guide.size || {}).label || {}).text || sizeScale.dim;

                        var first = domain[0];
                        var last = domain[domain.length - 1];

                        var values = [first];
                        if ((last - first)) {
                            var count = log10(last - first);
                            var xF = Math.round((4 - count));
                            var base = Math.pow(10, xF);
                            var step = (last - first) / 5;
                            var steps = [first, first + step, first + step * 2, first + step * 3, last];
                            values = _(steps)
                                .chain()
                                .map(function (x) {
                                    return (x === last || x === first) ? x : Math.round(x * base) / base;
                                })
                                .unique()
                                .value();
                        }

                        self._container
                            .insertAdjacentHTML('beforeend', self._template({
                                name: title,
                                items: values
                                    .map(function (value) {
                                        var radius = sizeScale(value);
                                        return self._itemSizeTemplate({
                                            diameter: doEven(radius * 2 + 2),
                                            radius: radius,
                                            value: value,
                                            className: firstNode.config.color ? 'color-definite' : 'color-default-size'
                                        });
                                    })
                                    .reverse()
                                    .join('')
                            }));
                    }
                });
            },

            _drawColorLegend: function () {
                var self = this;

                self._color.forEach(function (c) {
                    var firstNode = self
                        ._chart
                        .select(function (unit) {
                            return (unit.config.color === c);
                        })
                        [0];

                    if (firstNode) {

                        var guide = firstNode.config.guide || {};

                        var colorScale = firstNode.getScale('color');
                        var dataSource = self
                            ._chart
                            .getDataSources({excludeFilter: ['legend']});

                        var domain = _(dataSource[colorScale.source].data)
                            .chain()
                            .pluck(colorScale.dim)
                            .uniq()
                            .value();

                        var colorScaleConfig = self._chart.getSpec().scales[c];
                        if (colorScaleConfig.order) {
                            domain = _.union(_.intersection(colorScaleConfig.order, domain), domain);
                        } else {
                            var orderState = self._legendOrderState[c];
                            domain = domain.sort(function (a, b) {
                                var diff = orderState[a] - orderState[b];
                                return (diff && (diff / Math.abs(diff)));
                            });
                        }

                        var title = ((guide.color || {}).label || {}).text || colorScale.dim;
                        var noVal = ((guide.color || {}).tickFormatNullAlias || ('No ' + title));

                        var legendColorItems = domain.map(function (d) {
                            var val = JSON.stringify(isEmpty(d) ? null : d);
                            var key = colorScale.dim + val;

                            return {
                                scaleId: c,
                                dim: colorScale.dim,
                                color: colorScale(d),
                                disabled: self._currentFilters.hasOwnProperty(key),
                                label: d,
                                value: val
                            };
                        });

                        self._legendColorByScaleId[c] = legendColorItems;
                        self._container
                            .insertAdjacentHTML('beforeend', self._template({
                                name: title,
                                items: legendColorItems
                                    .map(function (d) {
                                        return self._itemTemplate({
                                            scaleId: d.scaleId,
                                            dim: _.escape(d.dim),
                                            color: d.color,
                                            classDisabled: d.disabled ? 'disabled' : '',
                                            label: _.escape(isEmpty(d.label) ? noVal : d.label),
                                            value: _.escape(d.value)
                                        });
                                    })
                                    .join('')
                            }));
                    }
                });
            },

            _toggleLegendItem: function (target) {

                var sid = target.getAttribute('data-scale-id');
                var dim = target.getAttribute('data-dim');
                var val = target.getAttribute('data-value');
                var key = dim + val;

                var items = this._legendColorByScaleId[sid];
                var activeItems = items.filter(function (x) {
                    return !x.disabled;
                });

                if ((activeItems.length === 1) &&
                    (sid === activeItems[0].scaleId) &&
                    (val === activeItems[0].value)) {
                    return;
                }

                var state = this._currentFilters;
                if (state.hasOwnProperty(key)) {
                    var filterId = state[key];
                    delete state[key];
                    target.classList.remove('disabled');
                    this._chart.removeFilter(filterId);
                } else {
                    target.classList.add('disabled');
                    var isRowMatch = createIsRowMatchInterceptor(dim, val);
                    state[key] = this._chart.addFilter({
                        tag: 'legend',
                        predicate: function (row) {
                            return !isRowMatch(row);
                        }
                    });
                }
                this._chart.refresh();
            },

            _highlightToggle: function (target, doHighlight) {

                if (target.classList.contains('disabled')) {
                    return;
                }

                // var scaleId = target.getAttribute('data-scale-id');
                var dim = target.getAttribute('data-dim');
                var val = target.getAttribute('data-value');

                var isRowMatch = doHighlight ?
                    (createIsRowMatchInterceptor(dim, val)) :
                    (function (row) { return null; });

                this._chart
                    .select(function (unit) {
                        // return unit.config.color === scaleId;
                        // use all found elements
                        return true;
                    })
                    .forEach(function (unit) {
                        unit.fire('highlight', isRowMatch);
                    });
            },

            _generateColorMap: function (domain) {

                var limit = 20;

                var defBrewer = _.times(limit, function (i) {
                    return 'color20-' + (1 + i);
                });

                return domain.reduce(function (memo, val, i) {
                        memo[val] = defBrewer[i % limit];
                        return memo;
                    },
                    {});
            },

            _assignStaticBrewersOrEx: function () {
                var self = this;
                self._color.forEach(function (c) {
                    var scaleConfig = self
                        ._chart
                        .getSpec()
                        .scales[c];

                    var fullLegendDataSource = self
                        ._chart
                        .getDataSources({excludeFilter: ['legend']});

                    var fullLegendDomain = self
                        ._chart
                        .getScaleFactory(fullLegendDataSource)
                        .createScaleInfoByName(c)
                        .domain();

                    if (!scaleConfig.brewer) {
                        scaleConfig.brewer = self._generateColorMap(fullLegendDomain);
                    }

                    self._legendOrderState[c] = fullLegendDomain.reduce(function (memo, x, i) {
                        memo[x] = i;
                        return memo;
                    }, {});
                });
            }
        };
    }

    tauCharts.api.plugins.add('legend', ChartLegend);

    return ChartLegend;
});
