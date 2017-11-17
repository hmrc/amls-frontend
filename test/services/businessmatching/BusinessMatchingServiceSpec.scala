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

package services.businessmatching

import cats.implicits._
import generators.businessmatching.BusinessMatchingGenerator
import generators.tradingpremises.TradingPremisesGenerator
import models.{DateOfChange, ViewResponse}
import models.aboutthebusiness.AboutTheBusiness
import models.businessactivities.BusinessActivities
import models.businessmatching.{BusinessActivities => BMActivities, _}
import models.declaration.AddPerson
import models.declaration.release7.RoleWithinBusinessRelease7
import models.status.{NotCompleted, SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import org.joda.time.LocalDate
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import utils.{DependencyMocks, FutureAssertions, GenericTestHelper, StatusConstants}

import scala.concurrent.ExecutionContext.Implicits.global

class BusinessMatchingServiceSpec extends PlaySpec
with GenericTestHelper
  with MockitoSugar
  with ScalaFutures
  with FutureAssertions
  with TradingPremisesGenerator
  with BusinessMatchingGenerator {

  trait Fixture extends DependencyMocks {
    val service = new BusinessMatchingService(mockStatusService, mockCacheConnector)

    val primaryModel = businessMatchingGen.sample.get
    val variationModel = businessMatchingGen.sample.get

    mockCacheFetch(Some(primaryModel), Some(BusinessMatching.key))
    mockCacheFetch(Some(variationModel), Some(BusinessMatching.variationKey))
    mockCacheSave[BusinessMatching]

  }

  "getModel" when {
    "called" must {
      "return the primary model" when {
        "in a pre-application status" in new Fixture {
          mockApplicationStatus(NotCompleted)
          service.getModel returnsSome primaryModel
        }

        "the variation model is empty and status is post-preapp" in new Fixture {
          mockApplicationStatus(SubmissionDecisionApproved)
          mockCacheFetch(Some(BusinessMatching()), Some(BusinessMatching.variationKey))

          service.getModel returnsSome primaryModel
        }
      }

      "return the variation model" when {
        "in a amendment or variation status" in new Fixture {
          mockApplicationStatus(SubmissionDecisionApproved)
          service.getModel returnsSome variationModel
        }
      }

      "not query for the original model when the variation model exists" in new Fixture {
        mockApplicationStatus(SubmissionReadyForReview)
        service.getModel returnsSome variationModel
        verify(mockCacheConnector, never).fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any())
      }
    }
  }

  "updateModel" when {
    "called" must {
      "update the original model" when {
        "in pre-application status" in new Fixture {
          mockApplicationStatus(NotCompleted)
          mockCacheSave(primaryModel)

          service.updateModel(primaryModel) returnsSome mockCacheMap
          verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any())
        }
      }

      "update the variation model" when {
        "not in pre-application status" in new Fixture {
          mockApplicationStatus(SubmissionReadyForReview)
          mockCacheSave(primaryModel)

          whenReady(service.updateModel(primaryModel).value) { _ =>
            verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.variationKey), any())(any(), any(), any())
          }
        }
      }
    }
  }

  "getAdditionalBusinessActivities" must {
    "return saved activities not found in view response" in new Fixture {

      val existing = BusinessMatching(
        activities = Some(BMActivities(
          Set(BillPaymentServices)
        ))
      )
      val current = BusinessMatching(
        activities = Some(BMActivities(
          Set(BillPaymentServices, HighValueDealing)
        ))
      )

      val viewResponse = ViewResponse(
        "",
        businessMatchingSection = BusinessMatching(
          activities = Some(BMActivities(
            Set(BillPaymentServices)
          ))
        ),
        aboutTheBusinessSection = AboutTheBusiness(),
        bankDetailsSection = Seq.empty,
        businessActivitiesSection = BusinessActivities(),
        eabSection = None,
        aspSection = None,
        tcspSection = None,
        responsiblePeopleSection = None,
        tradingPremisesSection = None,
        msbSection = None,
        hvdSection = None,
        supervisionSection = None,
        aboutYouSection = AddPerson("", None, "", RoleWithinBusinessRelease7(Set.empty))
      )

      mockApplicationStatus(SubmissionDecisionApproved)

      mockCacheFetch(Some(existing), Some(BusinessMatching.key))
      mockCacheFetch(Some(current), Some(BusinessMatching.variationKey))
      mockCacheFetch[ViewResponse](Some(viewResponse), Some(ViewResponse.key))


      whenReady(service.getAdditionalBusinessActivities.value){ result =>
        result must be(Some(Set(HighValueDealing)))
      }
    }

    "return an empty set if saved business activities are the same as view response" in new Fixture {

      val businessMatching = BusinessMatching(
        activities = Some(BMActivities(
          Set(BillPaymentServices)
        ))
      )

      val viewResponse = ViewResponse(
        "",
        businessMatchingSection = BusinessMatching(
          activities = Some(BMActivities(
            Set(BillPaymentServices)
          ))
        ),
        aboutTheBusinessSection = AboutTheBusiness(),
        bankDetailsSection = Seq.empty,
        businessActivitiesSection = BusinessActivities(),
        eabSection = None,
        aspSection = None,
        tcspSection = None,
        responsiblePeopleSection = None,
        tradingPremisesSection = None,
        msbSection = None,
        hvdSection = None,
        supervisionSection = None,
        aboutYouSection = AddPerson("", None, "", RoleWithinBusinessRelease7(Set.empty))
      )

      mockApplicationStatus(SubmissionDecisionApproved)

      mockCacheFetch(Some(businessMatching), Some(BusinessMatching.key))
      mockCacheFetch(Some(businessMatching), Some(BusinessMatching.variationKey))
      mockCacheFetch[ViewResponse](Some(viewResponse), Some(ViewResponse.key))

      whenReady(service.getAdditionalBusinessActivities.value){ result =>
        result must be(Some(Set.empty))
      }

    }
    "return none if all business activities cannot be retrieved" in new Fixture {

      val businessMatching = BusinessMatching(
        activities = Some(BMActivities(
          Set(BillPaymentServices)
        ))
      )

      mockApplicationStatus(SubmissionDecisionApproved)

      mockCacheFetch(Some(businessMatching), Some(BusinessMatching.key))
      mockCacheFetch(Some(businessMatching), Some(BusinessMatching.variationKey))
      mockCacheFetch[ViewResponse](None, Some(ViewResponse.key))

      whenReady(service.getAdditionalBusinessActivities.value){ result =>
        result must be(None)
      }

    }
  }

  "getOriginalBusinessActivities" must {
    "return the activities that are present only in the view response" in new Fixture {
      val existing = BusinessMatching(
        activities = Some(BMActivities(
          Set(BillPaymentServices)
        ))
      )

      val current = BusinessMatching(
        activities = Some(BMActivities(
          Set(BillPaymentServices, HighValueDealing)
        ))
      )

      val viewResponse = ViewResponse(
        "",
        businessMatchingSection = BusinessMatching(
          activities = Some(BMActivities(
            Set(BillPaymentServices)
          ))
        ),
        aboutTheBusinessSection = AboutTheBusiness(),
        bankDetailsSection = Seq.empty,
        businessActivitiesSection = BusinessActivities(),
        eabSection = None,
        aspSection = None,
        tcspSection = None,
        responsiblePeopleSection = None,
        tradingPremisesSection = None,
        msbSection = None,
        hvdSection = None,
        supervisionSection = None,
        aboutYouSection = AddPerson("", None, "", RoleWithinBusinessRelease7(Set.empty))
      )

      mockApplicationStatus(SubmissionDecisionApproved)

      mockCacheFetch(Some(existing), Some(BusinessMatching.key))
      mockCacheFetch(Some(current), Some(BusinessMatching.variationKey))
      mockCacheFetch[ViewResponse](Some(viewResponse), Some(ViewResponse.key))

      whenReady(service.getSubmittedBusinessActivities.value){ result =>
        result must be(Some(Set(BillPaymentServices)))
      }
    }

    "return none if all business activities cannot be retrieved" in new Fixture {

      val businessMatching = BusinessMatching(
        activities = Some(BMActivities(
          Set(BillPaymentServices)
        ))
      )

      mockApplicationStatus(SubmissionDecisionApproved)

      mockCacheFetch(Some(businessMatching), Some(BusinessMatching.key))
      mockCacheFetch(Some(businessMatching), Some(BusinessMatching.variationKey))
      mockCacheFetch[ViewResponse](None, Some(ViewResponse.key))

      whenReady(service.getSubmittedBusinessActivities.value){ result =>
        result must be(None)
      }
    }
  }

  "commitVariationData" when {
    "called" must {
      "simply return the cachemap when in pre-application status" in new Fixture {
        mockApplicationStatus(SubmissionReady)
        service.commitVariationData returnsSome mockCacheMap
      }

      "copy the variation data over the primary data when not in pre-application status" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheGetEntry(primaryModel.some, BusinessMatching.key)
        mockCacheGetEntry(variationModel.some, BusinessMatching.variationKey)

        whenReady(service.commitVariationData.value) { _ =>
          verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.key), eqTo(variationModel.copy(hasChanged = true)))(any(), any(), any())
          verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.variationKey), eqTo(BusinessMatching()))(any(), any(), any())
        }
      }

      "copy the variation data over the primary data, setting hasChanged to false when the models are the same" in new Fixture {
        val newModel = businessMatchingGen.sample

        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheGetEntry(newModel, BusinessMatching.key)
        mockCacheGetEntry(newModel, BusinessMatching.variationKey)

        whenReady(service.commitVariationData.value) { _ =>
          verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.key), eqTo(newModel.copy(hasChanged = false)))(any(), any(), any())
          verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.variationKey), eqTo(BusinessMatching()))(any(), any(), any())
        }
      }

      "return None if the variation data is not available" in new Fixture {
        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheGetEntry(primaryModel.some, BusinessMatching.key)
        mockCacheGetEntry(None, BusinessMatching.variationKey)

        whenReady(service.commitVariationData.value) { result =>
          verify(mockCacheConnector, never).save[BusinessMatching](any(), any())(any(), any(), any())
          result mustBe None
        }
      }

      "return None if the variation data is empty" in new Fixture {
        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheGetEntry(primaryModel.some, BusinessMatching.key)
        mockCacheGetEntry(BusinessMatching().some, BusinessMatching.variationKey)

        whenReady(service.commitVariationData.value) { result =>
          verify(mockCacheConnector, never).save[BusinessMatching](any(), any())(any(), any(), any())
          result mustBe None
        }
      }

    }
  }

  "clear" when {
    "called" must {
      "reset the variation model back to nothing" in new Fixture {
        whenReady(service.clearVariation.value) { _ =>
          verify(mockCacheConnector).save[BusinessMatching](eqTo(BusinessMatching.variationKey), eqTo(BusinessMatching()))(any(), any(), any())
        }
      }
    }
  }

  "fitAndProperRequired" must {
    "return true" when {
      "existing activities does not contain msb and tcsp" when {
        "current activities contains msb" in new Fixture {

          val existing = BusinessMatching(
            activities = Some(BMActivities(
              Set(BillPaymentServices)
            ))
          )
          val current = BusinessMatching(
            activities = Some(BMActivities(
              Set(MoneyServiceBusiness)
            ))
          )

          val viewResponse = ViewResponse(
            "",
            businessMatchingSection = existing,
            aboutTheBusinessSection = AboutTheBusiness(),
            bankDetailsSection = Seq.empty,
            businessActivitiesSection = BusinessActivities(),
            eabSection = None,
            aspSection = None,
            tcspSection = None,
            responsiblePeopleSection = None,
            tradingPremisesSection = None,
            msbSection = None,
            hvdSection = None,
            supervisionSection = None,
            aboutYouSection = AddPerson("", None, "", RoleWithinBusinessRelease7(Set.empty))
          )

          mockApplicationStatus(SubmissionDecisionApproved)

          mockCacheFetch(Some(current), Some(BusinessMatching.variationKey))
          mockCacheFetch[ViewResponse](Some(viewResponse), Some(ViewResponse.key))

          whenReady(service.fitAndProperRequired.value) { result =>
            result must be(Some(true))
          }
        }

        "current activities contains tcsp" in new Fixture {

          val existing = BusinessMatching(
            activities = Some(BMActivities(
              Set(BillPaymentServices)
            ))
          )
          val current = BusinessMatching(
            activities = Some(BMActivities(
              Set(TrustAndCompanyServices)
            ))
          )

          val viewResponse = ViewResponse(
            "",
            businessMatchingSection = existing,
            aboutTheBusinessSection = AboutTheBusiness(),
            bankDetailsSection = Seq.empty,
            businessActivitiesSection = BusinessActivities(),
            eabSection = None,
            aspSection = None,
            tcspSection = None,
            responsiblePeopleSection = None,
            tradingPremisesSection = None,
            msbSection = None,
            hvdSection = None,
            supervisionSection = None,
            aboutYouSection = AddPerson("", None, "", RoleWithinBusinessRelease7(Set.empty))
          )

          mockApplicationStatus(SubmissionDecisionApproved)

          mockCacheFetch(Some(current), Some(BusinessMatching.variationKey))
          mockCacheFetch[ViewResponse](Some(viewResponse), Some(ViewResponse.key))

          whenReady(service.fitAndProperRequired.value) { result =>
            result must be(Some(true))
          }
        }
      }
    }
    "return false" when {
      "existing activities contains msb" in new Fixture {

        val existing = BusinessMatching(
          activities = Some(BMActivities(
            Set(MoneyServiceBusiness)
          ))
        )
        val current = BusinessMatching(
          activities = Some(BMActivities(
            Set(TrustAndCompanyServices)
          ))
        )

        val viewResponse = ViewResponse(
          "",
          businessMatchingSection = existing,
          aboutTheBusinessSection = AboutTheBusiness(),
          bankDetailsSection = Seq.empty,
          businessActivitiesSection = BusinessActivities(),
          eabSection = None,
          aspSection = None,
          tcspSection = None,
          responsiblePeopleSection = None,
          tradingPremisesSection = None,
          msbSection = None,
          hvdSection = None,
          supervisionSection = None,
          aboutYouSection = AddPerson("", None, "", RoleWithinBusinessRelease7(Set.empty))
        )

        mockApplicationStatus(SubmissionDecisionApproved)

        mockCacheFetch(Some(current), Some(BusinessMatching.variationKey))
        mockCacheFetch[ViewResponse](Some(viewResponse), Some(ViewResponse.key))

        whenReady(service.fitAndProperRequired.value) { result =>
          result must be(Some(false))
        }
      }
      "existing activities contains tcsp" in new Fixture {

        val existing = BusinessMatching(
          activities = Some(BMActivities(
            Set(TrustAndCompanyServices)
          ))
        )
        val current = BusinessMatching(
          activities = Some(BMActivities(
            Set(BillPaymentServices)
          ))
        )

        val viewResponse = ViewResponse(
          "",
          businessMatchingSection = existing,
          aboutTheBusinessSection = AboutTheBusiness(),
          bankDetailsSection = Seq.empty,
          businessActivitiesSection = BusinessActivities(),
          eabSection = None,
          aspSection = None,
          tcspSection = None,
          responsiblePeopleSection = None,
          tradingPremisesSection = None,
          msbSection = None,
          hvdSection = None,
          supervisionSection = None,
          aboutYouSection = AddPerson("", None, "", RoleWithinBusinessRelease7(Set.empty))
        )

        mockApplicationStatus(SubmissionDecisionApproved)

        mockCacheFetch(Some(current), Some(BusinessMatching.variationKey))
        mockCacheFetch[ViewResponse](Some(viewResponse), Some(ViewResponse.key))

        whenReady(service.fitAndProperRequired.value) { result =>
          result must be(Some(false))
        }
      }
    }
  }

  "activitiesToIterate" must {
    "return true" when {
      "index is less than the amount of activities" in new Fixture {
        val result = service.activitiesToIterate(0, Set(AccountancyServices, HighValueDealing))

        result must be(true)
      }
    }
    "return false" when {
      "index is greater than the amount of activities" in new Fixture {
        val result = service.activitiesToIterate(3, Set(AccountancyServices, HighValueDealing))

        result must be(false)
      }
      "index is equal to the amount of activities" in new Fixture {
        val result = service.activitiesToIterate(2, Set(AccountancyServices, HighValueDealing))

        result must be(false)
      }
    }
  }

  "patchTradingPremises" must {
    "update activity of the trading premises identified by index in request data" when {
      "there is a single index" which {
        "will leave activity given remove equals false" in new Fixture {

          val models = Seq(
            tradingPremisesGen.sample.get.copy(
              whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(BillPaymentServices), Some(DateOfChange(new LocalDate(2001,10,31)))))
            ),
            tradingPremisesWithActivitiesGen(BillPaymentServices).sample.get.copy(status = Some(StatusConstants.Deleted)),
            tradingPremisesWithActivitiesGen(BillPaymentServices).sample.get,
            tradingPremisesWithActivitiesGen(BillPaymentServices).sample.get,
            tradingPremisesWithActivitiesGen(BillPaymentServices).sample.get
          )

          val result = service.patchTradingPremises(Seq(4), models, AccountancyServices, false)

          result.head mustBe models.head
          result(1) mustBe models(1)
          result(2) mustBe models(2)
          result(3) mustBe models(3)
          result.lift(4).get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices), None))

        }
      }
      "there are multiple indices" which {
        "will remove activity if existing in trading premises given remove equals true" in new Fixture {

          val models = Seq(
            tradingPremisesWithActivitiesGen(AccountancyServices, HighValueDealing).sample.get,
            tradingPremisesWithActivitiesGen(AccountancyServices, HighValueDealing).sample.get,
            tradingPremisesWithActivitiesGen(MoneyServiceBusiness).sample.get
          )

          val result = service.patchTradingPremises(Seq(0,2), models, AccountancyServices, true)

          result.headOption.get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(AccountancyServices, HighValueDealing), None))
          result.lift(1).get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(HighValueDealing), None))
          result.lift(2).get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(AccountancyServices, MoneyServiceBusiness), None))

          result.head.isComplete mustBe true
          result.head.hasChanged mustBe true

        }
      }
    }
    "mark the trading premises as incomplete if there are no activities left" in new Fixture {

      val models = Seq(
        tradingPremisesWithActivitiesGen(AccountancyServices).sample.get,
        tradingPremisesWithActivitiesGen(AccountancyServices, HighValueDealing).sample.get
      )

      val result = service.patchTradingPremises(Seq(1), models, AccountancyServices, true)

      result.headOption.get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(), None))
      result.lift(1).get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(AccountancyServices, HighValueDealing), None))

      result.head.isComplete mustBe false
      result.head.hasChanged mustBe true

    }
  }

  "assignBusinessActivitiesToTradingPremises" must {
    "remove business activities from trading premises" which {
      "also adds one remaining business activity to trading premises without business activity" in new Fixture {

        val models = Seq(
          tradingPremisesWithActivitiesGen(AccountancyServices).sample.get,
          tradingPremisesWithActivitiesGen(AccountancyServices).sample.get,
          tradingPremisesWithActivitiesGen().sample.get,
          tradingPremisesWithActivitiesGen(AccountancyServices).sample.get
        )

        val result = service.assignBusinessActivitiesToTradingPremises(models, Set(AccountancyServices))

        result must be(Seq(
          models.head,
          models(1),
          models(2).copy(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(AccountancyServices))),
            hasAccepted = true,
            hasChanged = true
          ),
          models(3)
        ))

      }
      "also adds first of remaining business activities to trading premises without business activity" in new Fixture {

        val models = Seq(
          tradingPremisesWithActivitiesGen(HighValueDealing, AccountancyServices).sample.get,
          tradingPremisesWithActivitiesGen().sample.get,
          tradingPremisesWithActivitiesGen(HighValueDealing, AccountancyServices).sample.get,
          tradingPremisesWithActivitiesGen(HighValueDealing, AccountancyServices).sample.get
        )

        val result = service.assignBusinessActivitiesToTradingPremises(models, Set(HighValueDealing, AccountancyServices))

        result must be(Seq(
          models.head,
          models(1).copy(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true
          ),
          models(2),
          models(3)
        ))

      }
    }
  }
}
