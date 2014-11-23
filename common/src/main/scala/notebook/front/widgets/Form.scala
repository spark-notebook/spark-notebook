package notebook.front.widgets

import org.json4s.JsonAST._
import org.json4s.JsonDSL._

import notebook.Codec
import notebook.JsonCodec._
import notebook.front.SingleConnectedWidget

trait Form[D] extends SingleConnectedWidget[Map[String, String]] {
  def title:String
  def initData:D
  def paramsCodec:Codec[D, Map[String, String]]
  def update:D => D

  private[this] val htmlId = "_"+java.util.UUID.randomUUID.toString.replaceAll("\\W", "_")

  private[widgets] var data:D = initData
  implicit val codec:Codec[JValue, Map[String, String]] = tMap[String]

  currentData.observable.inner.subscribe{ m =>
    data = paramsCodec.decode(m)
    update(data) // because the whole reactive `workflow` is not there... yet?
  }

  lazy val toHtml = <div>{
      scopedScript(
        s"""require( ['observable', 'knockout', 'jquery'],
                    function (Observable, ko, $$) {
                      var value = Observable.makeObservable(valueId);
                      var publishFormData = function(form) {
                        var r = $$(form).serializeArray();
                        var result = {};
                        r.forEach(function(o) {
                          result[o.name] = o.value;
                        });
                        value(result);
                      };
                      var addEntry = function(form) {
                        var entry = $$(form).serializeArray()[0];
                        $$("#$htmlId").find("button")
                                               .before("<div class='form-group'><label for-name='"+entry.value+"'>"+entry.value+"</label><input class='form-control' name='"+entry.value+"' value=''/></div>")
                      };
                      ko.applyBindings({
                        formShown:        ko.observable(false),
                        value:            value,
                        publishFormData:  publishFormData,
                        addEntry:         addEntry
                      }, this);
                    }
                  )""",
        ("valueId" -> dataConnection.id)
      )
    }
    <span class="help-block">
      <input type="checkbox" data-bind="checked: formShown" /><strong>{title}</strong>
    </span>
    <div data-bind="if: formShown">
      <form role="form" id={htmlId} data-bind="submit: publishFormData">
        {
          paramsCodec.encode(data).map { case (k, v) =>
            <div class="form-group">
              <label for-name={k}>{k}</label>
              <input name={k} value={v} class="form-control"/>
            </div>
          }
        }
        <button type="submit" class="btn btn-default">Change</button>
      </form>
      <form data-bind="submit: addEntry" role="form">
        <div class="form-group">
          <label for-name="add-entry">Add entry</label>
          <input name="add-entry" class="form-control" type="text" />
        </div>
        <button type="submit" class="btn btn-default">Add</button>
      </form>
    </div>
  </div>
}