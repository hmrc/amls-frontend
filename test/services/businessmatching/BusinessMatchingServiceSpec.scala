/*
 * Copyright 2024 HM Revenue & Customs
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

package services.businessmatching

import generators.businessmatching.BusinessMatchingGenerator
import generators.tradingpremises.TradingPremisesGenerator
import models.ViewResponse
import models.asp.Asp
import models.businessactivities.BusinessActivities
import models.businessdetails.BusinessDetails
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => BMActivities, _}
import models.declaration.AddPerson
import models.declaration.release7.RoleWithinBusinessRelease7
import models.eab.Eab
import models.hvd.Hvd
import models.moneyservicebusiness.{MoneyServiceBusiness => Msb}
import models.status.SubmissionDecisionApproved
import models.tcsp.Tcsp
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import utils.{AmlsSpec, DependencyMocks, FutureAssertions}

class BusinessMatchingServiceSpec
    extends PlaySpec
    with AmlsSpec
    with MockitoSugar
    with ScalaFutures
    with FutureAssertions
    with TradingPremisesGenerator
    with BusinessMatchingGenerator {

  trait Fixture extends DependencyMocks {
    val service = new BusinessMatchingService(mockCacheConnector)

    val businessMatchingModel = businessMatchingGen.sample.get

    mockCacheFetch(Some(businessMatchingModel), Some(BusinessMatching.key))
    mockCacheRemoveByKey[BusinessMatching]
  }

  "getModel" when {
    "called" must {
      "return the model" in new Fixture {
        mockCacheFetch(Some(businessMatchingModel), Some(BusinessMatching.key))

        service.getModel("internalId") returnsSome businessMatchingModel
      }
    }
  }

  "updateModel" when {
    "called" must {
      "update the model" in new Fixture {
        mockCacheSave(businessMatchingModel)

        service.updateModel("internalId", businessMatchingModel) returnsSome mockCacheMap
        verify(mockCacheConnector).save[BusinessMatching](any(), eqTo(BusinessMatching.key), any())(any())
      }
    }
  }

  "getAdditionalBusinessActivities" must {
    "return saved activities not found in view response" in new Fixture {
      val api5BusinessMatching = BusinessMatching(
        activities = Some(
          BMActivities(
            Set(BillPaymentServices)
          )
        )
      )

      val newBusinessMatching = BusinessMatching(
        activities = Some(
          BMActivities(
            Set(BillPaymentServices, HighValueDealing)
          )
        )
      )

      val viewResponse = ViewResponse(
        "",
        businessMatchingSection = api5BusinessMatching,
        businessDetailsSection = BusinessDetails(),
        bankDetailsSection = Seq.empty,
        businessActivitiesSection = BusinessActivities(),
        eabSection = None,
        aspSection = None,
        tcspSection = None,
        responsiblePeopleSection = None,
        tradingPremisesSection = None,
        msbSection = None,
        hvdSection = None,
        ampSection = None,
        supervisionSection = None,
        aboutYouSection = AddPerson("", None, "", RoleWithinBusinessRelease7(Set.empty))
      )

      mockCacheFetch(Some(newBusinessMatching), Some(BusinessMatching.key))
      mockCacheFetch[ViewResponse](Some(viewResponse), Some(ViewResponse.key))

      whenReady(service.getAdditionalBusinessActivities("internalId").value) { result =>
        result must be(Some(Set(HighValueDealing)))
      }
    }

    "return an empty set if saved business activities are the same as view response" in new Fixture {
      val businessMatching = BusinessMatching(
        activities = Some(
          BMActivities(
            Set(BillPaymentServices)
          )
        )
      )

      val viewResponse = ViewResponse(
        "",
        businessMatchingSection = BusinessMatching(
          activities = Some(
            BMActivities(
              Set(BillPaymentServices)
            )
          )
        ),
        businessDetailsSection = BusinessDetails(),
        bankDetailsSection = Seq.empty,
        businessActivitiesSection = BusinessActivities(),
        eabSection = None,
        aspSection = None,
        tcspSection = None,
        responsiblePeopleSection = None,
        tradingPremisesSection = None,
        msbSection = None,
        hvdSection = None,
        ampSection = None,
        supervisionSection = None,
        aboutYouSection = AddPerson("", None, "", RoleWithinBusinessRelease7(Set.empty))
      )

      mockCacheFetch(Some(businessMatching), Some(BusinessMatching.key))
      mockCacheFetch[ViewResponse](Some(viewResponse), Some(ViewResponse.key))

      whenReady(service.getAdditionalBusinessActivities("internalId").value) { result =>
        result must be(Some(Set.empty))
      }

    }

    "return none if all business activities cannot be retrieved" in new Fixture {

      val businessMatching = BusinessMatching(
        activities = Some(
          BMActivities(
            Set(BillPaymentServices)
          )
        )
      )

      mockApplicationStatus(SubmissionDecisionApproved)

      mockCacheFetch(Some(businessMatching), Some(BusinessMatching.key))
      mockCacheFetch(Some(businessMatching), Some(BusinessMatching.variationKey))
      mockCacheFetch[ViewResponse](None, Some(ViewResponse.key))

      whenReady(service.getAdditionalBusinessActivities("internalId").value) { result =>
        result must be(None)
      }

    }
  }

  "getRemainingBusinessActivities" must {
    "return the activities that the user has not yet added or previously submitted" in new Fixture {
      val businessMatching = BusinessMatching(
        activities = Some(
          BMActivities(
            Set(BillPaymentServices, HighValueDealing, AccountancyServices)
          )
        )
      )

      mockCacheFetch(Some(businessMatching), Some(BusinessMatching.key))

      whenReady(service.getRemainingBusinessActivities("internalId").value) { result =>
        result mustBe Some(
          Set(
            TelephonePaymentService,
            ArtMarketParticipant,
            EstateAgentBusinessService,
            TrustAndCompanyServices,
            MoneyServiceBusiness
          )
        )
      }
    }

    "return an empty set if all the available activities have been added" in new Fixture {
      val businessMatching = BusinessMatching(
        activities = Some(
          BMActivities(
            Set(
              BillPaymentServices,
              ArtMarketParticipant,
              HighValueDealing,
              AccountancyServices,
              TelephonePaymentService,
              EstateAgentBusinessService,
              TrustAndCompanyServices,
              MoneyServiceBusiness
            )
          )
        )
      )

      mockCacheFetch(Some(businessMatching), Some(BusinessMatching.key))

      whenReady(service.getRemainingBusinessActivities("internalId").value) { result =>
        result mustBe Some(Set.empty)
      }
    }
  }

  "getOriginalBusinessActivities" must {
    "return the activities that are present only in the view response" in new Fixture {
      val existing = BusinessMatching(
        activities = Some(
          BMActivities(
            Set(BillPaymentServices)
          )
        )
      )

      val current = BusinessMatching(
        activities = Some(
          BMActivities(
            Set(BillPaymentServices, HighValueDealing)
          )
        )
      )

      val viewResponse = ViewResponse(
        "",
        businessMatchingSection = BusinessMatching(
          activities = Some(
            BMActivities(
              Set(BillPaymentServices)
            )
          )
        ),
        businessDetailsSection = BusinessDetails(),
        bankDetailsSection = Seq.empty,
        businessActivitiesSection = BusinessActivities(),
        eabSection = None,
        aspSection = None,
        tcspSection = None,
        responsiblePeopleSection = None,
        tradingPremisesSection = None,
        msbSection = None,
        hvdSection = None,
        ampSection = None,
        supervisionSection = None,
        aboutYouSection = AddPerson("", None, "", RoleWithinBusinessRelease7(Set.empty))
      )

      mockApplicationStatus(SubmissionDecisionApproved)

      mockCacheFetch(Some(existing), Some(BusinessMatching.key))
      mockCacheFetch(Some(current), Some(BusinessMatching.variationKey))
      mockCacheFetch[ViewResponse](Some(viewResponse), Some(ViewResponse.key))

      whenReady(service.getSubmittedBusinessActivities("internalId").value) { result =>
        result must be(Some(Set(BillPaymentServices)))
      }
    }

    "return none if all business activities cannot be retrieved" in new Fixture {

      val businessMatching = BusinessMatching(
        activities = Some(
          BMActivities(
            Set(BillPaymentServices)
          )
        )
      )

      mockApplicationStatus(SubmissionDecisionApproved)

      mockCacheFetch(Some(businessMatching), Some(BusinessMatching.key))
      mockCacheFetch(Some(businessMatching), Some(BusinessMatching.variationKey))
      mockCacheFetch[ViewResponse](None, Some(ViewResponse.key))

      whenReady(service.getSubmittedBusinessActivities("internalId").value) { result =>
        result must be(None)
      }
    }
  }

  "clear section" must {
    "clear data of Asp given AccountancyServices" in new Fixture {
      val result = service.clearSection("internalId", AccountancyServices)

      await(result)

      verify(mockCacheConnector).removeByKey(
        eqTo("internalId"),
        eqTo(Asp.key)
      )

    }
    "clear data of Hvd given HighValueDealing" in new Fixture {

      val result = service.clearSection("internalId", HighValueDealing)

      await(result)

      verify(mockCacheConnector).removeByKey(
        eqTo("internalId"),
        eqTo(Hvd.key)
      )

    }
    "clear data of Msb given MoneyServiceBusiness" in new Fixture {

      val result = service.clearSection("internalId", MoneyServiceBusiness)

      await(result)

      verify(mockCacheConnector).removeByKey(
        eqTo("internalId"),
        eqTo(Msb.key)
      )

    }
    "clear data of Tcsp given TrustAndCompanyServices" in new Fixture {

      val result = service.clearSection("internalId", TrustAndCompanyServices)

      await(result)

      verify(mockCacheConnector).removeByKey(
        eqTo("internalId"),
        eqTo(Tcsp.key)
      )

    }
    "clear data of Eab given EstateAgentBusinessService" in new Fixture {

      val result = service.clearSection("internalId", EstateAgentBusinessService)

      await(result)

      verify(mockCacheConnector).removeByKey(
        eqTo("internalId"),
        eqTo(Eab.key)
      )

    }

  }

  "preApplicationComplete" when {
    "called" must {
      "return true" when {
        "in the right status" in new Fixture {
          mockCacheFetch[BusinessMatching](Some(BusinessMatching(preAppComplete = true)))

          val result = await(service.preApplicationComplete("internalId"))

          result mustBe true
        }
      }
    }
  }

}
