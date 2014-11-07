package notebook.front.widgets

import org.json4s.JsonAST._
import org.json4s.JsonDSL._

import org.apache.spark.SparkContext

import notebook.Codec
import notebook.JsonCodec._
import notebook.front.SingleConnectedWidget

class Spark(init:SparkContext) extends SingleConnectedWidget[Map[String, String]] {
  private[this] var sparkContext:SparkContext = init

  implicit val codec:Codec[JValue, Map[String, String]] = tMap[String]

  currentData.observable.inner.subscribe{ m =>
    val sparkConf = sparkContext.getConf
    val newConf = m.foldLeft(sparkConf)((acc, e) => acc.set(e._1, e._2))
    sparkContext.stop()
    sparkContext = new SparkContext(newConf) // BAD but probably temporary... hem hem
  }

  val toHtml = <div>{
      scopedScript(
        """require( ['observable', 'knockout', 'jquery'],
                    function (Observable, ko, $) {
                      var value = Observable.makeObservable(valueId);
                      var publishSparkConf = function(form) {
                        var r = $(form).serializeArray();
                        var result = {};
                        r.forEach(function(o) {
                          result[o.name] = o.value;
                        });
                        value(result);
                      };
                      var addEntry = function(form) {
                        var entry = $(form).serializeArray()[0];
                        $("#update-spark-form").find("button")
                                               .before("<div class='form-group'><label for-name='"+entry.value+"'>"+entry.value+"</label><input class='form-control' name='"+entry.value+"' value=''/></div>")
                      };
                      ko.applyBindings({
                        formShown:        ko.observable(false),
                        value:            value,
                        publishSparkConf: publishSparkConf,
                        addEntry:         addEntry
                      }, this);
                    }
                  )""",
        ("valueId" -> dataConnection.id)
      )
    }
    <span class="help-block">
      <input type="checkbox" data-bind="checked: formShown" />Update Spark configuration
    </span>
    <div data-bind="if: formShown">
      <form role="form" id="update-spark-form" data-bind="submit: publishSparkConf">
        {
          sparkContext.getConf.getAll.map { case (k, v) =>
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