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
    val msbServices = MsbServices(
      Set(
        TransmittingMoney,
        CurrencyExchange,
        ChequeCashingNotScrapMetal,
        ChequeCashingScrapMetal
      )
    )
    val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
    val BusinessActivitiesWithouMSB = BusinessActivities(Set(TrustAndCompanyServices, TelephonePaymentService))
    val businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB"))
    val ReviewDetailsModel = ReviewDetails("BusinessName", Some(BusinessType.SoleProprietor), businessAddress, "XE0000000000000")
    val TypeOfBusinessModel = TypeOfBusiness("test")
    val CompanyRegistrationNumberModel = CompanyRegistrationNumber("12345678")
    val BusinessAppliedForPSRNumberModel = BusinessAppliedForPSRNumberYes("123456")

    val jsonBusinessMatching = Json.obj(
      "businessActivities" -> Seq("05", "06", "07"),
      "msbServices"-> Seq("01","02","03","04"),
      "businessName" -> "BusinessName",
      "businessType" -> "Sole Trader",
      "businessAddress" -> Json.obj(
        "line_1" -> "line1",
        "line_2" -> "line2",
        "line_3" -> "line3",
        "line_4" -> "line4",
        "postcode" -> "AA11 1AA",
        "country" -> "GB"
      ),
      "safeId" -> "XE0000000000000",
      "typeOfBusiness" -> "test",
      "companyRegistrationNumber" -> "12345678",
      "appliedFor" -> true,
      "regNumber" -> "123456"
      ,
      "hasChanged" -> false
    )


    val businessMatching = BusinessMatching(
      Some(ReviewDetailsModel),
      Some(BusinessActivitiesModel),
      Some(msbServices),
      Some(TypeOfBusinessModel),
      Some(CompanyRegistrationNumberModel),
      Some(BusinessAppliedForPSRNumberModel),
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
          result must be(BusinessMatching(None, None, None, Some(TypeOfBusinessModel), None, hasChanged = true))
        }
      }

      "Merged with CompanyRegistrationNumberModel" must {
        "return BusinessMatching with correct CompanyRegistrationNumberModel" in {
          val result = initial.companyRegistrationNumber(CompanyRegistrationNumberModel)
          result must be(BusinessMatching(None, None, None, None, Some(CompanyRegistrationNumberModel), hasChanged = true))
        }
      }

      "Merged with BusinessAppliedForPSRNumberModel" must {
        "return BusinessMatching with correct BusinessAppliedForPSRNumberModel" in {
          val result = initial.businessAppliedForPSRNumber(BusinessAppliedForPSRNumberModel)
          result must be(BusinessMatching(None, None, None, None, None,Some(BusinessAppliedForPSRNumberModel), hasChanged = true))
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
          val ReviewDetailsModel = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany), businessAddress, "XE0000000000000")
          businessMatching.copy(typeOfBusiness = None, reviewDetails = Some(ReviewDetailsModel)).isComplete mustBe true
        }
        "reviewDetails, activites and crn are set and BusinessType contains LPrLLP" in {
          val ReviewDetailsModel = ReviewDetails("BusinessName", Some(BusinessType.LPrLLP), businessAddress, "XE0000000000000")
          businessMatching.copy(typeOfBusiness = None, reviewDetails = Some(ReviewDetailsModel)).isComplete mustBe true
        }

        "business activity selected as MSB and msb services model is defined" in {

          val businessMatching = BusinessMatching(
            Some(ReviewDetailsModel),
            Some(BusinessActivitiesModel),
            Some(msbServices),
            Some(TypeOfBusinessModel),
            Some(CompanyRegistrationNumberModel),
            Some(BusinessAppliedForPSRNumberModel),
            hasChanged = false)

          businessMatching.isComplete must be (true)
        }

        "business activity selected as option other then MSB" in {

          val businessMatching = BusinessMatching(
            Some(ReviewDetailsModel),
            Some(BusinessActivitiesWithouMSB),
            None,
            Some(TypeOfBusinessModel),
            Some(CompanyRegistrationNumberModel),
            None,
            hasChanged = false)

          businessMatching.isComplete must be (true)
        }
      }

      "equal false" when {
        "no properties are set" in {
          BusinessMatching().isComplete mustBe false
        }
        "reviewDetails and activites are not set" in {
          businessMatching.copy(reviewDetails = None, activities = None).isComplete mustBe false
        }
        "reviewDetails is not set and activites is set" in {
          businessMatching.copy(reviewDetails = None).isComplete mustBe false
        }
        "reviewDetails is set and activites is not" in {
          businessMatching.copy(activities = None).isComplete mustBe false
        }
        "reviewDetails and activites are set, type is set and UnincorporatedBody is not set" in {
          val testModel = businessMatching.copy(
            reviewDetails = Some(ReviewDetails("BusinessName", Some(BusinessType.LPrLLP), businessAddress, "XE0000000000000")),
            companyRegistrationNumber = None
          )
          testModel.isComplete mustBe false
        }
        "reviewDetails and activites are set, crn is set, LimitedCompany and UnincorporatedBody are not set" in {
          val testModel = businessMatching.copy(
            reviewDetails = Some(ReviewDetails("BusinessName", Some(BusinessType.UnincorporatedBody), businessAddress, "XE0000000000000")),
            typeOfBusiness = None
          )
          testModel.isComplete mustBe false
        }

        "business activity selected as MSB and msb services model is not defined" in {

          val businessMatching = BusinessMatching(
            Some(ReviewDetailsModel),
            Some(BusinessActivitiesModel),
            None,
            Some(TypeOfBusinessModel),
            Some(CompanyRegistrationNumberModel),
            Some(BusinessAppliedForPSRNumberModel),
            hasChanged = false)

          businessMatching.isComplete must be (false)
        }

        "business activity selected as MSB and msb services model is defined and psr is not defined" in {

          val businessMatching = BusinessMatching(
            Some(ReviewDetailsModel),
            Some(BusinessActivitiesModel),
            Some(msbServices),
            Some(TypeOfBusinessModel),
            Some(CompanyRegistrationNumberModel),
            None,
            hasChanged = false)

          businessMatching.isComplete must be (false)
        }
      }
    }

    "section" must {

      "return `NotStarted` section when there is no section in Save4Later" in {
        implicit val cache = CacheMap("", Map.empty)
        BusinessMatching.section mustBe Section("businessmatching", NotStarted, false, controllers.businessmatching.routes.RegisterServicesController.get())
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
