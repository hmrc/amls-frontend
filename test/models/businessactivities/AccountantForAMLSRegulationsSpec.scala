package models.businessactivities

import models.businessactivities.AccountantForAMLSRegulations
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.Success
import play.api.libs.json.{JsPath, JsSuccess, Json}


class AccountantForAMLSRegulationsSpec extends PlaySpec {

  "Form Validation" must {

    "successfully validate given an true value" in {
      val data = Map("accountantForAMLSRegulations" -> Seq("true"))
      val result = AccountantForAMLSRegulations.formRule.validate(data)
      result mustBe Success(AccountantForAMLSRegulations(true))
    }

    "successfully validate given a false value" in {
      val data = Map("accountantForAMLSRegulations" -> Seq("false"))
      val result = AccountantForAMLSRegulations.formRule.validate(data)
      result mustBe Success(AccountantForAMLSRegulations(false))
    }

    "write correct data from true value" in {
      val result = AccountantForAMLSRegulations.formWrites.writes(AccountantForAMLSRegulations(true))
      result must be(Map("accountantForAMLSRegulations" -> Seq("true")))
    }

    "write correct data from false value" in {
      val result = AccountantForAMLSRegulations.formWrites.writes(AccountantForAMLSRegulations(false))
      result must be(Map("accountantForAMLSRegulations" -> Seq("false")))
    }

  }

  "JSON validation" must {

    "successfully validate given an `true` value" in {
      val json = Json.obj("accountantForAMLSRegulations" -> true)
      Json.fromJson[AccountantForAMLSRegulations](json) must
        be(JsSuccess(AccountantForAMLSRegulations(true), JsPath \ "accountantForAMLSRegulations"))
    }

    "successfully validate given an `false` value" in {
      val json = Json.obj("accountantForAMLSRegulations" -> false)
      Json.fromJson[AccountantForAMLSRegulations](json) must
        be(JsSuccess(AccountantForAMLSRegulations(false), JsPath \ "accountantForAMLSRegulations"))
    }

    "write the correct value given an NCARegisteredYes" in {
      Json.toJson(AccountantForAMLSRegulations(true)) must
        be(Json.obj("accountantForAMLSRegulations" -> true))
    }

    "write the correct value given an NCARegisteredNo" in {
      Json.toJson(AccountantForAMLSRegulations(false)) must
        be(Json.obj("accountantForAMLSRegulations" -> false))
    }
  }

}
