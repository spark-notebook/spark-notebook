/*
 *  CSRF prevention via custom HTTP request header transmitted with every JQuery AJAX request.
 *  Header value is extracted from a cookie that must be set by the server.
 */



  var IPython = (function (IPython) {

	  var CSRFToken = 'CSRF-Key';
	  
    /* TODO: Move to a better home */
    var getCookie = function(name) {
        var extName = name + '='
        var cValue = null
        if (document.cookie && document.cookie != '') {
          var cookies = document.cookie.split(';')
          for (var i = 0; i < cookies.length; i++) {
            var cookie = jQuery.trim(cookies[i])
            if (cookie.substring(0, extName.length) == extName) {
              cValue = decodeURIComponent(cookie.substring(extName.length));
              break;
            }
          }
        }
        return cValue
    };

    var CSRF = function () {
        var csrfKey = getCookie(CSRFToken)
        if (typeof(Storage) !== "undefined") {
           if (csrfKey) { /* Move the key into local storage */
               sessionStorage.csrfKey = csrfKey
               document.cookie = CSRFToken + "=invalid; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT"
           } else if (sessionStorage.csrfKey)
              csrfKey = sessionStorage.csrfKey
           else if (window.opener && window.opener.IPython) csrfKey = window.opener.IPython.CSRF.key
        }
        this.key = csrfKey
    };


    CSRF.prototype.postAction = function (url) {
        var pForm = document.createElement("form")
        pForm.setAttribute("method", "post")
        pForm.setAttribute("action", url)
        pForm.setAttribute("target", "_blank")

        var csrfField = document.createElement("input")
        csrfField.setAttribute("name", CSRFToken)
        csrfField.setAttribute("value", this.key)
        pForm.appendChild(csrfField)

        document.body.appendChild(pForm)
        pForm.submit();
        document.body.removeChild(pForm)
    };

    IPython.CSRF = new CSRF();
    
    
    var origAjaxSetup = jQuery.ajaxSetup
    var csrfKey = IPython.CSRF.key
    if (csrfKey) {
      jQuery.ajaxSetup = function(target, settings) {
        var result = origAjaxSetup(target, settings)
        if (result.headers) result.headers[CSRFToken] = csrfKey
        else {
        	result.headers = {}
        	result.headers[CSRFToken] = csrfKey
        }
        return result
      }
    }
    
    return IPython;
  }(IPython));
