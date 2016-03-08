package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.mapping.Success
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

import scala.language.postfixOps

class HowManyEmployeesSpec extends PlaySpec with MockitoSugar with OneServerPerSuite {

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
  }

  "JSON read" must {

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