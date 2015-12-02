package models

import org.scalatestplus.play.PlaySpec

class AboutTheBusinessSpec extends PlaySpec {

  "AboutTheBusiness " should {
    "successfully apply 1 to return the Model with correct values" in {
      val registeredOfficeApply = RegisteredOffice.applyString("1")
      registeredOfficeApply.isRegisteredOffice mustBe true
      registeredOfficeApply.isCorrespondenceAddressSame mustBe true
    }

    "successfully unapply the (true, false) to return the values as Strings" in {
      val registeredOfficeUnapply = RegisteredOffice.unapplyString(RegisteredOffice(isRegisteredOffice=true, isCorrespondenceAddressSame=true)) match {
        case Some(registeredOfficeExtracted) => registeredOfficeExtracted
        case _ => ""
      }
      registeredOfficeUnapply mustBe "1"
    }


    "successfully apply 2 to return the Model with correct values" in {
      val registeredOfficeApply = RegisteredOffice.applyString("2")
      registeredOfficeApply.isRegisteredOffice mustBe true
      registeredOfficeApply.isCorrespondenceAddressSame mustBe false
    }

    "successfully unapply the (true,true) to return the values as Strings" in {
      val registeredOfficeUnapply = RegisteredOffice.unapplyString(RegisteredOffice(isRegisteredOffice=true, isCorrespondenceAddressSame=false)) match {
        case Some(registeredOfficeExtracted) => registeredOfficeExtracted
        case _ => ""
      }
      registeredOfficeUnapply mustBe "2"
    }

    "successfully apply 3 to return the Model with correct values" in {
      val registeredOfficeApply = RegisteredOffice.applyString("3")
      registeredOfficeApply.isRegisteredOffice mustBe false
      registeredOfficeApply.isCorrespondenceAddressSame mustBe false
    }

    "successfully unapply the (false, false) to return the values as Strings" in {
      val registeredOfficeUnapply = RegisteredOffice.unapplyString(RegisteredOffice(isRegisteredOffice=false, isCorrespondenceAddressSame=false)) match {
        case Some(registeredOfficeExtracted) => registeredOfficeExtracted
        case _ => ""
      }
      registeredOfficeUnapply mustBe "3"
    }


    "throw an exception if invalid value in apply" in {
      a[java.lang.RuntimeException] should be thrownBy {
        RegisteredOffice.unapplyString(RegisteredOffice.applyString("4"))
      }
    }

    "throw an exception if invalid value in unapply" in {
      a[java.lang.RuntimeException] should be thrownBy {
        RegisteredOffice.unapplyString(RegisteredOffice(isRegisteredOffice=false, isCorrespondenceAddressSame=true))
      }
    }

  }
}
