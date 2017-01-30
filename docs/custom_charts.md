# Documentation

## Creating your own custom Visualizations

If you want/need to exerce your js fu, you can always use the `Chart` (for instance) API to create new dynamic widgets types.

In the following, we'll create a widget that can plot duration bars based for given operations (only a name):
* a `js` string which is the javascript to execute for the new chart. It:
  * has to be a function with 3 params
    * `dataO` a knockout observable wich can be listened for new incoming data, see the `subscribe` call
    * `container` is the div element where you can add new elements
    * `options` is an extra object passed to the widget which defines additional configuration options (like width or a specific color or whatever)
  * has a `this` object containing:
    * `dataInit` this is the JSON representation of the Scala data as an array of objects having the same schema as the Scala type
    * `genId` a unique id that you can use for a high level element for instance

```scala
val js = """
function progressgraph (dataO, container, options) {
  var css = 'div.prog {position: relative; overflow: hidden; } span.pp {display: inline-block; position: absolute; height: 16px;} span.prog {display: inline-block; position: absolute; height: 16px; }' +
            '.progs {border: solid 1px #ccc; background: #eee; } .progs .pv {background: #3182bd; }',
      head = document.head || document.getElementsByTagName('head')[0],
      style = document.createElement('style');

  style.type = 'text/css';
  if (style.styleSheet){
    style.styleSheet.cssText = css;
  } else {
    style.appendChild(document.createTextNode(css));
  }

  head.appendChild(style);


  var width = options.width||600
  var height = options.height||400
  
  function create(name, duration) {
    var div =  d3.select(container).append("div").attr("class", "prog");

    div.append("span").attr("class", "pp prog")
        .style("width", "74px")
        .style("text-align", "right")
        .style("z-index", "2000")
        .text(name);

    div.append("span")
        .attr("class", "progs")
        .style("width", "240px")
        .style("left", "80px")
      .append("span")
        .attr("class", "pp pv")
      .transition()
        .duration(duration)
        .ease("linear")
        .style("width", "350px");

    div.transition()
        .style("height", "20px")
      .transition()
        .delay(duration)
        .style("height", "0px")
        .remove();

  }

  function onData(data) {
    _.each(data, function(d) {
      create(d[options.name], 5000 + d[options.duration])
    });
  }

  onData(this.dataInit);
  dataO.subscribe(onData);
}
""".trim
```

Now we can create the widget extending `notebook.front.widgets.charts.Chart[C]`, where `C` is any Scala type, it'll be converted to JS using the implicit instance of `ToPoints`.

It has to declare the original dataset which needs to be a wrapper (`List`, `Array`, ...) of the `C` instances we want to plot. But it can also define other things like below:
* `sizes` are the $w \times h$ dimension of the chart
* `maxPoints` the number of points to plot, the way to select them is defined in the implicitly available instance of `Sampler`.
* `scripts` a list of references to existing javascript scripts
* `snippets` a list of string that represent snippets to execute in JS, they take the form of a JSON object with
  * `f` the function to call when the snippet will be executed
  * `o` a JSON object that will be provided to the above function at execution time. Here we define which field has to be used for the name and duration.

  ```scala
  import notebook.front.widgets._
import notebook.front.widgets.magic._
import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.magic.SamplerImplicits._
case class ProgChart[C:ToPoints:Sampler](
  originalData:C,
  override val sizes:(Int, Int)=(600, 400),
  maxPoints:Int = 1000,
  name:String,
  duration:String
) extends notebook.front.widgets.charts.Chart[C](originalData, maxPoints) {
  def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = t.data.toSeq


  override val snippets = List(s"""|{
                                   |  "f": $js, 
                                   |  "o": {
                                   |    "name": "$name",
                                   |    "duration": "$duration"
                                   |  }
                                   |}
                                  """.stripMargin)
  
  override val scripts = Nil
}
```

We can define the type of data we'll use for this example

```scala
case class ProgData(n:String, v:Int)
```

Then we generate a bunch of data bucketized by 10, and we create an instance of the new widget giving it the first bucket of data and specifying the right field names for `name` and `duration`.

```scala
val pdata = for {
  c1 <- 'a' to 'e'
  c2 <- 'a' to 'e'
} yield ProgData(""+c1+c2, (nextDouble * 10000).toInt)
val pdataH :: pdataS = pdata.toList.sliding(10, 10).toList
```

And finally we can display our custom chart

```scala
val pc = ProgChart(pdataH, name = "n", duration = "v")
pc
```

![Fully Customized  Chart](./images/widgets-custom.png)
