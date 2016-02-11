package models.businessmatching

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class BusinessMatchingSpec extends PlaySpec with MockitoSugar {

  "BusinessMatchingSpec" must {

    import play.api.libs.json._

    "JSON validation" must {

      "READ the JSON successfully and return the domain Object" in {

        val businessActivities = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
        val businessMatching = BusinessMatching(Some(businessActivities))

        val jsonBusinessActivities = Json.obj("businessActivities" -> Seq("05", "06", "07"))
        Json.fromJson[BusinessMatching](jsonBusinessActivities) must be(JsSuccess(businessMatching, JsPath))
      }

      "WRITE the JSON successfully from the domain Object" in {

        val businessActivities = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
        val businessMatching = BusinessMatching(Some(businessActivities))

        val jsonBusinessActivities = Json.obj("businessActivities" -> Seq("05", "06", "07"))
        Json.toJson(businessActivities) must be(jsonBusinessActivities)
      }

    }

  }
}
