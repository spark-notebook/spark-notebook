package notebook.front

import xml.{NodeSeq, UnprefixedAttribute, Null}
import play.api.libs.json._
import notebook._
import notebook._
import JsonCodec._
import scala.util.Random
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.Json.JsValueWrapper

/**
 * This package contains primitive widgets that can be used in the child environment.
 */
package object widgets {
  def scopedScript(content: String, data: JsValue = null, selector:Option[String]=None) = {
    val tag = <script type="text/x-scoped-javascript">/*{xml.PCData("*/" + content + "/*")}*/</script>
    val withData = if (data == null)
      tag
    else
      tag % new UnprefixedAttribute("data-this", Json.stringify(data), Null)

    val withSelector = selector.map { s =>
      withData % new UnprefixedAttribute("data-selector", s, Null)
    }.getOrElse(withData)



    withSelector
  }

  def text(value: String) = html(xml.Text(value))

  def text(value: Connection[String], style: Connection[String] = Connection.just("")) = {
    val _currentValue = JSBus.createConnection
    val stringCodec:Codec[JsValue, String] = formatToCodec(implicitly[Format[String]])
    val currentValue = _currentValue biMap stringCodec
    currentValue <-- value

    val _currentStyle = JSBus.createConnection
    val currentStyle = _currentStyle biMap stringCodec
    currentStyle <-- style

    html(<p data-bind="text: value, style: style">{
      scopedScript(
        """ req(
              ['observable', 'knockout'],
              function (O, ko) {
                ko.applyBindings({
                    value: O.makeObservable(valueId),
                    style: O.makeObservable(styleId)
                  },
                  this
                );
              }
        """,
        Json.obj("valueId" -> _currentValue.id, "styleId" -> _currentStyle.id)
      )}</p>)
  }

  def out = new SingleConnectedWidget[String] {
    implicit val codec:Codec[JsValue, String] = formatToCodec(Format.of[String])

    lazy val toHtml = <p data-bind="text: value">{
      scopedScript(
        """
            req(
              ['observable', 'knockout'],
              function (O, ko) {
                ko.applyBindings({
                    value: O.makeObservable(valueId)
                  },
                  this
                );
              }
            );
        """,
        Json.obj("valueId" -> dataConnection.id)
      )}</p>
  }

  import java.awt.image.BufferedImage
  import java.io.ByteArrayOutputStream
  import javax.imageio.ImageIO

  def imageCodec(tpe:String) = new Codec[JsValue, BufferedImage] {
    def toBytes(bi:BufferedImage):String = {
      val bos = new ByteArrayOutputStream()
      ImageIO.write(bi, tpe, bos)
      val imageBytes = bos.toByteArray()
      val encodedImage = org.apache.commons.codec.binary.Base64.encodeBase64String(imageBytes)
      val imageString = "data:image/"+tpe+";base64,"+encodedImage
      bos.close()
      imageString
    }
    def decode(a: BufferedImage):JsValue = JsString(toBytes(a))
    def encode(v: JsValue):BufferedImage = ??? //todo
  }

  def img(tpe:String="png", width:String="150px", height:String="150px") = new SingleConnectedWidget[BufferedImage] {
    implicit val codec:Codec[JsValue, BufferedImage] = imageCodec(tpe)

    lazy val toHtml = <p>
      <img width={width} height={height} data-bind="attr:{src: value}" />
        {
          scopedScript(
            """
                req(
                  ['observable', 'knockout'],
                  function (O, ko) {
                    ko.applyBindings({
                        value: O.makeObservable(valueId)
                      },
                      this
                    );
                  }
                );
            """,
            Json.obj("valueId" -> dataConnection.id)
          )
        }
      </p>

      def url(u:java.net.URL) = apply(ImageIO.read(u))
      def file(f:java.io.File) = apply(ImageIO.read(f))

  }


  def html(html: NodeSeq): Widget = new SimpleWidget(html)
  
  def barChart(width: Int, jsons: Seq[(String, Any)]) = {
    val id = Math.abs(Random.nextInt).toString
    
    	<svg id={ "bar"+id } width="400px" height="400px"
       xmlns="http://www.w3.org/2000/svg" version="1.1">
    	{
      scopedScript(
        s""" req(
              ['d3', 'dimple'],
              function (O, x) {
                var svg = d3.select("#bar${id}");
    		  	var chart = new dimple.chart(svg, data);
        		chart.setBounds(30, 20, 380, 360);
    		  	chart.addCategoryAxis("x", "${jsons(0)._1.trim}");
    		  	chart.addMeasureAxis("y", "${jsons(1)._1.trim}");
    		  	chart.addSeries(null, dimple.plot.bar);
    		  	chart.draw();
          
              });
        """,
        Json.obj( "data" -> (jsons grouped width map { row =>  Json.obj(row.map( f => f._1.trim -> toJson(f._2) ):_*)  }).toSeq    )
      )
    } </svg>
    
  }
  
  def pieChart(width: Int, jsons: Seq[(String, Any)]) = {
    val id = Math.abs(Random.nextInt).toString
    
	    <svg id={ "pie"+id } width="400px" height="400px"
       xmlns="http://www.w3.org/2000/svg" version="1.1">
	    {
	      scopedScript(
	        s""" req(
	              ['d3', 'dimple'],
	              function (O, x) {
	                  var svg = d3.select("#pie${id}");
				      var myChart = new dimple.chart(svg, data);
				      myChart.setBounds(50, 50, 350, 350);
	        		  myChart.addMeasureAxis("p", "${jsons(1)._1.trim}");
				      myChart.addSeries("${jsons(0)._1.trim}", dimple.plot.pie);
				      myChart.addLegend(350, 30, 80, 200, "left");
				      myChart.draw();
	              });
	        """,
	        Json.obj( "data" -> (jsons grouped width map { row =>  Json.obj(row.map( f => f._1.trim -> toJson(f._2) ):_*)  }).toSeq    )
	      )
	    } 
    </svg>
    
  }
  
  def tabControl(pages: Seq[(String, scala.xml.Elem)]) = {
    val tabControlId = Math.abs(Random.nextInt).toString
    html(
    <div style="width:600px; height:450px;" >
    	<script>{
	        s""" 
	        	$$('#ul${tabControlId} li').first().addClass('active');
	        	$$('#tab${tabControlId} div').first().addClass('active');
    			$$('#ul${tabControlId} a').click(function(){
    				$$('#tab${tabControlId} div.active').removeClass('active');
    				$$('#ul${tabControlId} li.active').removeClass('active');
    				var id = $$(this).attr('href');
    				$$(id).addClass('active');
    				$$(this).parent().addClass('active');
    			});
	        """
	      }
    	</script>	
    <ul class="nav nav-tabs" id={ "ul"+tabControlId }>{
    	pages.zipWithIndex map { p: ((String, scala.xml.Elem), Int) => 
		    <li>
    			<a href={ "#tab"+tabControlId+"-"+p._2 }><i class={p._1._1} />{ p._1._1 }</a>
    		</li>
		  }
    }
    </ul>

    <div class="tab-content" id={ "tab"+tabControlId }>
      	
    	{
		  pages.zipWithIndex map { p: ((String, scala.xml.Elem), Int) => 
		    <div class="tab-pane" id={ "tab"+tabControlId+"-"+p._2 }>
	    	{ p._1._2 }
	    	</div>
		  }
    	}
    </div>
</div>
	  )
  }
 
  
  def toJson(obj: Any): JsValueWrapper = { 
    obj match {
          case v: Int => JsNumber(v)
          case v: Float => JsNumber(v)
          case v: Double => JsNumber(v)
          case v: String => JsString(v)
          case v: Boolean => JsBoolean(v)
          case v: Any => JsString(v.toString)
        }
  }
  
  def layout(width: Int, contents: Seq[Widget], headers: Seq[Widget] = Nil): Widget = html(table(width, contents, headers ))
    
  def table(width: Int, contents: Seq[Widget], headers: Seq[Widget] = Nil) = 
    <div>
  	<table>{
      (headers ++ contents) grouped width map { row =>
        <tr>{
          row map { html => <td>{html}</td> }
        }</tr>
      }
    }</table></div>

  def row(contents: Widget*) = layout(contents.length, contents)
  def column(contents: Widget*) = layout(1, contents)

  def multi(widgets: Widget*) = html(NodeSeq.fromSeq(widgets.map(_.toHtml).flatten))
}