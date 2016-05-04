package models.asp

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class ServicesOfBusinessSpec extends PlaySpec with MockitoSugar {

  "Form validation" must {

    val businessServices: Set[Service] = Set(Accountancy, PayrollServices, BookKeeping, Auditing, FinancialOrTaxAdvice)

    import play.api.data.mapping.forms.Rules._

    "validate model with few check box selected" in {

      val model = Map(
        "services[]" -> Seq("01", "02", "03", "04", "05")
      )

      ServicesOfBusiness.formReads.validate(model) must
        be(Success(ServicesOfBusiness(businessServices)))
    }

    "fail to validate on empty Map" in {

      ServicesOfBusiness.formReads.validate(Map.empty) must
        be(Failure(Seq((Path \ "services") -> Seq(ValidationError("error.required.eab.business.services")))))

    }

    "fail to validate when given invalid data" in {
      val model = Map(
        "services[]" -> Seq("02", "99", "03")
      )

      ServicesOfBusiness.formReads.validate(model) must
        be(Failure(Seq((Path \ "services" \ 1 \ "services", Seq(ValidationError("error.invalid"))))))
    }

    "write correct data for services value" in {

      ServicesOfBusiness.formWrites.writes(ServicesOfBusiness(Set(Accountancy, PayrollServices, BookKeeping))) must
        be(Map("services[]" -> Seq("01", "02", "03")))

      ServicesOfBusiness.formWrites.writes(ServicesOfBusiness(Set(Auditing, FinancialOrTaxAdvice))) must
        be(Map("services[]" -> Seq("04", "05")))

    }

    "JSON validation" must {

      "successfully validate given values" in {

        val json =  Json.obj("services" -> Seq("01","02","03","04","05"))

        Json.fromJson[ServicesOfBusiness](json) must
          be(JsSuccess(ServicesOfBusiness(businessServices), JsPath \ "services"))
      }

      "fail when on invalid data" in {

        Json.fromJson[ServicesOfBusiness](Json.obj("services" -> Seq("40"))) must
          be(JsError(((JsPath \ "services")(0) \ "services") -> ValidationError("error.invalid")))
      }

      "successfully validate json write" in {

        val json = Json.obj("services" -> Set("01","02","03","04","05"))
        Json.toJson(ServicesOfBusiness(businessServices)) must be(json)

      }

    }

  }

}
