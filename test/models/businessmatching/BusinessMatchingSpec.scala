/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.businessmatching

import generators.businessmatching.BusinessMatchingGenerator
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.inject.guice.GuiceApplicationBuilder

class BusinessMatchingSpec extends PlaySpec with MockitoSugar with BusinessMatchingGenerator with OneAppPerSuite {

  override lazy val app = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.has-accepted" -> true)
    .build()

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

    val businessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
    val businessActivitiesWithouMSB = BusinessActivities(Set(TrustAndCompanyServices, TelephonePaymentService))
    val businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB"))
    val reviewDetailsModel = ReviewDetails("BusinessName", Some(BusinessType.SoleProprietor), businessAddress, "XE0000000000000")
    val typeOfBusinessModel = TypeOfBusiness("test")
    val companyRegistrationNumberModel = CompanyRegistrationNumber("12345678")
    val businessAppliedForPSRNumberModel = BusinessAppliedForPSRNumberYes("123456")

    val jsonBusinessMatching = Json.obj(
      "businessActivities" -> Seq("05", "06", "07"),
      "msbServices" -> Seq("01", "02", "03", "04"),
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
      "hasChanged" -> false,
      "hasAccepted" -> true,
      "preAppComplete" -> true
    )

    val businessMatching = BusinessMatching(
      Some(reviewDetailsModel),
      Some(businessActivitiesModel),
      Some(msbServices),
      Some(typeOfBusinessModel),
      Some(companyRegistrationNumberModel),
      Some(businessAppliedForPSRNumberModel),
      hasAccepted = true,
      preAppComplete = true)

    "READ the JSON successfully and return the domain Object" in {
      Json.fromJson[BusinessMatching](jsonBusinessMatching - "hasChanged") must be(JsSuccess(businessMatching))
    }

    "WRITE the JSON successfully from the domain Object" in {
      Json.toJson(businessMatching) must be(jsonBusinessMatching)
    }

    val initial: Option[BusinessMatching] = None

    "Merged with BusinessActivities" must {
      "return BusinessMatching with correct BusinessActivities" in {
        val result = initial.activities(businessActivitiesModel)
        result must be(BusinessMatching(None, Some(businessActivitiesModel), None, hasChanged = true))
      }
    }

    "Merged with ReviewDetails" must {
      "return BusinessMatching with correct reviewDetails" in {
        val result = initial.reviewDetails(reviewDetailsModel)
        result must be(BusinessMatching(Some(reviewDetailsModel), None, None, hasChanged = true))
      }
    }

    "Merged with TypeOfBusiness" must {
      "return BusinessMatching with correct TypeOfBusiness" in {
        val result = initial.typeOfBusiness(typeOfBusinessModel)
        result must be(BusinessMatching(None, None, None, Some(typeOfBusinessModel), None, hasChanged = true))
      }
    }

    "Merged with CompanyRegistrationNumberModel" must {
      "return BusinessMatching with correct CompanyRegistrationNumberModel" in {
        val result = initial.companyRegistrationNumber(companyRegistrationNumberModel)
        result must be(BusinessMatching(None, None, None, None, Some(companyRegistrationNumberModel), hasChanged = true))
      }
    }

    "Merged with BusinessAppliedForPSRNumberModel" must {
      "return BusinessMatching with correct BusinessAppliedForPSRNumberModel" in {
        val result = initial.businessAppliedForPSRNumber(businessAppliedForPSRNumberModel)
        result must be(BusinessMatching(None, None, None, None, None, Some(businessAppliedForPSRNumberModel), hasChanged = true))
      }
    }

    "isComplete" must {

      "equal true" when {
        "the business type is not UnincorporatedBody or LPrLLP/LimitedCompany" in {

          val businessMatching = BusinessMatching(
            Some(reviewDetailsModel),
            Some(businessActivitiesModel),
            Some(msbServices),
            None,
            None,
            Some(businessAppliedForPSRNumberModel),
            hasChanged = false,
            hasAccepted = true,
            preAppComplete = true
          )

          businessMatching.isComplete mustBe true
        }

        "reviewDetails, activites and typeOfBusiness are set and BusinessType contains UnincorporatedBody" in {

          val businessMatching = BusinessMatching(
            Some(reviewDetailsModel),
            Some(businessActivitiesModel),
            Some(msbServices),
            Some(typeOfBusinessModel),
            None,
            Some(businessAppliedForPSRNumberModel),
            hasChanged = false,
            hasAccepted = true,
            preAppComplete = true
          )

          businessMatching.isComplete mustBe true
        }

        "reviewDetails, activites and crn are set and BusinessType contains LimitedCompany" in {
          val reviewDetailsModel = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany), businessAddress, "XE0000000000000")

          val businessMatching = BusinessMatching(
            Some(reviewDetailsModel),
            Some(businessActivitiesModel),
            Some(msbServices),
            None,
            Some(companyRegistrationNumberModel),
            Some(businessAppliedForPSRNumberModel),
            hasChanged = false,
            hasAccepted = true,
            preAppComplete = true
          )

          businessMatching.isComplete mustBe true
        }

        "reviewDetails, activites and crn are set and BusinessType contains LPrLLP" in {
          val reviewDetailsModel = ReviewDetails("BusinessName", Some(BusinessType.LPrLLP), businessAddress, "XE0000000000000")

          val businessMatching = BusinessMatching(
            Some(reviewDetailsModel),
            Some(businessActivitiesModel),
            Some(msbServices),
            None,
            Some(companyRegistrationNumberModel),
            Some(businessAppliedForPSRNumberModel),
            hasChanged = false,
            hasAccepted = true,
            preAppComplete = true
          )

          businessMatching.isComplete mustBe true
        }

        "business activity selected as MSB and msb services model is defined" in {

          val businessMatching = BusinessMatching(
            Some(reviewDetailsModel),
            Some(businessActivitiesModel),
            Some(msbServices),
            Some(typeOfBusinessModel),
            Some(companyRegistrationNumberModel),
            Some(businessAppliedForPSRNumberModel),
            hasChanged = false,
            hasAccepted = true,
            preAppComplete = true
          )

          businessMatching.isComplete must be(true)
        }

        "business activity selected as option other then MSB" in {

          val businessMatching = BusinessMatching(
            Some(reviewDetailsModel),
            Some(businessActivitiesWithouMSB),
            None,
            Some(typeOfBusinessModel),
            Some(companyRegistrationNumberModel),
            None,
            hasChanged = false,
            hasAccepted = true,
            preAppComplete = true
          )

          businessMatching.isComplete must be(true)
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

        "hasAccepted is not set" in {
          val model = BusinessMatching(
            Some(reviewDetailsModel),
            Some(businessActivitiesModel),
            Some(msbServices),
            None,
            None,
            Some(businessAppliedForPSRNumberModel),
            hasChanged = false,
            hasAccepted = false
          )

          model.isComplete mustBe false
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
            Some(reviewDetailsModel),
            Some(businessActivitiesModel),
            None,
            Some(typeOfBusinessModel),
            Some(companyRegistrationNumberModel),
            Some(businessAppliedForPSRNumberModel),
            hasChanged = false)

          businessMatching.isComplete must be(false)
        }

        "business activity selected as MSB and msb services model is defined and psr is not defined" in {

          val businessMatching = BusinessMatching(
            Some(reviewDetailsModel),
            Some(businessActivitiesModel),
            Some(msbServices),
            Some(typeOfBusinessModel),
            Some(companyRegistrationNumberModel),
            None,
            hasChanged = false)

          businessMatching.isComplete must be(false)
        }
      }
    }

    "hasAccepted" must {
      "reset to false" when {

        val tests = Seq[(BusinessMatching => BusinessMatching, String)](
          (_.activities(BusinessActivities(Set(MoneyServiceBusiness))), "activities"),
          (_.msbServices(MsbServices(Set(CurrencyExchange))), "msbServices"),
          (_.reviewDetails(reviewDetailsGen.sample.get), "reviewDetails"),
          (_.typeOfBusiness(TypeOfBusiness("type of business")), "typeOfBusiness"),
          (_.companyRegistrationNumber(CompanyRegistrationNumber("987654321")), "companyRegistrationNumber"),
          (_.businessAppliedForPSRNumber(BusinessAppliedForPSRNumberNo), "businessAppliedForPSRNumber")
        )

        tests.foreach { test =>
          s"${test._2} was changed" in {
            val model = test._1(businessMatching.copy(hasAccepted = true))
            model.hasAccepted mustBe false
          }
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
