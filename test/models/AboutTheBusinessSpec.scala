package models

import org.scalatestplus.play.PlaySpec

import scala.util.Random

class AboutTheBusinessSpec extends PlaySpec {

  "AboutTheBusiness " should {

    "Successfully apply all combinations for boolean tuple to model and then unapply" in {

    Seq( (true,true), (true, false), (false,false), (false, true)).foreach( tuple => {
        val registeredOfficeApply = RegisteredOffice.fromBooleanTuple((tuple._1,tuple._2))
        registeredOfficeApply.isRegisteredOffice mustBe tuple._1
        registeredOfficeApply.isCorrespondenceAddressSame mustBe tuple._2

        RegisteredOffice.toBooleanTuple(RegisteredOffice(isRegisteredOffice = tuple._1, isCorrespondenceAddressSame = tuple._2)) mustBe Some((tuple._1, tuple._2))
      })
    }

    "successfully convert a RegisteredOfficeSave4Later object to a RegisteredOffice object" in {
      val registeredOfficeSave4Later = RegisteredOfficeSave4Later(BCAddress("", "", None, None, None, ""), Random.nextBoolean(), Random.nextBoolean())
      val result = RegisteredOffice.fromRegisteredOfficeSave4Later(registeredOfficeSave4Later)
      result mustBe RegisteredOffice(registeredOfficeSave4Later.isRegisteredOffice, registeredOfficeSave4Later.isCorrespondenceAddressSame)
    }

  }
}
