package models.businessmatching

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class CompanyRegistrationNumberSpec extends PlaySpec with MockitoSugar {

  "CompanyRegistrationNumber" must {

    "Form Validation" must {

      "successfully validate given a correct numeric value" in {
        val data = Map("companyRegistrationNumber" -> Seq("12345678"))
        val result = CompanyRegistrationNumber.formReads.validate(data)
        result mustBe Success(CompanyRegistrationNumber("12345678"))
      }

      "successfully validate given a correct lower case alphanumeric value" in {
        val data = Map("companyRegistrationNumber" -> Seq("AB765BHD"))
        val result = CompanyRegistrationNumber.formReads.validate(data)
        result mustBe Success(CompanyRegistrationNumber("AB765BHD"))
      }

      "successfully validate given a correct upper case alphanumeric value" in {
        val data = Map("companyRegistrationNumber" -> Seq("AB78JC12"))
        val result = CompanyRegistrationNumber.formReads.validate(data)
        result mustBe Success(CompanyRegistrationNumber("AB78JC12"))
      }

      "validate with a failure on missing mandatory field" in {
        val result = CompanyRegistrationNumber.formReads.validate(Map("companyRegistrationNumber" -> Seq("")))
        result mustBe Failure(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.required"))))
      }

      "validate with a failure given a value with length greater than 8" in {
        val data = Map("companyRegistrationNumber" -> Seq("1234567890"))
        val result = CompanyRegistrationNumber.formReads.validate(data)
        result mustBe Failure(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.pattern",
          CompanyRegistrationNumber.registrationNumberRegex))))
      }

      "validate with a failure given a value with length less than 8" in {
        val data = Map("companyRegistrationNumber" -> Seq("1290"))
        val result = CompanyRegistrationNumber.formReads.validate(data)
        result mustBe Failure(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.pattern",
          CompanyRegistrationNumber.registrationNumberRegex))))
      }

      "validate with a failure given a value containing non-alphanumeric characters" in {
        val data = Map("companyRegistrationNumber" -> Seq("1234567!"))
        val result = CompanyRegistrationNumber.formReads.validate(data)
        result mustBe Failure(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.pattern",
          CompanyRegistrationNumber.registrationNumberRegex))))
      }

      "write correct data from correct value" in {
        val result = CompanyRegistrationNumber.formWrites.writes(CompanyRegistrationNumber("12345678"))
        result must be(Map("companyRegistrationNumber" -> Seq("12345678")))
      }

    }

    "Json validation" must {

      "READ the JSON successfully and return the domain Object" in {
        val companyRegistrationNumber = CompanyRegistrationNumber("12345678")
        val jsonCompanyRegistrationNumber = Json.obj("companyRegistrationNumber" -> "12345678")
        val fromJson = Json.fromJson[CompanyRegistrationNumber](jsonCompanyRegistrationNumber)
        fromJson must be(JsSuccess(companyRegistrationNumber, JsPath \ "companyRegistrationNumber"))
      }

      "validate model with valid numeric registration number" in {
        val model = Map("companyRegistrationNumber" -> Seq("12345678"))
        CompanyRegistrationNumber.formReads.validate(model) must
          be(Success(CompanyRegistrationNumber("12345678")))
      }

      "validate model with valid upper case registration number" in {
        val model = Map("companyRegistrationNumber" -> Seq("ABCDEFGH"))
        CompanyRegistrationNumber.formReads.validate(model) must
          be(Success(CompanyRegistrationNumber("ABCDEFGH")))
      }

      "validate model with valid lower case registration number" in {
        val model = Map("companyRegistrationNumber" -> Seq("ABCDEFGH"))
        CompanyRegistrationNumber.formReads.validate(model) must
          be(Success(CompanyRegistrationNumber("ABCDEFGH")))
      }

      "fail to validate when given data with length greater than 8" in {
        val model = Map("companyRegistrationNumber" -> Seq("1234567890"))
        CompanyRegistrationNumber.formReads.validate(model) mustBe Failure(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.pattern",
          CompanyRegistrationNumber.registrationNumberRegex))))
      }

      "fail to validate when given data with length less than 8" in {
        val model = Map("companyRegistrationNumber" -> Seq("123"))
        CompanyRegistrationNumber.formReads.validate(model) mustBe Failure(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.pattern",
          CompanyRegistrationNumber.registrationNumberRegex))))
      }

      "fail to validate when given data with non-alphanumeric characters" in {
        val model = Map("companyRegistrationNumber" -> Seq("1234567!"))
        CompanyRegistrationNumber.formReads.validate(model) mustBe Failure(Seq((Path \ "companyRegistrationNumber") -> Seq(ValidationError("error.pattern",
          CompanyRegistrationNumber.registrationNumberRegex))))
      }
    }
  }
}