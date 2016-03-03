package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class IdentifySuspiciousActivitySpec extends PlaySpec {

  "Form Validation" must {
    "Fail if neither option is picked" in {
      IdentifySuspiciousActivity.formRule.validate(Map()) must be (Failure(Seq(
        (Path \ "hasWrittenGuidance") -> Seq(ValidationError("error.required")))))
    }

    "Succeed if yes option is picked" in {
      IdentifySuspiciousActivity.formRule.validate(Map("hasWrittenGuidance" -> Seq("true"))) must be (Success(IdentifySuspiciousActivity(true)))
    }

    "Succeed if no option is picked" in {
      IdentifySuspiciousActivity.formRule.validate(Map("hasWrittenGuidance" -> Seq("false"))) must be (Success(IdentifySuspiciousActivity(false)))
    }

    "Fail if an invalid value is passed" in {
      IdentifySuspiciousActivity.formRule.validate(Map("hasWrittenGuidance" -> Seq("random"))) must be (Failure(Seq(
        (Path \ "hasWrittenGuidance") -> Seq(ValidationError("error.invalid", "Boolean")))))
    }
    }
  }
