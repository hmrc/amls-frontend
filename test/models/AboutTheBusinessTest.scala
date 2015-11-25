package models

import org.scalatestplus.play.PlaySpec

class AboutTheBusinessTest extends PlaySpec {

  private val registeredAddress = RegisteredOfficeAddress("Line 1 Mandatory", Option("NE98 1ZZ"))

  "AboutTheBusiness " should {
    "successfully apply String to return the Model with correct values" in {
      val registeredOfficeApply = RegisteredOffice.applyString(registeredAddress, "true,false")
      registeredOfficeApply.registeredOfficeAddress mustBe registeredAddress
      registeredOfficeApply.isRegisteredOffice mustBe true
      registeredOfficeApply.isCorrespondenceAddressSame mustBe false
    }

    "successfully unapply the Model to return the values as Strings" in {
      val registeredOfficeUnapply = RegisteredOffice.unapplyString(RegisteredOffice.applyString(registeredAddress, "true, false")) match {
        case Some(registeredOfficeExtracted) => registeredOfficeExtracted
        case _ => ""
      }
      registeredOfficeUnapply mustBe(registeredAddress, "true,false")
    }
  }
}