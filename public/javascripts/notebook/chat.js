define([
  'jquery',
  'base/js/events',
  'knockout',
  'underscore'
], function($, events, ko, _) {
  events.on('Observable.ready', function(){
    require(['observable'], function(O) {
      var ChatModel = function() {
        var self = this;

        this.alias = ko.observable(null);
        this.joined = ko.computed(function() {
          return ! _.isNull(self.alias());
        });
        this.messages = O.makeObservableArray("chat");
        this.new_message = ko.observable(null);
        this.new_message.subscribe(function(msg) {
          if (!msg || !msg.trim().length || msg.alias == self.alias()) return;
          self.messages.push({
            content: msg,
            time: new Date(),
            alias: self.alias()
          });
          self.new_message('');
        });
        this.aliasing = ko.observable(false);
        this.chatting = ko.observable(false);
        this.isChatting = ko.computed(function() {
          return self.chatting() || self.aliasing();
        });
        this.isChatting.subscribe(function(is) {
          if (is) {
            IPython.notebook.keyboard_manager.disable();
          } else {
            IPython.notebook.keyboard_manager.enable();
          }
        });
      }


      model = new ChatModel();
      var chatRoom = $("#chat-room").get(0);
      ko.cleanNode(chatRoom);
      ko.applyBindings({ chat: model }, chatRoom);

      var messagePanel = $('.chat').get(0);
      window.setInterval(function() {
        messagePanel.scrollTop = messagePanel.scrollHeight;
      }, 1000);
    });
  });
});