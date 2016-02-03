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
    // jscs:disable
    var regressionsHub = (function () {

        'use strict';

        var gaussianElimination = function (a, o) {
            var i = 0, j = 0, k = 0, maxrow = 0, tmp = 0, n = a.length - 1, x = new Array(o);
            for (i = 0; i < n; i++) {
                maxrow = i;
                for (j = i + 1; j < n; j++) {
                    if (Math.abs(a[i][j]) > Math.abs(a[i][maxrow]))
                        maxrow = j;
                }
                for (k = i; k < n + 1; k++) {
                    tmp = a[k][i];
                    a[k][i] = a[k][maxrow];
                    a[k][maxrow] = tmp;
                }
                for (j = i + 1; j < n; j++) {
                    for (k = n; k >= i; k--) {
                        a[k][j] -= a[k][i] * a[i][j] / a[i][i];
                    }
                }
            }
            for (j = n - 1; j >= 0; j--) {
                tmp = 0;
                for (k = j + 1; k < n; k++)
                    tmp += a[k][j] * x[k];
                x[j] = (a[n][j] - tmp) / a[j][j];
            }
            return (x);
        };

        // GREAT THANKS to Tom Alexander!
        // https://github.com/Tom-Alexander/regression-js
        var methods = {
            linear: function (data) {
                var sum = [0, 0, 0, 0, 0], n = 0, results = [];

                for (; n < data.length; n++) {
                    if (data[n][1]) {
                        sum[0] += data[n][0];
                        sum[1] += data[n][1];
                        sum[2] += data[n][0] * data[n][0];
                        sum[3] += data[n][0] * data[n][1];
                        sum[4] += data[n][1] * data[n][1];
                    }
                }

                var gradient = (n * sum[3] - sum[0] * sum[1]) / (n * sum[2] - sum[0] * sum[0]);

                gradient = isNaN(gradient) ? 0 : gradient;

                var intercept = (sum[1] / n) - (gradient * sum[0]) / n;
                //  var correlation = (n * sum[3] - sum[0] * sum[1]) / Math.sqrt((n * sum[2] - sum[0] * sum[0]) * (n * sum[4] - sum[1] * sum[1]));

                for (var i = 0, len = data.length; i < len; i++) {
                    var coordinate = [data[i][0], data[i][0] * gradient + intercept];
                    results.push(coordinate);
                }

                var string = 'y = ' + Math.round(gradient * 100) / 100 + 'x + ' + Math.round(intercept * 100) / 100;

                return {equation: [gradient, intercept], points: results, string: string};
            },

            exponential: function (data) {
                var sum = [0, 0, 0, 0, 0, 0], n = 0, results = [];

                for (len = data.length; n < len; n++) {
                    if (data[n][1]) {
                        sum[0] += data[n][0];
                        sum[1] += data[n][1];
                        sum[2] += data[n][0] * data[n][0] * data[n][1];
                        sum[3] += data[n][1] * Math.log(data[n][1]);
                        sum[4] += data[n][0] * data[n][1] * Math.log(data[n][1]);
                        sum[5] += data[n][0] * data[n][1];
                    }
                }

                var denominator = (sum[1] * sum[2] - sum[5] * sum[5]);
                var A = Math.pow(Math.E, (sum[2] * sum[3] - sum[5] * sum[4]) / denominator);
                var B = (sum[1] * sum[4] - sum[5] * sum[3]) / denominator;

                for (var i = 0, len = data.length; i < len; i++) {
                    var coordinate = [data[i][0], A * Math.pow(Math.E, B * data[i][0])];
                    results.push(coordinate);
                }

                var string = 'y = ' + Math.round(A * 100) / 100 + 'e^(' + Math.round(B * 100) / 100 + 'x)';

                return {equation: [A, B], points: results, string: string};
            },

            logarithmic: function (data) {
                var sum = [0, 0, 0, 0], n = 0, results = [];

                for (len = data.length; n < len; n++) {
                    if (data[n][1]) {
                        sum[0] += Math.log(data[n][0]);
                        sum[1] += data[n][1] * Math.log(data[n][0]);
                        sum[2] += data[n][1];
                        sum[3] += Math.pow(Math.log(data[n][0]), 2);
                    }
                }

                var B = (n * sum[1] - sum[2] * sum[0]) / (n * sum[3] - sum[0] * sum[0]);
                var A = (sum[2] - B * sum[0]) / n;

                for (var i = 0, len = data.length; i < len; i++) {
                    var coordinate = [data[i][0], A + B * Math.log(data[i][0])];
                    results.push(coordinate);
                }

                var string = 'y = ' + Math.round(A * 100) / 100 + ' + ' + Math.round(B * 100) / 100 + ' ln(x)';

                return {equation: [A, B], points: results, string: string};
            },

            power: function (data) {
                var sum = [0, 0, 0, 0], n = 0, results = [];

                for (len = data.length; n < len; n++) {
                    if (data[n][1]) {
                        sum[0] += Math.log(data[n][0]);
                        sum[1] += Math.log(data[n][1]) * Math.log(data[n][0]);
                        sum[2] += Math.log(data[n][1]);
                        sum[3] += Math.pow(Math.log(data[n][0]), 2);
                    }
                }

                var B = (n * sum[1] - sum[2] * sum[0]) / (n * sum[3] - sum[0] * sum[0]);
                var A = Math.pow(Math.E, (sum[2] - B * sum[0]) / n);

                for (var i = 0, len = data.length; i < len; i++) {
                    var coordinate = [data[i][0], A * Math.pow(data[i][0], B)];
                    results.push(coordinate);
                }

                var string = 'y = ' + Math.round(A * 100) / 100 + 'x^' + Math.round(B * 100) / 100;

                return {equation: [A, B], points: results, string: string};
            },

            polynomial: function (data, order) {
                if (typeof order == 'undefined') {
                    order = 2;
                }
                var lhs = [], rhs = [], results = [], a = 0, b = 0, i = 0, k = order + 1;

                for (; i < k; i++) {
                    for (var l = 0, len = data.length; l < len; l++) {
                        if (data[l][1]) {
                            a += Math.pow(data[l][0], i) * data[l][1];
                        }
                    }
                    lhs.push(a), a = 0;
                    var c = [];
                    for (var j = 0; j < k; j++) {
                        for (var l = 0, len = data.length; l < len; l++) {
                            if (data[l][1]) {
                                b += Math.pow(data[l][0], i + j);
                            }
                        }
                        c.push(b), b = 0;
                    }
                    rhs.push(c);
                }
                rhs.push(lhs);

                var equation = gaussianElimination(rhs, k);

                for (var i = 0, len = data.length; i < len; i++) {
                    var answer = 0;
                    for (var w = 0; w < equation.length; w++) {
                        answer += equation[w] * Math.pow(data[i][0], w);
                    }
                    results.push([data[i][0], answer]);
                }

                var string = 'y = ';

                for (var i = equation.length - 1; i >= 0; i--) {
                    if (i > 1) string += Math.round(equation[i] * 100) / 100 + 'x^' + i + ' + ';
                    else if (i == 1) string += Math.round(equation[i] * 100) / 100 + 'x' + ' + ';
                    else string += Math.round(equation[i] * 100) / 100;
                }

                return {equation: equation, points: results, string: string};
            },

            lastvalue: function (data) {
                var results = [];
                var lastvalue = null;
                for (var i = 0; i < data.length; i++) {
                    if (data[i][1]) {
                        lastvalue = data[i][1];
                        results.push([data[i][0], data[i][1]]);
                    }
                    else {
                        results.push([data[i][0], lastvalue]);
                    }
                }

                return {equation: [lastvalue], points: results, string: "" + lastvalue};
            },

            loess: function (data) {
                //adapted from the LoessInterpolator in org.apache.commons.math
                function loess_pairs(pairs, bandwidth) {
                    var xval = pairs.map(function (pair) {
                        return pair[0]
                    });
                    var yval = pairs.map(function (pair) {
                        return pair[1]
                    });
                    var res = loess(xval, yval, bandwidth);
                    return xval.map(function (x, i) {
                        return [x, res[i]];
                    });
                }

                function loess(xval, yval, bandwidth) {
                    function tricube(x) {
                        var tmp = 1 - x * x * x;
                        return tmp * tmp * tmp;
                    }

                    var res = [];

                    var left = 0;
                    var right = Math.floor(bandwidth * xval.length) - 1;

                    for (var i = 0; i < xval.length; i++) {

                        var x = xval[i];

                        if (i > 0) {
                            if (right < xval.length - 1 &&
                                xval[right + 1] - xval[i] < xval[i] - xval[left]) {
                                left++;
                                right++;
                            }
                        }

                        var edge;
                        if (xval[i] - xval[left] > xval[right] - xval[i])
                            edge = left;
                        else
                            edge = right;

                        var denom = Math.abs(1.0 / (xval[edge] - x));

                        var sumWeights = 0;
                        var sumX = 0, sumXSquared = 0, sumY = 0, sumXY = 0;

                        var k = left;
                        while (k <= right) {
                            var xk = xval[k];
                            var yk = yval[k];
                            var dist;
                            if (k < i) {
                                dist = (x - xk);
                            } else {
                                dist = (xk - x);
                            }
                            var w = tricube(dist * denom);
                            var xkw = xk * w;
                            sumWeights += w;
                            sumX += xkw;
                            sumXSquared += xk * xkw;
                            sumY += yk * w;
                            sumXY += yk * xkw;
                            k++;
                        }

                        var meanX = sumX / sumWeights;
                        var meanY = sumY / sumWeights;
                        var meanXY = sumXY / sumWeights;
                        var meanXSquared = sumXSquared / sumWeights;

                        var beta;
                        if (meanXSquared == meanX * meanX)
                            beta = 0;
                        else
                            beta = (meanXY - meanX * meanY) / (meanXSquared - meanX * meanX);

                        var alpha = meanY - beta * meanX;

                        res[i] = beta * x + alpha;
                    }

                    return res;
                }

                return {
                    equation: 'loess',
                    points: loess_pairs(data, 0.5),
                    string: 'loess'
                };
            }
        };

        return (function (method, data, order) {
            if (typeof method == 'string') {
                return methods[method](data, order);
            }
        });
    }());
    // jscs:enable
    var _ = tauCharts.api._;
    var d3 = tauCharts.api.d3;

    function trendline(xSettings) {

        var settings = _.defaults(
            xSettings || {},
            {
                type: 'linear',
                hideError: false,
                showPanel: true,
                showTrend: true,
                models: ['linear', 'exponential', 'logarithmic']
            });

        return {

            init: function (chart) {

                this._chart = chart;
                this._applicableElements = [
                    'ELEMENT.POINT',
                    'ELEMENT.LINE',
                    'ELEMENT.AREA',
                    'ELEMENT.INTERVAL'
                ];

                this._isApplicable = this.checkIfApplicable(chart);

                if (settings.showPanel) {

                    this._container = chart.insertToRightSidebar(this.containerTemplate);

                    var classToAdd = 'applicable-true';
                    if (!this._isApplicable) {
                        classToAdd = 'applicable-false';
                        this._error = [
                            'Trend line can\'t be computed for categorical data.',
                            'Each axis should be either a measure or a date.'
                        ].join(' ');
                    }

                    this._container.classList.add(classToAdd);

                    if (settings.hideError) {
                        this._container
                            .classList
                            .add('hide-trendline-error');
                    }

                    this.uiChangeEventsDispatcher = function (e) {

                        var target = e.target;
                        var selector = target.classList;

                        if (selector.contains('i-role-show-trend')) {
                            settings.showTrend = target.checked;
                        }

                        if (selector.contains('i-role-change-model')) {
                            settings.type = target.value;
                        }

                        this._chart.refresh();

                    }.bind(this);

                    this._container
                        .addEventListener('change', this.uiChangeEventsDispatcher, false);
                }
            },

            checkIfApplicable: function (chart) {

                var self = this;
                var specRef = chart.getSpec();
                var isApplicable = false;

                chart.traverseSpec(specRef, function (unit, parentUnit) {
                    if (self.predicateIsApplicable(specRef, unit, parentUnit)) {
                        isApplicable = true;
                    }
                });

                return isApplicable;
            },

            predicateIsApplicable: function (specRef, unit, parentUnit) {

                if (parentUnit && parentUnit.type !== 'COORDS.RECT') {
                    return false;
                }

                if (parentUnit && !parentUnit.units) {
                    return false;
                }

                if (this._applicableElements.indexOf(unit.type) === -1) {
                    return false;
                }

                var xScale = specRef.scales[unit.x];
                var yScale = specRef.scales[unit.y];

                return !(xScale.type === 'ordinal' || yScale.type === 'ordinal');
            },

            onSpecReady: function (chart, specRef) {

                var self = this;

                if (!settings.showTrend) {
                    return;
                }

                var periodGenerator = tauCharts.api.tickPeriod;
                var createPeriodCaster = function (period) {
                    var gen = periodGenerator.get(period);
                    return function (x) {
                        return gen.cast(new Date(x));
                    };
                };

                specRef.transformations = specRef.transformations || {};
                specRef.transformations.regression = function (data, props) {

                    var x = props.x.dim;
                    var y = props.y.dim;

                    var isXPeriod = (props.x.type === 'period' && props.x.period);
                    var isYPeriod = (props.y.type === 'period' && props.y.period);

                    var xMapper = isXPeriod ?
                        (createPeriodCaster(props.x.period)) :
                        (_.identity);

                    var yMapper = isYPeriod ?
                        (createPeriodCaster(props.y.period)) :
                        (_.identity);

                    var src = data.map(function (item) {
                        var ix = _.isDate(item[x]) ? item[x].getTime() : item[x];
                        var iy = _.isDate(item[y]) ? item[y].getTime() : item[y];
                        return [ix, iy];
                    });

                    var regression = regressionsHub(props.type, src);
                    var points = _(regression.points)
                        .chain()
                        .filter(function (p) {
                            return ((p[0] !== null) && (p[1] !== null));
                        })
                        .sortBy(function (p) {
                            return p[0];
                        })
                        .map(function (p) {
                            var item = {};
                            item[x] = xMapper(p[0]);
                            item[y] = yMapper(p[1]);
                            return item;
                        })
                        .value();

                    if ((points.length > 1) && (isXPeriod || isYPeriod)) {
                        points = [points[0], points[points.length - 1]];
                    }

                    return points.length > 1 ? points : [];
                };

                chart.traverseSpec(
                    specRef,
                    function (unit, parentUnit) {

                        if (!self.predicateIsApplicable(specRef, unit, parentUnit)) {
                            return;
                        }

                        var xScale = specRef.scales[unit.x];
                        var yScale = specRef.scales[unit.y];

                        var trend = JSON.parse(JSON.stringify(unit));

                        trend.type = 'ELEMENT.LINE';
                        trend.namespace = 'trendline';
                        trend.transformation = trend.transformation || [];
                        trend.transformation.push({
                            type: 'regression',
                            args: {
                                type: settings.type,
                                x: xScale,
                                y: yScale
                            }
                        });
                        trend.guide = trend.guide || {};
                        trend.guide.showAnchors   = false;
                        trend.guide.cssClass      = 'graphical-report__trendline';
                        trend.guide.widthCssClass = 'graphical-report__line-width-1';

                        parentUnit.units.push(trend);
                    });
            },

            // jscs:disable maximumLineLength
            containerTemplate: '<div class="graphical-report__trendlinepanel"></div>',
            template: _.template([
                '<label class="graphical-report__trendlinepanel__title graphical-report__checkbox">',
                '<input type="checkbox" class="graphical-report__checkbox__input i-role-show-trend" <%= showTrend %> />',
                '<span class="graphical-report__checkbox__icon"></span>',
                '<span class="graphical-report__checkbox__text">',
                '<%= title %>',
                '</span>',
                '</label>',

                '<div>',
                '<select class="i-role-change-model graphical-report__select graphical-report__trendlinepanel__control">',
                '<%= models %>',
                '</select>',
                '</div>',

                '<div class="graphical-report__trendlinepanel__error-message"><%= error %></div>'
            ].join('')),
            // jscs:enable maximumLineLength

            onRender: function (chart) {

                if (this._container) {
                    this._container.innerHTML = this.template({
                        title: 'Trend line',
                        error: this._error,
                        showTrend: (settings.showTrend && this._isApplicable) ? 'checked' : '',
                        models: settings.models.map(function (x) {
                            var selected = (settings.type === x) ? 'selected' : '';
                            return '<option ' + selected + ' value="' + x + '">' + x + '</option>';
                        })
                    });

                    var handleMouse = function (isActive) {
                        return function () {
                            d3
                                .select(this)
                                .classed({
                                    active: isActive,
                                    'graphical-report__line-width-1': !isActive,
                                    'graphical-report__line-width-3': isActive
                                });
                        };
                    };

                    var canv = d3.select(chart.getSVG());
                    canv.selectAll('.graphical-report__trendline')
                        .on('mouseenter', handleMouse(true))
                        .on('mouseleave', handleMouse(false));
                }
            }
        };
    }

    tauCharts.api.plugins.add('trendline', trendline);

    return trendline;
});