package com.datafellas.g3nerator.model

/**
  * Renders objects to configuration
  */

trait Renderable[T] {
  def render(obj:T, prefix: String): String
}

object Renderable {
  def render[T:Renderable](obj:T, prefix: String): String = {
    val r = implicitly[Renderable[T]]
    r.render(obj, prefix)
  }

  def render[T:Renderable](list:List[T], prefix: String): String = {
    val r = implicitly[Renderable[T]]
    list.map(elem => r.render(elem, prefix)).mkString("\n")
  }

  implicit object stringConfigRender extends Renderable[StringConfig] {
    def render(obj: StringConfig, prefix:String) =
      s"""|
          |$prefix.${obj.key}= "${obj.value}"
          |""".stripMargin
  }
}

case class StringConfig(key:String, value:String)
