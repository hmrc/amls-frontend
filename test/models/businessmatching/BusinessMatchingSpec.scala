package models.businessmatching

import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._

class BusinessMatchingSpec extends PlaySpec with MockitoSugar {

  "BusinessMatchingSpec" must {

    import play.api.libs.json._

    val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
    val businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB"))
    val ReviewDetailsModel = ReviewDetails("BusinessName", Some(BusinessType.UnincorporatedBody), businessAddress, "XE0001234567890")
    val TypeOfBusinessModel = TypeOfBusiness("test")
    val CompanyRegistrationNumberModel = CompanyRegistrationNumber("12345678")

    val jsonBusinessMatching = Json.obj(
      "businessActivities" -> Seq("05", "06", "07"),
      "businessName" -> "BusinessName",
      "businessType" -> "Unincorporated Body",
      "businessAddress" -> Json.obj(
        "line_1" -> "line1",
        "line_2" -> "line2",
        "line_3" -> "line3",
        "line_4" -> "line4",
        "postcode" -> "NE77 0QQ",
        "country" -> "GB"
      ),
      "safeId" -> "XE0001234567890",
      "typeOfBusiness" -> "test",
      "companyRegistrationNumber" -> "12345678",
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
        Json.fromJson[BusinessMatching](jsonBusinessMatching - "hasChanged") must be(JsSuccess(businessMatching))
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

    "isComplete" must {

      "equal true" when {
        "reviewDetails and activites are set" in {
          businessMatching.copy(typeOfBusiness = None, companyRegistrationNumber = None).isComplete mustBe true
        }
        "reviewDetails, activites and typeOfBusiness are set and BusinessType contains UnincorporatedBody" in {
          businessMatching.copy(companyRegistrationNumber = None).isComplete mustBe true
        }
        "reviewDetails, activites and crn are set and BusinessType contains LimitedCompany" in {
          val ReviewDetailsModel = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany), businessAddress, "XE0001234567890")
          businessMatching.copy(typeOfBusiness = None, reviewDetails = Some(ReviewDetailsModel)).isComplete mustBe true
        }
        "reviewDetails, activites and crn are set and BusinessType contains LPrLLP" in {
          val ReviewDetailsModel = ReviewDetails("BusinessName", Some(BusinessType.LPrLLP), businessAddress, "XE0001234567890")
          businessMatching.copy(typeOfBusiness = None, reviewDetails = Some(ReviewDetailsModel)).isComplete mustBe true
        }
      }

      "equal false" when {
        "no properties are set" in {
          BusinessMatching().isComplete mustBe false
        }
        "reviewDetails and activites are not set" in {
          businessMatching.copy(reviewDetails = None,activities = None).isComplete mustBe false
        }
        "reviewDetails is not set and activites is set" in {
          businessMatching.copy(reviewDetails = None).isComplete mustBe false
        }
        "reviewDetails is set and activites is not" in {
          businessMatching.copy(activities = None).isComplete mustBe false
        }
        "reviewDetails and activites are set, type is set and UnincorporatedBody is not set" in {
          val testModel = businessMatching.copy(
            reviewDetails = Some(ReviewDetails("BusinessName", Some(BusinessType.LPrLLP), businessAddress, "XE0001234567890")),
            companyRegistrationNumber = None
          )
          testModel.isComplete mustBe false
        }
        "reviewDetails and activites are set, crn is set, LimitedCompany and UnincorporatedBody are not set" in {
          val testModel = businessMatching.copy(
            reviewDetails = Some(ReviewDetails("BusinessName", Some(BusinessType.UnincorporatedBody), businessAddress, "XE0001234567890")),
            typeOfBusiness = None
          )
          testModel.isComplete mustBe false
        }
      }

    }

    "section" must {

      "return `NotStarted` section when there is no section in Save4Later" in {
        implicit val cache = CacheMap("", Map.empty)
        BusinessMatching.section mustBe Section("businessmatching", NotStarted, false,  controllers.businessmatching.routes.RegisterServicesController.get())
      }

      "return `Started` section when there is a section which isn't completed" in {
        implicit val cache = mock[CacheMap]
        implicit val ac = mock[AuthContext]
        when {
          cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any())
        } thenReturn Some(BusinessMatching())
        BusinessMatching.section mustBe Section("businessmatching", Started, false, controllers.businessmatching.routes.RegisterServicesController.get())
      }

      "return `Completed` section when there is a section which is completed" in {
        implicit val cache = mock[CacheMap]
        implicit val ac = mock[AuthContext]
        when {
          cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any())
        } thenReturn Some(businessMatching)
        BusinessMatching.section mustBe Section("businessmatching", Completed, false, controllers.businessmatching.routes.SummaryController.get())
      }
    }
  }
}
