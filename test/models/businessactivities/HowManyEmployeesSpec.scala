package models.businessactivities

import jto.validation.{Failure, Path, Success, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

import scala.language.postfixOps

class HowManyEmployeesSpec extends PlaySpec {

  "Form Validation" must {

    "read and validate the input url encoded fields" in {

      HowManyEmployees.formRule.validate(
        Map("employeeCount" -> Seq("123456789"),
          "employeeCountAMLSSupervision" -> Seq("12345678"))) must
        be(Success(HowManyEmployees("123456789", "12345678")))
    }

    "write the model fields to url encoded response" in {

      HowManyEmployees.formWrites.writes(HowManyEmployees("123456789", "12345678")) must
        be(Map("employeeCount" -> Seq("123456789"),
          "employeeCountAMLSSupervision" -> Seq("12345678")))

    }

    "fail when mandatory fields not filled" in {

      val data = Map("employeeCount" -> Seq(""),
        "employeeCountAMLSSupervision" -> Seq(""))

      HowManyEmployees.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "employeeCount") -> Seq(ValidationError("error.required.ba.employee.count1")),
          (Path \ "employeeCountAMLSSupervision") -> Seq(ValidationError("error.required.ba.employee.count2"))
        )))
    }

    "fail to validate given invalid data" in {

      val data = Map("employeeCount" -> Seq("124545"),
        "employeeCountAMLSSupervision" -> Seq("ghjgj"))

      HowManyEmployees.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "employeeCountAMLSSupervision") -> Seq(ValidationError("error.invalid.ba.employee.count"))
        )))
    }

    "fail to validate given max value" in {

      val data = Map("employeeCount" -> Seq("12454"),
        "employeeCountAMLSSupervision" -> Seq("111111111111"))

      HowManyEmployees.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "employeeCountAMLSSupervision") -> Seq(ValidationError("error.max.length.ba.employee.count"))
        )))
    }
  }

  "JSON read" must {
  import play.api.data.validation.ValidationError
    "fail to validate when given employeeCountAMLSSupervision is missing" in {
      val json = Json.obj("employeeCount" -> "12345678901")
      Json.fromJson[HowManyEmployees](json) must
        be(JsError((JsPath \ "employeeCountAMLSSupervision") -> ValidationError("error.path.missing")))
    }

    "fail to validate when given employeeCount is missing" in {
      val json = Json.obj("employeeCountAMLSSupervision" -> "12345678901")
      Json.fromJson[HowManyEmployees](json) must
        be(JsError((JsPath \ "employeeCount") -> ValidationError("error.path.missing")))
    }

    "successfully read the JSON value to create the Model" in {
      val json = Json.obj("employeeCount" -> "12345678901", "employeeCountAMLSSupervision" -> "123456789")
      Json.fromJson[HowManyEmployees](json) must
        be(JsSuccess(HowManyEmployees("12345678901", "123456789"), JsPath))
    }

  }

  "JSON write the correct value" must {

    "be populated in the JSON from the Model" in {
      val howManyEmployees = HowManyEmployees("12345678901", "123456789")
      Json.toJson(howManyEmployees) must
        be(Json.obj("employeeCount" -> "12345678901", "employeeCountAMLSSupervision" -> "123456789"))
    }
  }
}