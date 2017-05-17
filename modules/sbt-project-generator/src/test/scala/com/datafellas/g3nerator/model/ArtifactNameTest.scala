package com.datafellas.g3nerator.model

import org.scalatest.{Matchers, WordSpec}
import org.scalatest.prop.TableDrivenPropertyChecks._

class ArtifactNameTest extends WordSpec with Matchers {

  val names = Table (
    ("notebook", "debian", "class", "package", "library"),
    ("nb", "nb" , "Nb", "nb", "nb"),
    ("my_notebook", "my-notebook", "MyNotebook", "mynotebook", "my-notebook"),
    ("dash-notebook", "dash-notebook", "DashNotebook", "dashnotebook", "dash-notebook"),
    ("great-stuff!!!", "great-stuff", "GreatStuff", "greatstuff", "great-stuff"),
    ("wow notebook","wow-notebook","WowNotebook","wownotebook", "wow-notebook"),
    ("mega_wow_notebook","mega-wow-notebook","MegaWowNotebook","megawownotebook", "mega-wow-notebook"),
    ("shit#!^!!_doesnt_work","shit-doesnt-work","ShitDoesntWork","shitdoesntwork", "shit-doesnt-work"),
    ("***FINALLY_WORKING***","finally-working","FinallyWorking","finallyworking", "finally-working"),
    ("","","","", "") // is this a valid name anyway!?
  )

  "Artifact Name" should {
    "comply with naming conventions" in {
      forAll(names) {(notebook: String, debian: String, clazz:String, pkg:String, lib:String) =>
        DebianName(notebook).name should be (debian)
        ClassName(notebook).name should be (clazz)
        PackageName(notebook).name should be (pkg)
        LibraryName(notebook).name should be (lib)
      }
    }
  }

}
