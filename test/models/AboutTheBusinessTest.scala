package models

import org.scalatestplus.play.PlaySpec

class AboutTheBusinessTest extends PlaySpec {

  "AboutTheBusiness " should {
    "successfully apply String to return the Model with correct values" in {
      val registeredOffice = RegisteredOffice.applyString("true,false")
      registeredOffice.isRegisteredOffice mustBe true
      registeredOffice.isCorrespondenceAddressSame mustBe false
    }

    "successfully unapply the Model to return the values as Strings" in {
      val aa = RegisteredOffice.unapplyString(RegisteredOffice.applyString("true, false")) match {
        case Some(s) => s
        case _ => ""
      }
      aa mustBe "true,false"
    }
  }
}