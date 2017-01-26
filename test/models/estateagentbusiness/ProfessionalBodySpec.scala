package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Failure, Path, Success}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ProfessionalBodySpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {


    "successfully validate given an enum value" in {

      ProfessionalBody.formRule.validate(Map("penalised" -> Seq("false"))) must
        be(Success(ProfessionalBodyNo))
    }

    "successfully validate given an `Yes` value" in {

      val data = Map(
        "penalised" -> Seq("true"),
        "professionalBody" -> Seq("details")
      )

      ProfessionalBody.formRule.validate(data) must
        be(Success(ProfessionalBodyYes("details")))
    }

    "fail to validate missing mandatory value" in {

      ProfessionalBody.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "penalised") -> Seq(ValidationError("error.required.eab.penalised.by.professional.body"))
        )))
    }

    "fail to validate given an `Yes` with no value" in {

      val data = Map(
        "penalised" -> Seq("true"),
        "professionalBody" -> Seq("")
      )

      ProfessionalBody.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "professionalBody") -> Seq(ValidationError("error.required.eab.info.about.penalty"))
        )))
    }

    "fail to validate given an `Yes` with max value" in {

      val data = Map(
        "penalised" -> Seq("true"),
        "professionalBody" -> Seq("zzxczxczx"*50)
      )

      ProfessionalBody.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "professionalBody") -> Seq(ValidationError("error.invalid.eab.info.about.penalty"))
        )))
    }

    "write correct data from enum value" in {

      ProfessionalBody.formWrites.writes(ProfessionalBodyNo) must
        be(Map("penalised" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      ProfessionalBody.formWrites.writes(ProfessionalBodyYes("details")) must
        be(Map("penalised" -> Seq("true"), "professionalBody" -> Seq("details")))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[ProfessionalBody](Json.obj("penalised" -> false)) must
        be(JsSuccess(ProfessionalBodyNo, JsPath \ "penalised"))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("penalised" -> true, "professionalBody" ->"details")

      Json.fromJson[ProfessionalBody](json) must
        be(JsSuccess(ProfessionalBodyYes("details"), JsPath \ "penalised" \ "professionalBody"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("penalised" -> true)

      Json.fromJson[ProfessionalBody](json) must
        be(JsError((JsPath \ "penalised" \ "professionalBody") -> play.api.data.validation.ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(ProfessionalBodyNo) must
        be(Json.obj("penalised" -> false))

      Json.toJson(ProfessionalBodyYes("details")) must
        be(Json.obj(
          "penalised" -> true,
          "professionalBody" -> "details"
        ))
    }
  }


}
