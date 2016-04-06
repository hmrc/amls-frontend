package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsObject, JsSuccess, Json}

class TimeAtAddressSpec extends PlaySpec with MockitoSugar {

  val ZeroToFiveForm = Map("timeAtAddress" -> "01")
  val SixToElevenForm = Map("timeAtAddress" -> "02")
  val OneToThreeForm = Map("timeAtAddress" -> "03")
  val MoreThanThreeForm = Map("timeAtAddress" -> "04")

  "Form Rules and Writes" must {

    "successfully validate objects given correct fields" in {
      TimeAtAddress.timeAtAddressFormRead.validate("01") must be(Success(ZeroToFiveMonths))
      TimeAtAddress.timeAtAddressFormRead.validate("02") must be(Success(SixToElevenMonths))
      TimeAtAddress.timeAtAddressFormRead.validate("03") must be(Success(OneToThreeYears))
      TimeAtAddress.timeAtAddressFormRead.validate("04") must be(Success(ThreeYearsPlus))
    }

    "throw error when field is missing" in {
      TimeAtAddress.timeAtAddressFormRead.validate("") must be(
        Failure(Seq(
          Path -> Seq(ValidationError("error.required.timeAtAddress")
        ))))
    }



    "throw error when there is invalid data" in {
      TimeAtAddress.timeAtAddressFormRead.validate("INVALID") must be(
        Failure(Seq(
          Path -> Seq(ValidationError("error.invalid", "Boolean")
        ))))
    }

  }

  "JSON" must {

    "Round trip a ZeroToFiveMonths correctly" in {
      TimeAtAddress.jsonReads.reads(
        TimeAtAddress.jsonWrites.writes(ZeroToFiveMonths)
      ) must be (JsSuccess(ZeroToFiveMonths))
    }

    "Serialise ZeroToFiveMonths as expected" in {
      Json.toJson(ZeroToFiveMonths) must be(Json.obj("timeAtAddress" -> "01"))
    }

    "Deserialise ZeroToFiveMonths as expected" in {
      Json.obj("timeAtAddress" -> "01").as[PreviousHomeAddress] must be(ZeroToFiveMonths)
    }

  }
}
