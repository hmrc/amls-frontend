package models.businessmatching

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}

class BusinessMatchingSpec extends PlaySpec with MockitoSugar {

  "BusinessMatchingSpec" must {

    import play.api.libs.json._

    "JSON validation" must {

      "successfully validate given an enum value" in {

        val businessActivities = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
        val businessMatching = BusinessMatching(Some(businessActivities))

        val jsonBusinessActivities = Json.obj("businessActivities" -> Seq("05", "06", "07"))

        Json.fromJson[BusinessMatching](jsonBusinessActivities) must be(JsSuccess(businessMatching, JsPath))
      }

    }

  }
}
