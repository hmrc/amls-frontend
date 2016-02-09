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

      val model = Map("businessActivities[]" -> Seq("03","01"))
      val ba = BusinessActivities(Set(EstateAgentBusinessService, AccountancyServices))

      BusinessActivities.formReads.validate(model) must be(Success(ba))

    }

    "validate model with residential estate agency check box selected" in {
      val model = Map(
        "businessActivities" -> Seq("07")
      )

      BusinessActivities.formReads.validate(model) must
        be(Success(BusinessActivities(Set(TelephonePaymentService))))
    }

    "fail to validate on empty Map" in {

      BusinessActivities.formReads.validate(Map.empty) must
        be(Failure(Seq((Path \ "businessActivities") -> Seq(ValidationError("error.required")))))
    }

    "fail to validate when given invalid data" in {
      val model = Map(
        "businessActivities[]" -> Seq("02", "99", "03")
      )
      val ba = BusinessActivities.formReads.validate(model)
      ba must
        be(Failure(Seq((Path \ "businessActivities[1]" \ "businessActivities", Seq(ValidationError("error.invalid"))))))
    }

    "write correct data for businessActivities value" in {

      BusinessActivities.formWrites.writes(BusinessActivities(Set(EstateAgentBusinessService, BillPaymentServices, MoneyServiceBusiness))) must
        be(Map("businessActivities" -> Seq("03","02", "05")))
    }

    "write correct data for businessActivities value when residential option is selected" in {

      BusinessActivities.formWrites.writes(BusinessActivities(Set(AccountancyServices))) must
        be(Map("businessActivities" -> Seq("01")))
    }

    "JSON validation" must {

      "successfully validate given an enum value" in {
        val json =  Json.obj("businessActivities" -> Seq("05","06", "07"))

        Json.fromJson[BusinessActivities](json) must
          be(JsSuccess(BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService)), JsPath \ "businessActivities"))
      }

      "fail when on invalid data" in {
        Json.fromJson[BusinessActivities](Json.obj("businessActivity" -> "01")) must
          be(JsError((JsPath \ "businessActivities") -> ValidationError("error.path.missing")))
      }
    }

    "validate json write" in {
      Json.toJson(BusinessActivities(Set(HighValueDealing))) must
        be(Json.obj("businessActivities" -> Seq("04")))
    }

    "successfully validate json write" in {
      val json = Json.obj("businessActivities" -> Seq("02","07", "01"))
      Json.toJson(BusinessActivities(Set(BillPaymentServices, TelephonePaymentService, AccountancyServices))) must be(json)

    }
  }
}