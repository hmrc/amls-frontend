package models.tradingpremises

import org.scalatestplus.play.PlaySpec
import jto.validation.{Path, Invalid, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class BusinessStructureSpec extends PlaySpec {

  "BusinessStructure" should {

    "Read Form data successfully" in {

      BusinessStructure.agentsBusinessStructureRule.validate(Map("agentsBusinessStructure" -> Seq("01"))) must be(Valid(SoleProprietor))
      BusinessStructure.agentsBusinessStructureRule.validate(Map("agentsBusinessStructure" -> Seq("02"))) must be(Valid(LimitedLiabilityPartnership))
      BusinessStructure.agentsBusinessStructureRule.validate(Map("agentsBusinessStructure" -> Seq("03"))) must be(Valid(Partnership))
      BusinessStructure.agentsBusinessStructureRule.validate(Map("agentsBusinessStructure" -> Seq("04"))) must be(Valid(IncorporatedBody))
      BusinessStructure.agentsBusinessStructureRule.validate(Map("agentsBusinessStructure" -> Seq("05"))) must be(Valid(UnincorporatedBody))
    }

    "Write Form data successfully" in {

      BusinessStructure.formWritesBusinessStructure.writes(SoleProprietor) must be(Map("agentsBusinessStructure" -> Seq("01")))
      BusinessStructure.formWritesBusinessStructure.writes(LimitedLiabilityPartnership) must be(Map("agentsBusinessStructure" -> Seq("02")))
      BusinessStructure.formWritesBusinessStructure.writes(Partnership) must be(Map("agentsBusinessStructure" -> Seq("03")))
      BusinessStructure.formWritesBusinessStructure.writes(IncorporatedBody) must be(Map("agentsBusinessStructure" -> Seq("04")))
      BusinessStructure.formWritesBusinessStructure.writes(UnincorporatedBody) must be(Map("agentsBusinessStructure" -> Seq("05")))
    }

    "Fail on invalid data" in {
      BusinessStructure.agentsBusinessStructureRule.validate(Map("agentsBusinessStructure" -> Seq("11"))) must be(Invalid(List(
        (Path \ "agentsBusinessStructure", List(ValidationError("error.invalid"))))))
    }
    "Fail on missing mandatory field" in {
      BusinessStructure.agentsBusinessStructureRule.validate(Map.empty) must be(Invalid(List(
        (Path \ "agentsBusinessStructure", List(ValidationError("error.required.tp.select.business.structure"))))))
    }

    "Read JSON data successfully" in {
      Json.fromJson[BusinessStructure](Json.obj("agentsBusinessStructure" -> "01")) must be(JsSuccess(SoleProprietor,
        JsPath \ "agentsBusinessStructure"))

      Json.fromJson[BusinessStructure](Json.obj("agentsBusinessStructure" -> "02")) must be(JsSuccess(LimitedLiabilityPartnership,
        JsPath \ "agentsBusinessStructure"))

      Json.fromJson[BusinessStructure](Json.obj("agentsBusinessStructure" -> "03")) must be(JsSuccess(Partnership,
        JsPath \ "agentsBusinessStructure"))

      Json.fromJson[BusinessStructure](Json.obj("agentsBusinessStructure" -> "04")) must be(JsSuccess(IncorporatedBody,
        JsPath \ "agentsBusinessStructure"))

      Json.fromJson[BusinessStructure](Json.obj("agentsBusinessStructure" -> "05")) must be(JsSuccess(UnincorporatedBody,
        JsPath \ "agentsBusinessStructure"))
    }

    "Write JSON data successfully" in {

      Json.toJson(SoleProprietor) must be(Json.obj("agentsBusinessStructure" -> "01"))
      Json.toJson(LimitedLiabilityPartnership) must be(Json.obj("agentsBusinessStructure" -> "02"))
      Json.toJson(Partnership) must be(Json.obj("agentsBusinessStructure" -> "03"))
      Json.toJson(IncorporatedBody) must be(Json.obj("agentsBusinessStructure" -> "04"))
      Json.toJson(UnincorporatedBody) must be(Json.obj("agentsBusinessStructure" -> "05"))
    }

    "throw error for invalid data" in {
      Json.fromJson[BusinessStructure](Json.obj("agentsBusinessStructure" -> "20")) must
        be(JsError(JsPath \ "agentsBusinessStructure", play.api.data.validation.ValidationError("error.invalid")))
    }
  }

}
