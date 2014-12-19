define(['jquery', 'knockout', 'equiv'], function ($, ko, equiv) {

return new function () {

    this.observables = {};
    this.internalObservables = {};
    this.channel = null;
    this.base_url = $('body').data('baseObservableUrl');

    if (typeof(WebSocket) !== 'undefined') {
        this.WebSocket = WebSocket;
    } else if (typeof(MozWebSocket) !== 'undefined') {
        this.WebSocket = MozWebSocket;
    } else {
        alert('Your browser does not have WebSocket support, please try Chrome, Safari or Firefox ≥ 6. Firefox 4 and 5 are also supported but you have to enable WebSockets in about:config.');
    };

    this.start = function() {
    	if (this.channelID === IPython.notebook.kernel.kernel_id) return;
        var that = this;
        this.stop_channels();
        var ws_url = this.base_url;
        console.log("Starting observable WS:", ws_url);
        this.channelID = IPython.notebook.kernel.kernel_id
        this.channel = new this.WebSocket(ws_url + '/' + IPython.notebook.kernel.kernel_id); // TODO: This is a hack, obviously; fix when we support observable stuff outside of Scala Notebook
        send_cookie = function(){
            this.send(document.cookie);
        };
        var already_called_onclose = false; // only alert once
        ws_closed_early = function(evt){
            if (already_called_onclose){
                return;
            }
            already_called_onclose = true;
            if ( ! evt.wasClean ){
                that._websocket_closed(ws_url, true);
            }
        };
        ws_closed_late = function(evt){
            if (already_called_onclose){
                return;
            }
            already_called_onclose = true;
            if ( ! evt.wasClean ){
                that._websocket_closed(ws_url, false);
            }
        };
        this.channel.onopen = send_cookie;
        this.channel.onclose = ws_closed_early;
        // switch from early-close to late-close message after 1s
        setTimeout(function(){
           that.channel.onclose = ws_closed_late;
        }, 1000);

        this.channel.onmessage = $.proxy(this.handle_channel_reply, this);
    };

    this.handle_channel_reply = function (e) {
        reply = $.parseJSON(e.data);
        this.update_observable(reply);
    };

    this.stop_channels = function () {
        if (this.channel !== null) {
            this.channel.onclose = function (evt) {};
            this.channel.close();
            this.channel = null;
        };
        delete this.channelId;
    };

    this._websocket_closed = function(ws_url, early) {
        var msg;
        var parent_item = $('body');
        if (early) {
            msg = "Websocket connection to " + ws_url + " could not be established." +
            " You will NOT be able to run code." +
            " Your browser may not be compatible with the websocket version in the server," +
            " or if the url does not look right, there could be an error in the" +
            " server's configuration.";
        } else {
            msg = "Websocket connection closed unexpectedly." +
            " The kernel will no longer be responsive.";
        }
        this.stop_channels();
        var dialog = $('<div/>');
        dialog.html(msg);
        parent_item.append(dialog);
        dialog.dialog({
            resizable: false,
            modal: true,
            title: "Websocket closed",
            closeOnEscape: true,
            closeText: "",
            close: function(event, ui) {$(this).dialog('destroy').remove();},
            buttons : {
                "OK": function () {
                    $(this).dialog('close');
                }
            }
        });

    };



    this.makeObservableHelper = function (id, kind, initialValue) {
        var observable = this.observables[id];
        if (typeof observable === 'undefined') {
        	console.log("Creating new observable (client request): " + id)
            observable = ko[kind](initialValue);
            this.register_observable(id, observable);
        } else if (typeof initialValue !== 'undefined') {
            this.observableSetIfChanged(observable, initialValue);
        }
        return observable;
    };

    this.observableSetIfChanged = function (observable, newValue) {
        if (!equiv(observable(), newValue)) {
            observable(newValue);
        }
    };

    this.makeObservable = function (id, initialValue) {
        return this.makeObservableHelper(id, 'observable', initialValue);
    };

    this.makeObservableArray = function (id, initialValue) {
        return this.makeObservableHelper(id, 'observableArray', initialValue);
    };


    ko.observable.fn.noEcho = function () {
        var obs = this;
        var firing = false;
        var noEchoObs = function () {
            firing = true;
            var result = obs.apply(this, arguments);
            firing = false;
            return result;
        };
        noEchoObs.subscribe = function (subscription) {
            obs.subscribe(function () {
               if (!firing) {
                   return subscription.apply(this, arguments);
           	   }
            });
        };
        return noEchoObs;
    };

    this.notify_dom_change = function (wId, newDomValue) {
        if (this.isInitialized()) {
            var content = {
                            id: wId,
                            new_value: newDomValue
                          };
            console.log("Observable -> Server");
            console.log(content);
            var msg = content;
            var msgString = JSON.stringify(msg);
            if (msgString.length < 8192) {
                this.channel.send(msgString);
            } else {
    //            $.ajax({
    //                type: 'POST',
    //                url: this.kernel_url + '/publish/' + wId,
    //                data: msgString,
    //                contentType: 'application/json'
    //            });
                throw "Message too long, implement post";
            }
        }
    };


    this.register_observable = function (id, obs) {
        var noEcho = obs.noEcho();
        this.observables[id] = obs;
        this.internalObservables[id] = noEcho;
        var that = this;
        noEcho.subscribe(function(newValue) {
            setTimeout(function () {
                that.notify_dom_change(id, newValue);
            }, 0);
        });
    };

    this.update_observable = function (data) {
        var noEcho = this.internalObservables[data.id];
        if (typeof noEcho === 'undefined') {
        	console.log("Creating new observable (server request), id: " +data.id+ " initial value: " + JSON.stringify(data.new_value))
            var observable = ko.observable(data.new_value);
            this.register_observable(data.id, observable);
        } else {
//        	console.log("Server update for observable: " + data.id + " value: " + JSON.stringify(data.new_value))
            this.observableSetIfChanged(noEcho, data.new_value);
        }
    };

    this.isInitialized = function () {
        return this.channel != null;
    };

    // TODO: This is not a great place for this function; put it somewhere else
    /** Returns a thunk to be evaluated to execute the scoped script tags.
     */
    this.scopedEval = function (toEval) {
        var callbacks = $('script[type="text/x-scoped-javascript"]', toEval).map(function () {
            var data = $.parseJSON($(this).attr('data-this')) || {};
            var scope = this.parentElement;
            var source = this.textContent;
            $(this).remove();

            function require(requirements, callback) {
              curl(requirements, function () { callback.apply(scope, arguments); });
            }

            return function () { (function() { with (data) { eval(source); } }).call(scope); };
        });
        return function () { callbacks.each(function () { this.call(); }); };
    };

    this.start();
};

});