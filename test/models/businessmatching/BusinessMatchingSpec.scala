package models.businessmatching

import models.businesscustomer.{Address, ReviewDetails}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class BusinessMatchingSpec extends PlaySpec with MockitoSugar {

  "BusinessMatchingSpec" must {

    import play.api.libs.json._

    val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
    val businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), "GB")
    val ReviewDetailsModel = ReviewDetails("BusinessName", Some("SOP"), businessAddress, "ghghg", "XE0001234567890")
    val TypeOfBusinessModel = TypeOfBusiness("test")

    "JSON validation" must {

      "READ the JSON successfully and return the domain Object" in {

        val businessMatching = BusinessMatching(Some(BusinessActivitiesModel), Some(ReviewDetailsModel))

        val jsonBusinessMatching = Json.obj("businessActivities" -> Seq("05", "06", "07"),
                                              "businessName" ->"BusinessName",
                                              "businessType" -> "SOP",
                                              "businessAddress" -> Json.obj("line_1" ->"line1",
                                                "line_2" ->"line2",
                                                "line_3" ->"line3",
                                                "line_4" ->"line4",
                                                "postcode" ->"NE77 0QQ",
                                                "country" ->"GB"),
                                              "sapNumber" ->"ghghg",
                                              "safeId" ->"XE0001234567890")

        Json.fromJson[BusinessMatching](jsonBusinessMatching) must be(JsSuccess(businessMatching))
      }

      "WRITE the JSON successfully from the domain Object" in {

        val businessMatching = BusinessMatching(Some(BusinessActivitiesModel), Some(ReviewDetailsModel))

        val jsonBusinessMatching = Json.obj("businessActivities" -> Seq("05", "06", "07"),
          "businessName" ->"BusinessName",
          "businessType" -> "SOP",
          "businessAddress" -> Json.obj("line_1" ->"line1","line_2" ->"line2","line_3" ->"line3","line_4" ->"line4","postcode" ->"NE77 0QQ","country" ->"GB"),
          "sapNumber" ->"ghghg",
          "safeId" ->"XE0001234567890")

        Json.toJson(businessMatching) must be(jsonBusinessMatching)
      }

    }

    "None" when {
      val initial: Option[BusinessMatching] = None

      "Merged with BusinessActivities" must {
        "return BusinessMatching with correct BusinessActivities" in {
          val result = initial.activities(BusinessActivitiesModel)
          result must be (BusinessMatching(Some(BusinessActivitiesModel), None))
        }
      }

      "Merged with ReviewDetails" must {
        "return BusinessMatching with correct reviewDetails" in {
          val result = initial.reviewDetails(ReviewDetailsModel)
          result must be (BusinessMatching(None, Some(ReviewDetailsModel), None))
        }
      }

      "Merged with TypeOfBusiness" must {
        "return BusinessMatching with correct TypeOfBusiness" in {
          val result = initial.typeOfBusiness(TypeOfBusinessModel)
          result must be (BusinessMatching(None, None, Some(TypeOfBusinessModel)))
        }
      }
    }
  }
}
