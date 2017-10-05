package models.businessactivities

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import models.aboutthebusiness.EmployeeCountAMLSSupervision
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class EmployeeCountAMLSSupervisionSpec extends PlaySpec with MockitoSugar {
  "EmployeeCountAMLSSupervisionSpec" must {
    "successfully validate" when {
      "given a valid number" in {

        val data = Map(
          "employeeCountAMLSSupervision" -> Seq("12345678")
        )

        EmployeeCountAMLSSupervision.formRule.validate(data) must
          be(Valid(EmployeeCountAMLSSupervision("12345678")))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        EmployeeCountAMLSSupervision.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "employeeCountAMLSSupervision") -> Seq(ValidationError("error.required"))
          )))
      }

      "given missing data represented by an empty string" in {

        val data = Map(
          "employeeCountAMLSSupervision" -> Seq("")
        )

        EmployeeCountAMLSSupervision.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "employeeCountAMLSSupervision") -> Seq(ValidationError("error.required.ba.employee.count1"))
          )))
      }

      "given data which exceeds max length" in {

        val data = Map(
          "employeeCountAMLSSupervision" -> Seq("123456789123")
        )

        EmployeeCountAMLSSupervision.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "employeeCountAMLSSupervision") -> Seq(ValidationError("error.max.length.ba.employee.count"))
          )))
      }

      "given data which doesn't conform to regex" in {

        val data = Map(
          "employeeCountAMLSSupervision" -> Seq("1234XXX")
        )

        EmployeeCountAMLSSupervision.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "employeeCountAMLSSupervision") -> Seq(ValidationError("error.invalid.ba.employee.count"))
          )))
      }
    }

    "write correct data" in {

      val model = EmployeeCountAMLSSupervision("12")

      EmployeeCountAMLSSupervision.formWrites.writes(model) must
        be(Map(
          "employeeCountAMLSSupervision" -> Seq("12")
        ))
    }
  }

}
