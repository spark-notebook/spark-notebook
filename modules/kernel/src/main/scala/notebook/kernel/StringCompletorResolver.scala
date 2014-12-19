/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package notebook.kernel

//import org.clapper.util.classutil.ClassUtil
import notebook.util.StringCompletor

object StringCompletorResolver {
  lazy val completor = {
    val className = "notebook.kernel.TestStringCompletor"
    //ClassUtil.instantiateClass(className).asInstanceOf[StringCompletor]
      Class.forName(className).getConstructor().newInstance().asInstanceOf[StringCompletor]
  }
}
