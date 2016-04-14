package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}


class PenalisedUnderEstateAgentsActSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {
      PenalisedUnderEstateAgentsAct.formRule.validate(Map("penalisedUnderEstateAgentsAct" -> Seq("false"))) must
        be(Success(PenalisedUnderEstateAgentsActNo))
    }

    "successfully validate given an `Yes` value" in {
      val data = Map(
        "penalisedUnderEstateAgentsAct" -> Seq("true"),
        "penalisedUnderEstateAgentsActDetails" -> Seq("Do not remember why penalised before")
      )

      PenalisedUnderEstateAgentsAct.formRule.validate(data) must
        be(Success(PenalisedUnderEstateAgentsActYes("Do not remember why penalised before")))
    }

    "fail to validate given mandatory field" in {

      PenalisedUnderEstateAgentsAct.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "penalisedUnderEstateAgentsAct") -> Seq(ValidationError("error.required.eab.penalised.under.act"))
        )))
    }


    "fail to validate given a `Yes` but no details provided" in {
      val data = Map(
        "penalisedUnderEstateAgentsAct" -> Seq("true"),
        "penalisedUnderEstateAgentsActDetails" -> Seq("")
      )

      PenalisedUnderEstateAgentsAct.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "penalisedUnderEstateAgentsActDetails") -> Seq(ValidationError("error.required.eab.info.about.penalty"))
        )))
    }

    "fail to validate given a `Yes` but max details provided" in {
      val data = Map(
        "penalisedUnderEstateAgentsAct" -> Seq("true"),
        "penalisedUnderEstateAgentsActDetails" -> Seq("zxzxcz"*50)
      )

      PenalisedUnderEstateAgentsAct.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "penalisedUnderEstateAgentsActDetails") -> Seq(ValidationError("error.invalid.eab.info.about.penalty"))
        )))
    }

    "write correct data from enum value" in {
      PenalisedUnderEstateAgentsAct.formWrites.writes(PenalisedUnderEstateAgentsActNo) must
        be(Map("penalisedUnderEstateAgentsAct" -> Seq("false")))
    }

    "write correct data from `Yes` value" in {
      PenalisedUnderEstateAgentsAct.formWrites.writes(PenalisedUnderEstateAgentsActYes("Do not remember why penalised before")) must
        be(Map("penalisedUnderEstateAgentsAct" -> Seq("true"), "penalisedUnderEstateAgentsActDetails" -> Seq("Do not remember why penalised before")))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {
      Json.fromJson[PenalisedUnderEstateAgentsAct](Json.obj("penalisedUnderEstateAgentsAct" -> false)) must
        be(JsSuccess(PenalisedUnderEstateAgentsActNo, JsPath \ "penalisedUnderEstateAgentsAct"))
    }

    "successfully validate given an `Yes` value" in {
      val json = Json.obj("penalisedUnderEstateAgentsAct" -> true, "penalisedUnderEstateAgentsActDetails" -> "Do not remember why penalised before")
      Json.fromJson[PenalisedUnderEstateAgentsAct](json) must
        be(JsSuccess(PenalisedUnderEstateAgentsActYes("Do not remember why penalised before"),
          JsPath \ "penalisedUnderEstateAgentsAct" \ "penalisedUnderEstateAgentsActDetails"))
    }

    "fail to validate when given an empty `Yes` value" in {
      val json = Json.obj("penalisedUnderEstateAgentsAct" -> true)
      Json.fromJson[PenalisedUnderEstateAgentsAct](json) must
        be(JsError((JsPath \ "penalisedUnderEstateAgentsAct" \ "penalisedUnderEstateAgentsActDetails") -> ValidationError("error.path.missing")))
    }

    "write the correct value" in {
      Json.toJson(PenalisedUnderEstateAgentsActNo) must be(Json.obj("penalisedUnderEstateAgentsAct" -> false))
      Json.toJson(PenalisedUnderEstateAgentsActYes("Do not remember why penalised before")) must
        be(Json.obj(
          "penalisedUnderEstateAgentsAct" -> true,
          "penalisedUnderEstateAgentsActDetails" -> "Do not remember why penalised before"
        ))
    }
  }

}
