/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import xml.{NodeBuffer, Text, NodeSeq}
import runtime.BoxedUnit

/**
 * Typeclass for rendering objects of a specific type. Implement one of these and import it
 * in your notebook to change how objects of the specified type get presented after evaluation.
 */
trait Renderer[-A] {
  def render(value: A): NodeSeq
}

class WidgetRenderer[-A](toWidget: A => Widget) extends Renderer[A] {
  def render(value: A) = toWidget(value).toHtml
}

object Renderer extends LowPriorityRenderers {
  implicit object htmlAsItself extends Renderer[NodeSeq] {
    def render(value: NodeSeq) = value
  }
  implicit object nodeBufferAsItself extends Renderer[NodeBuffer] {
    def render(value: NodeBuffer) = value
  }
  implicit object widgetAsItself extends Renderer[Widget] {
    def render(value: Widget) = value.toHtml
  }
  implicit object stringAsItself extends Renderer[String] {
    def render(value: String) = Text(value)
  }
  implicit object anyValAsItself extends Renderer[AnyVal] {
    def render(value: AnyVal) = if (value == BoxedUnit.UNIT) NodeSeq.Empty else Text(value.toString)
  }
}

trait LowPriorityRenderers {
  implicit object seqAsColumn extends Renderer[Seq[_]] {
    def render(x: Seq[_]) = {
      val values = if (x.lengthCompare(25) < 0) x.map(_.toString) else x.take(24).map(_.toString) :+ "..."
      widgets.column(values.map(widgets.text(_)):_*)
    }
  }
}