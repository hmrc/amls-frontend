package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class CorporationTaxRegisteredSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an false value" in {
      CorporationTaxRegistered.formRule.validate(Map("registeredForCorporationTax" -> Seq("false"))) must
        be(Success(CorporationTaxRegisteredNo))
    }

    "successfully validate given an `true` value" in {

      val data = Map(
        "registeredForCorporationTax" -> Seq("true"),
        "corporationTaxReference" -> Seq("1234567890")
      )

      CorporationTaxRegistered.formRule.validate(data) must
        be(Success(CorporationTaxRegisteredYes("1234567890")))
    }

    "fail to validate given an `Yes` with no value" in {

      val data = Map(
        "registeredForCorporationTax" -> Seq("true")
      )

      CorporationTaxRegistered.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "corporationTaxReference") -> Seq(ValidationError("error.required"))
        )))
    }

    "write correct data from enum value" in {

      CorporationTaxRegistered.formWrites.writes(CorporationTaxRegisteredNo) must
        be(Map("registeredForCorporationTax" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      CorporationTaxRegistered.formWrites.writes(CorporationTaxRegisteredYes("1234567890")) must
        be(Map("registeredForCorporationTax" -> Seq("true"), "corporationTaxReference" -> Seq("1234567890")))
    }
  }

  "JSON validation" must {

    "successfully validate given an false value" in {
      Json.fromJson[CorporationTaxRegistered](Json.obj("registeredForCorporationTax" -> false)) must
        be(JsSuccess(CorporationTaxRegisteredNo, JsPath \ "registeredForCorporationTax"))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("registeredForCorporationTax" -> true, "corporationTaxReference" ->"1234567890")

      Json.fromJson[CorporationTaxRegistered](json) must
        be(JsSuccess(CorporationTaxRegisteredYes("1234567890"), JsPath \ "registeredForCorporationTax" \ "corporationTaxReference"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("registeredForCorporationTax" -> true)

      Json.fromJson[CorporationTaxRegistered](json) must
        be(JsError((JsPath \ "registeredForCorporationTax" \ "corporationTaxReference") -> ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(CorporationTaxRegisteredNo) must
        be(Json.obj("registeredForCorporationTax" -> false))

      Json.toJson(CorporationTaxRegisteredYes("1234567890")) must
        be(Json.obj(
          "registeredForCorporationTax" -> true,
          "corporationTaxReference" -> "1234567890"
        ))
    }
  }


}
