package models.businessactivities

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import models.aboutthebusiness.EmployeeCount
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class EmployeeCountSpec extends PlaySpec with MockitoSugar {
  "EmployeeCountSpec" must {
    "successfully validate" when {
      "given a valid number" in {

        val data = Map(
          "employeeCount" -> Seq("12345678")
        )

        EmployeeCount.formRule.validate(data) must
          be(Valid(EmployeeCount("12345678")))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        EmployeeCount.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "employeeCount") -> Seq(ValidationError("error.required"))
          )))
      }

      "given missing data represented by an empty string" in {

        val data = Map(
          "employeeCount" -> Seq("")
        )

        EmployeeCount.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "employeeCount") -> Seq(ValidationError("error.required.ba.employee.count1"))
          )))
      }

      "given data which exceeds max length" in {

        val data = Map(
          "employeeCount" -> Seq("123456789123")
        )

        EmployeeCount.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "employeeCount") -> Seq(ValidationError("error.max.length.ba.employee.count"))
          )))
      }

      "given data which doesn't conform to regex" in {

        val data = Map(
          "employeeCount" -> Seq("1234XXX")
        )

        EmployeeCount.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "employeeCount") -> Seq(ValidationError("error.invalid.ba.employee.count"))
          )))
      }
    }

    "write correct data" in {

      val model = EmployeeCount("12")

      EmployeeCount.formWrites.writes(model) must
        be(Map(
          "employeeCount" -> Seq("12")
        ))
    }
  }

}
