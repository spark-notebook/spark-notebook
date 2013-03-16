/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import util.ClassUtils
import xml.{Node, NodeSeq}
import java.util.UUID
import scalaz._

trait Widget extends Iterable[Node] {
  def toHtml: NodeSeq

  def iterator = toHtml.iterator

  def ++(other: Widget): Widget = toHtml ++ other

  override def toString = "<" + ClassUtils.getSimpleName(getClass) + " widget>"
}

class SimpleWidget(html: NodeSeq) extends Widget {
  def toHtml = html
  override def toString = "<widget>"
}

object Widget {
  implicit def toHtml(widget: Widget): NodeSeq = widget.toHtml
  def fromHtml(html: NodeSeq): Widget = new SimpleWidget(html)

  implicit def fromRenderer[A](value: A)(implicit renderer: Renderer[A]): Widget = fromHtml(renderer.render(value))

  object Empty extends Widget {
    def toHtml = NodeSeq.Empty
    override def toString = "<empty widget>"
  }

  implicit val widgetInstances = new Monoid[Widget] {
    def zero = Empty
    def append(s1: Widget, s2: ⇒ Widget) = s1 ++ s2
  }

  // We're stripping out dashes because we want these to be valid JS identifiers.
  // Prepending with the "obs_" accomplishes that as well in that it forces it to
  // start with a letter, but it also helps make the namespace a little more
  // manageable.
  @deprecated("Avoid using IDs in widgets, to support the same widget appearing in multiple places on a page.", "1.0")
  def generateId = "widget_" + UUID.randomUUID().toString.replaceAll("-", "")
}
