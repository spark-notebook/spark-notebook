/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package notebook.kernel

import notebook.front.Renderer

object render {
  def apply[A](a: A)(implicit renderer: Renderer[A]) = renderer.render(a)
}