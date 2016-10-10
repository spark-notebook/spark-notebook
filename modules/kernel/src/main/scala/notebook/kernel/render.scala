package notebook.kernel

import notebook.front.Renderer

object render {
  def apply[A](a: A)(implicit renderer: Renderer[A]) = renderer.render(a)
}
