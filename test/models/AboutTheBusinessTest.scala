package models

import org.scalatestplus.play.PlaySpec

class AboutTheBusinessTest extends PlaySpec {
  private val registeredAddress = BCAddress("line_1", "line_2", Some(""), Some(""), Some("CA3 9ST"), "UK")

  "AboutTheBusiness " should {
    "successfully apply String to return the Model with correct values" in {
      val registeredOfficeApply = RegisteredOffice.applyString( "true,false")
      registeredOfficeApply.isRegisteredOffice mustBe true
      registeredOfficeApply.isCorrespondenceAddressSame mustBe false
    }

    "successfully unapply the Model to return the values as Strings" in {
      val registeredOfficeUnapply = RegisteredOffice.unapplyString(RegisteredOffice.applyString( "true, false")) match {
        case Some(registeredOfficeExtracted) => registeredOfficeExtracted
        case _ => ""
      }
      registeredOfficeUnapply mustBe "true,false"
    }
  }
}
