package models.businessmatching

import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class BusinessMatchingSpec extends PlaySpec with MockitoSugar {

  "BusinessMatchingSpec" must {

    import play.api.libs.json._

    val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
    val businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB"))
    val ReviewDetailsModel = ReviewDetails("BusinessName", Some(BusinessType.SoleProprietor), businessAddress, "XE0001234567890")
    val TypeOfBusinessModel = TypeOfBusiness("test")
    val CompanyRegistrationNumberModel = CompanyRegistrationNumber("12345678")

    val jsonBusinessMatching = Json.obj(
      "activities" -> Json.obj(
          "businessActivities" -> Seq("05", "06", "07")
      ),
      "reviewDetails" -> Json.obj(
        "businessAddress" -> Json.obj(
          "line_1" -> "line1",
          "line_2" -> "line2",
          "line_3" -> "line3",
          "line_4" -> "line4",
          "postcode" -> "NE77 0QQ",
          "country" -> "GB"
        ),
        "businessName" -> "BusinessName",
        "businessType" -> "Sole Trader",
        "safeId" -> "XE0001234567890"
      ),
      "typeOfBusiness" -> Json.obj(
        "typeOfBusiness" -> "test"
      ),
      "companyRegistrationNumber" -> Json.obj(
        "companyRegistrationNumber" -> "12345678"
      ),
      "hasChanged" -> false
    )

    val businessMatching = BusinessMatching(
      Some(ReviewDetailsModel),
      Some(BusinessActivitiesModel),
      Some(TypeOfBusinessModel),
      Some(CompanyRegistrationNumberModel),
      hasChanged = false)

    "JSON validation" must {

      "READ the JSON successfully and return the domain Object" in {
        Json.fromJson[BusinessMatching](jsonBusinessMatching) must be(JsSuccess(businessMatching))
      }

      "WRITE the JSON successfully from the domain Object" in {
        Json.toJson(businessMatching) must be(jsonBusinessMatching)
      }
    }

    "None" when {

      val initial: Option[BusinessMatching] = None

      "Merged with BusinessActivities" must {
        "return BusinessMatching with correct BusinessActivities" in {
          val result = initial.activities(BusinessActivitiesModel)
          result must be(BusinessMatching(None, Some(BusinessActivitiesModel), None, hasChanged = true))
        }
      }

      "Merged with ReviewDetails" must {
        "return BusinessMatching with correct reviewDetails" in {
          val result = initial.reviewDetails(ReviewDetailsModel)
          result must be(BusinessMatching(Some(ReviewDetailsModel), None, None, hasChanged = true))
        }
      }

      "Merged with TypeOfBusiness" must {
        "return BusinessMatching with correct TypeOfBusiness" in {
          val result = initial.typeOfBusiness(TypeOfBusinessModel)
          result must be(BusinessMatching(None, None, Some(TypeOfBusinessModel), None, hasChanged = true))
        }
      }

      "Merged with CompanyRegistrationNumberModel" must {
        "return BusinessMatching with correct CompanyRegistrationNumberModel" in {
          val result = initial.companyRegistrationNumber(CompanyRegistrationNumberModel)
          result must be(BusinessMatching(None, None, None, Some(CompanyRegistrationNumberModel), hasChanged = true))
        }
      }
    }
  }
}
