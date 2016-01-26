package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}


class PenalisedUnderEstateAgentsActSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {
      PenalisedUnderEstateAgentsAct.formRule.validate(Map("penalisedunderestateagentsact" -> Seq("false"))) must
        be(Success(PenalisedUnderEstateAgentsActNo))
    }

    "successfully validate given an `Yes` value" in {
      val data = Map(
        "penalisedunderestateagentsact" -> Seq("true"),
        "penalisedunderestateagentsactdetails" -> Seq("penalisedunderestateagentsactdetails data")
      )

      PenalisedUnderEstateAgentsAct.formRule.validate(data) must
        be(Success(PenalisedUnderEstateAgentsActYes("penalisedunderestateagentsactdetails data")))
    }

    "fail to validate given a `Yes` but no details provided" in {
      val data = Map(
        "penalisedunderestateagentsact" -> Seq("true")
      )

      PenalisedUnderEstateAgentsAct.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "penalisedunderestateagentsactdetails") -> Seq(ValidationError("error.required"))
        )))
    }

    "write correct data from enum value" in {
      PenalisedUnderEstateAgentsAct.formWrites.writes(PenalisedUnderEstateAgentsActNo) must
        be(Map("penalisedunderestateagentsact" -> Seq("false")))
    }

    "write correct data from `Yes` value" in {
      PenalisedUnderEstateAgentsAct.formWrites.writes(PenalisedUnderEstateAgentsActYes("penalisedunderestateagentsactdetails data")) must
        be(Map("penalisedunderestateagentsact" -> Seq("true"), "penalisedunderestateagentsactdetails" -> Seq("penalisedunderestateagentsactdetails data")))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {
      Json.fromJson[PenalisedUnderEstateAgentsAct](Json.obj("penalisedunderestateagentsact" -> false)) must
        be(JsSuccess(PenalisedUnderEstateAgentsActNo, JsPath \ "penalisedunderestateagentsact"))
    }

    "successfully validate given an `Yes` value" in {
      val json = Json.obj("penalisedunderestateagentsact" -> true, "penalisedunderestateagentsactdetails" -> "penalisedunderestateagentsactdetails data")
      Json.fromJson[PenalisedUnderEstateAgentsAct](json) must
        be(JsSuccess(PenalisedUnderEstateAgentsActYes("penalisedunderestateagentsactdetails data"), JsPath \ "penalisedunderestateagentsact" \ "penalisedunderestateagentsactdetails"))
    }

    "fail to validate when given an empty `Yes` value" in {
      val json = Json.obj("penalisedunderestateagentsact" -> true)
      Json.fromJson[PenalisedUnderEstateAgentsAct](json) must
        be(JsError((JsPath \ "penalisedunderestateagentsact" \ "penalisedunderestateagentsactdetails") -> ValidationError("error.path.missing")))
    }

    "write the correct value" in {
      Json.toJson(PenalisedUnderEstateAgentsActNo) must be(Json.obj("penalisedunderestateagentsact" -> false))
      Json.toJson(PenalisedUnderEstateAgentsActYes("penalisedunderestateagentsactdetails data")) must
        be(Json.obj(
          "penalisedunderestateagentsact" -> true,
          "penalisedunderestateagentsactdetails" -> "penalisedunderestateagentsactdetails data"
        ))
    }
  }

}
