/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.businessmatching.updateservice

import generators.businessmatching.BusinessActivitiesGenerator
import models.businessactivities._
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import models.supervision._
import org.joda.time.LocalDate
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify}
import org.scalatest.MustMatchers
import play.api.test.Helpers._
import services.TradingPremisesService
import utils.{AuthorisedFixture, DependencyMocks, FutureAssertions, GenericTestHelper}

class UpdateServiceHelperSpec extends GenericTestHelper with MustMatchers with BusinessActivitiesGenerator with FutureAssertions {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val tradingPremisesService = mock[TradingPremisesService]
    val mockUpdateServiceHelper = mock[UpdateServiceHelper]

    val helper = new UpdateServiceHelper(
      self.authConnector,
      mockCacheConnector,
      tradingPremisesService
    )

    val businessActivitiesSection = BusinessActivities(
      involvedInOther = Some(InvolvedInOtherNo),
      whoIsYourAccountant = Some(mock[WhoIsYourAccountant]),
      accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true)),
      taxMatters = Some(TaxMatters(true)),
      hasAccepted = true
    )
  }

  "updateBusinessActivities" must {
    "remove the accountancy data from the 'business activities' section" in new Fixture {
      mockCacheUpdate[BusinessActivities](Some(BusinessActivities.key), businessActivitiesSection)

      val result = await(helper.updateBusinessActivities(AccountancyServices))

      result.involvedInOther mustBe Some(InvolvedInOtherNo)
      result.whoIsYourAccountant must not be defined
      result.accountantForAMLSRegulations must not be defined
      result.taxMatters must not be defined
      result.hasAccepted mustBe true
    }

    "not touch the accountancy data if the activity is not 'accountancy services'" in new Fixture {
      mockCacheUpdate[BusinessActivities](Some(BusinessActivities.key), businessActivitiesSection)

      val result = await(helper.updateBusinessActivities(HighValueDealing))

      result.whoIsYourAccountant mustBe defined
      result.accountantForAMLSRegulations mustBe Some(AccountantForAMLSRegulations(true))
      result.taxMatters mustBe Some(TaxMatters(true))
      result.hasAccepted mustBe true
    }
  }

  "updateSupervision" must {
    "return a blank supervision element" when {
      "the business doesn't have ASP or TSCP" in new Fixture {
        mockCacheFetch[Supervision](
          Some(Supervision(Some(AnotherBodyNo), Some(ProfessionalBodyMemberNo), None, Some(ProfessionalBodyNo))),
          Some(Supervision.key))

        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BusinessMatchingActivities(Set(HighValueDealing))))),
          Some(BusinessMatching.key))

        mockCacheSave(Supervision(), Some(Supervision.key))

        helper.updateSupervision.returnsSome(Supervision())

        verify(mockCacheConnector).save(eqTo(Supervision.key), eqTo(Supervision()))(any(), any(), any())
      }
    }

    "leave the supervision section alone" when {
      "the business has ASP" in new Fixture {
        val supervisionModel = Supervision(Some(AnotherBodyYes("Some supervisor", LocalDate.now, LocalDate.now, "no reason")),
          Some(ProfessionalBodyMemberNo),
          None,
          Some(ProfessionalBodyNo))

        mockCacheFetch[Supervision](Some(supervisionModel), Some(Supervision.key))

        mockCacheFetch[BusinessMatching](
          Some(BusinessMatching(activities = Some(BusinessMatchingActivities(Set(AccountancyServices))))),
          Some(BusinessMatching.key))

        helper.updateSupervision.returnsSome(supervisionModel)

        verify(mockCacheConnector, never).save(any(), any())(any(), any(), any())
      }
    }

    "the business has TCSP" in new Fixture {
      val supervisionModel = Supervision(Some(AnotherBodyYes("Some supervisor", LocalDate.now, LocalDate.now, "no reason")),
        Some(ProfessionalBodyMemberNo),
        None,
        Some(ProfessionalBodyNo))

      mockCacheFetch[Supervision](Some(supervisionModel), Some(Supervision.key))

      mockCacheFetch[BusinessMatching](
        Some(BusinessMatching(activities = Some(BusinessMatchingActivities(Set(TrustAndCompanyServices))))),
        Some(BusinessMatching.key))

      helper.updateSupervision.returnsSome(supervisionModel)

      verify(mockCacheConnector, never).save(any(), any())(any(), any(), any())
    }
  }
}
