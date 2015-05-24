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
