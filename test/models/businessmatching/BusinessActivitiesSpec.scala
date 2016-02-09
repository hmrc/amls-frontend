package models.businessmatching

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json._


class BusinessActivitiesSpec extends PlaySpec with MockitoSugar {

  "BusinessActivitiesSpec" must {
    import play.api.data.mapping.forms.Rules._

    "validate model with few check box selected" in {
      val model = Map(
        "services[]" -> Seq("03","01")
      )

      BusinessActivities.formReads.validate(model) must
        be(Success(BusinessActivities(Set(AccountancyServices, BillPaymentServices))))

    }

    "validate model with residential estate agency check box selected" in {
      val model = Map(
        "services" -> Seq("07")
      )

      BusinessActivities.formReads.validate(model) must
        be(Success(BusinessActivities(Set(TelephonePaymentService))))
    }

    "fail to validate on empty Map" in {

      BusinessActivities.formReads.validate(Map.empty) must
        be(Failure(Seq((Path \ "services") -> Seq(ValidationError("error.required")))))

    }

    "fail to validate when given invalid data" in {
      val model = Map(
        "services[]" -> Seq("02", "99", "03")
      )

      BusinessActivities.formReads.validate(model) must
        be(Failure(Seq((Path \ "services[1]" \ "services", Seq(ValidationError("error.invalid"))))))
    }

    "write correct data for services value" in {

      BusinessActivities.formWrites.writes(BusinessActivities(Set(EstateAgentBusinessService, BillPaymentServices, MoneyServiceBusiness))) must
        be(Map("services" -> Seq("03","02", "05")))
    }

    "write correct data for services value when residential option is selected" in {

      BusinessActivities.formWrites.writes(BusinessActivities(Set(AccountancyServices))) must
        be(Map("services" -> Seq("01")))
    }

    "JSON validation" must {

      "successfully validate given an enum value" in {
        val json =  Json.obj("services" -> Seq("05","06", "07"))

        Json.fromJson[BusinessActivities](json) must
          be(JsSuccess(BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService)), JsPath \ "services"))
      }

      "fail when on invalid data" in {
        Json.fromJson[BusinessActivities](Json.obj("service" -> "01")) must
          be(JsError((JsPath \ "services") -> ValidationError("error.path.missing")))
      }
    }

    "validate json write" in {
      Json.toJson(BusinessActivities(Set(HighValueDealing))) must
        be(Json.obj("services" -> Seq("04")))
    }

    "successfully validate json write" in {
      val json = Json.obj("services" -> Seq("02","07", "01"))
      Json.toJson(BusinessActivities(Set(BillPaymentServices, TelephonePaymentService, AccountancyServices))) must be(json)

    }
  }
}