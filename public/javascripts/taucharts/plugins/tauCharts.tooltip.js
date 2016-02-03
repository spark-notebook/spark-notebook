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

    function Tooltip(xSettings) {

        var settings = _.defaults(
            xSettings || {},
            {
                // add default settings here
                fields: null,
                formatters: {},
                dockToData: false,
                aggregationGroupFields: [],
                onRevealAggregation: function (filters, row) {
                    console.log(
                        'Setup [onRevealAggregation] callback and filter original data by the following criteria: ',
                        JSON.stringify(filters, null, 2));
                }
            });

        function getOffsetRect(elem) {

            var box = elem.getBoundingClientRect();

            var body = document.body;
            var docElem = document.documentElement;

            var scrollTop = window.pageYOffset || docElem.scrollTop || body.scrollTop;
            var scrollLeft = window.pageXOffset || docElem.scrollLeft || body.scrollLeft;

            var clientTop = docElem.clientTop || body.clientTop || 0;
            var clientLeft = docElem.clientLeft || body.clientLeft || 0;

            var top  = box.top +  scrollTop - clientTop;
            var left = box.left + scrollLeft - clientLeft;

            return {
                top: Math.round(top),
                left: Math.round(left)
            };
        }

        var plugin = {

            init: function (chart) {

                this._currentData = null;
                this._currentUnit = null;
                this._chart = chart;

                this._metaInfo = {};
                this._skipInfo = {};

                // TODO: for compatibility with old TargetProcess implementation
                _.extend(this, _.omit(settings, 'fields', 'getFields'));

                this._tooltip = this._chart.addBalloon(
                    {
                        spacing: 3,
                        auto: true,
                        effectClass: 'fade'
                    });

                var revealAggregationBtn = ((settings.aggregationGroupFields.length > 0) ?
                        (this.templateRevealAggregation) :
                        ('')
                );

                var template = _.template(this.template);

                this._tooltip
                    .content(template({
                        revealTemplate: revealAggregationBtn,
                        excludeTemplate: this.templateExclude
                    }));

                this._tooltip
                    .getElement()
                    .addEventListener('click', function (e) {

                        var target = e.target;

                        while (target !== e.currentTarget && target !== null) {
                            if (target.classList.contains('i-role-exclude')) {
                                self._exclude();
                            }

                            if (target.classList.contains('i-role-reveal')) {
                                self._reveal();
                            }

                            target = target.parentNode;
                        }

                        self._tooltip.hide();

                    }, false);

                var self = this;
                var timeoutHide;
                this.showTooltip = function (data, pos) {

                    clearTimeout(timeoutHide);

                    self._currentData = data;

                    var content = self._tooltip.getElement().querySelectorAll('.i-role-content');
                    if (content[0]) {

                        var fields = (
                            settings.fields
                            ||
                            (_.isFunction(settings.getFields) && settings.getFields(self._chart))
                            ||
                            Object.keys(data)
                        );

                        content[0].innerHTML = this.render(data, fields);
                    }

                    self._tooltip
                        .show(pos.x, pos.y)
                        .updateSize();
                };

                this.hideTooltip = function (e) {
                    timeoutHide = setTimeout(
                        function () {
                            self._tooltip.hide();
                            self._removeFocus();
                        },
                        300);
                };

                this._tooltip
                    .getElement()
                    .addEventListener('mouseover', function (e) {
                        clearTimeout(timeoutHide);
                        self._accentFocus();
                    }, false);

                this._tooltip
                    .getElement()
                    .addEventListener('mouseleave', function (e) {
                        self._tooltip.hide();
                        self._removeFocus();
                    }, false);

                this.afterInit(this._tooltip.getElement());
            },

            destroy: function () {
                this._removeFocus();
                this._tooltip.destroy();
            },

            afterInit: function (tooltipNode) {
                // for override
            },

            render: function (data, fields) {
                var self = this;
                return fields
                    .filter(function (k) {
                        var tokens = k.split('.');
                        var matchX = ((tokens.length === 2) && self._skipInfo[tokens[0]]);
                        return !matchX;
                    })
                    .map(function (k) {
                        var key = k;
                        var val = data[k];
                        return self.renderItem(self._getLabel(key), self._getFormat(key)(val), key, val);
                    })
                    .join('');
            },

            renderItem: function (label, formattedValue, fieldKey, fieldVal) {
                return this.itemTemplate({
                    label: label,
                    value: formattedValue
                });
            },

            _getFormat: function (k) {
                var meta = this._metaInfo[k] || {format: _.identity};
                return meta.format;
            },

            _getLabel: function (k) {
                var meta = this._metaInfo[k] || {label: k};
                return meta.label;
            },

            _appendFocus: function (g, x, y) {

                if (this.circle) {
                    this.circle.remove();
                }

                this.circle = d3
                    .select(g)
                    .append('circle')
                    .attr({
                        r: 0,
                        cx: x,
                        cy: y
                    });

                return this.circle;
            },

            _removeFocus: function () {
                if (this.circle) {
                    this.circle.remove();
                }

                if (this._currentUnit) {
                    this._currentUnit.fire('highlight-data-points', function (row) {
                        return false;
                    });
                }

                return this;
            },

            _accentFocus: function () {
                var self = this;
                if (self._currentUnit && self._currentData) {
                    self._currentUnit.fire('highlight-data-points', function (row) {
                        return row === self._currentData;
                    });
                }

                return this;
            },

            _reveal: function () {
                var aggregatedRow = this._currentData;
                var groupFields = (settings.aggregationGroupFields || []);
                var descFilters = groupFields.reduce(function (memo, k) {
                    if (aggregatedRow.hasOwnProperty(k)) {
                        memo[k] = aggregatedRow[k];
                    }
                    return memo;
                }, {});

                settings.onRevealAggregation(descFilters, aggregatedRow);
            },

            _exclude: function () {
                this._chart
                    .addFilter({
                        tag: 'exclude',
                        predicate: (function (element) {
                            return function (row) {
                                return JSON.stringify(row) !== JSON.stringify(element);
                            };
                        }(this._currentData))
                    });
                this._chart.refresh();
            },

            onRender: function () {

                var info = this._getFormatters();
                this._metaInfo = info.meta;
                this._skipInfo = info.skip;

                this._subscribeToHover();
            },

            templateRevealAggregation: [
                '<div class="i-role-reveal graphical-report__tooltip__vertical">',
                '   <div class="graphical-report__tooltip__vertical__wrap">',
                '       Reveal',
                '   </div>',
                '</div>'
            ].join(''),

            templateExclude: [
                '<div class="i-role-exclude graphical-report__tooltip__exclude">',
                '   <div class="graphical-report__tooltip__exclude__wrap">',
                '       <span class="tau-icon-close-gray"></span>',
                '       Exclude',
                '   </div>',
                '</div>'
            ].join(''),

            template: [
                '<div class="i-role-content graphical-report__tooltip__content"></div>',
                '<%= revealTemplate %>',
                '<%= excludeTemplate %>'
            ].join(''),

            itemTemplate: _.template([
                '<div class="graphical-report__tooltip__list__item">',
                '<div class="graphical-report__tooltip__list__elem"><%=label%></div>',
                '<div class="graphical-report__tooltip__list__elem"><%=value%></div>',
                '</div>'
            ].join('')),

            _subscribeToHover: function () {
                var self = this;

                var elementsToMatch = [
                    'ELEMENT.LINE',
                    'ELEMENT.AREA',
                    'ELEMENT.PATH',
                    'ELEMENT.INTERVAL',
                    'ELEMENT.INTERVAL.STACKED'
                ];

                var mouseOverHandler = function (sender, e) {
                    var data = e.data;
                    var coords = (settings.dockToData ?
                        self._getNearestDataCoordinates(sender, e) :
                        self._getMouseCoordinates(sender, e));

                    self._currentUnit = sender;
                    self.showTooltip(data, {x: coords.left, y: coords.top});
                };

                this._chart
                    .select(function (node) {
                        return true;
                    })
                    .forEach(function (node) {

                        node.on('mouseout.chart', function (sender, e) {
                            self.hideTooltip(e);
                        });

                        node.on('mouseover.chart', mouseOverHandler);

                        if (elementsToMatch.indexOf(node.config.type) > -1) {
                            node.on('mousemove.chart', mouseOverHandler);
                        }
                    });
            },

            _getNearestDataCoordinates: function (sender, e) {

                var data = e.data;
                var xLocal;
                var yLocal;

                var xScale = sender.getScale('x');
                var yScale = sender.getScale('y');

                if (sender.config.type === 'ELEMENT.INTERVAL.STACKED') {
                    var view = e.event.chartElementViewModel;
                    xLocal = xScale(view.x);
                    yLocal = yScale(view.y);
                } else {
                    xLocal = xScale(data[xScale.dim]);
                    yLocal = yScale(data[yScale.dim]);
                }

                var g = e.event.target.parentNode;
                var c = this._appendFocus(g, xLocal, yLocal);

                return getOffsetRect(c.node());
            },

            _getMouseCoordinates: function (sender, e) {

                var xLocal = e.event.pageX;
                var yLocal = e.event.pageY;

                return {left: xLocal, top: yLocal};
            },

            _getFormatters: function () {

                var info = pluginsSDK.extractFieldsFormatInfo(this._chart.getSpec());
                var skip = {};
                Object.keys(info).forEach(function (k) {

                    if (info[k].isComplexField) {
                        skip[k] = true;
                    }

                    if (info[k].parentField) {
                        delete info[k];
                    }
                });

                Object.keys(settings.formatters).forEach(function (k) {
                    var fmt = settings.formatters[k];
                    info[k] = info[k] || {label: k, nullAlias: ('No ' + k)};
                    info[k].format = (_.isFunction(fmt) ?
                            (fmt) :
                            (tauCharts.api.tickFormat.get(fmt, info[k].nullAlias))
                    );
                });

                return {
                    meta: info,
                    skip: skip
                };
            }
        };

        return plugin;
    }

    tauCharts.api.plugins.add('tooltip', Tooltip);

    return Tooltip;
});