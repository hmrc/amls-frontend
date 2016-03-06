package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.mapping.Success

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

    "JSON validation" must {

      "successfully read the JSON value" in {

      }


      "write the JSON value" in {

      }


    }

  }
}