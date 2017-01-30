package notebok.util

import org.scalatest._
import notebook.util.Reflector

class ReflectorTests extends FunSpec with Matchers with BeforeAndAfterAll {

  object SomeObject {
    val a = "aValue"
    val b = "bValue"
  }

  case class SomeCaseClass(a: String)

  class SomeClass {
    val a = "aValue"
  }

  describe("Reflector.toObjArray (returns fieldName -> value pairs)") {
    it("works with objects") {
      Reflector.toObjArray(SomeObject).toMap shouldBe Map("a" -> "aValue", "b" -> "bValue")
    }

    it("works with case classes") {
      Reflector.toObjArray(SomeCaseClass(a = "aValue")).toMap shouldBe Map("a" -> "aValue")
    }

    it("works with regular classes") {
      Reflector.toObjArray(new SomeClass).toMap shouldBe Map("a" -> "aValue")
    }
  }

  describe("Reflector.toFieldNameArray") {
    it("returns fieldNames") {
      Reflector.toFieldNameArray(SomeObject) shouldBe List("a", "b")
    }
  }

  describe("Reflector.toFieldValueArray") {
    it("returns field values") {
      Reflector.toFieldValueArray(SomeObject) shouldBe List("aValue", "bValue")
    }
  }

}
