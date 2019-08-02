/*
 * Copyright 2019 HM Revenue & Customs
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

import models.asp.Asp
import models.businessmatching.{BillPaymentServices, BusinessActivities => BMBusinessActivities, _}
import models.estateagentbusiness.EstateAgentBusiness
import models.flowmanagement.RemoveBusinessTypeFlowModel
import models.hvd.Hvd
import models.moneyservicebusiness.{MoneyServiceBusiness => MSBSection}
import models.responsiblepeople.{ApprovalFlags, ResponsiblePerson}
import models.tcsp.Tcsp
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RemoveBusinessTypeHelperSpec extends AmlsSpec with FutureAssertions with MockitoSugar with ScalaFutures {

  val MSBOnlyModel = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness)))

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val helper = new RemoveBusinessTypeHelper(
      self.authConnector,
      mockCacheConnector
    )
  }

  "RemoveBusinessTypeHelper" must {
    "have removeSectionData method which" when {
      "called with business types to remove" must {
        "return seq of updated cache maps" in new Fixture {
          val activitiesToRemove = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(
              HighValueDealing,
              AccountancyServices,
              EstateAgentBusinessService,
              MoneyServiceBusiness,
              TrustAndCompanyServices
            )))

          mockCacheRemoveByKey[MSBSection]
          mockCacheRemoveByKey[Hvd]
          mockCacheRemoveByKey[Tcsp]
          mockCacheRemoveByKey[Asp]
          mockCacheRemoveByKey[EstateAgentBusiness]

          val result = helper.removeSectionData(activitiesToRemove).value

          whenReady(result) {
            case Some(seqOfCache) => seqOfCache.size mustBe 5
          }
        }
      }

      "called with BPS" must {
        "return cache map" in new Fixture {
          val activitiesToRemove = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(
              BillPaymentServices
            )))

          when(mockCacheConnector.fetchAllWithDefault).thenReturn(Future.successful(mockCacheMap))

          val result = helper.removeSectionData(activitiesToRemove).value

          whenReady(result) {
            case Some(seqOfCache) => seqOfCache.size mustBe 1
          }
        }
      }
    }

    "must have removeBusinessMatchingBusinessTypes method which" when {
      "called with activities to remove" must {
        "return updated business matching" in new Fixture {
          val activitiesToRemove = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(
              HighValueDealing,
              AccountancyServices,
              EstateAgentBusinessService,
              MoneyServiceBusiness,
              TrustAndCompanyServices
            )))

          val businessMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(
            HighValueDealing,
            AccountancyServices,
            EstateAgentBusinessService,
            MoneyServiceBusiness,
            TrustAndCompanyServices,
            BillPaymentServices))),
            hasAccepted = true,
            hasChanged = true)

          val newBusinessMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(
            BillPaymentServices))),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[BusinessMatching](Some(businessMatching))

          mockCacheUpdate[BusinessMatching](Some(BusinessMatching.key), newBusinessMatching)

          helper.removeBusinessMatchingBusinessTypes(activitiesToRemove).returnsSome(newBusinessMatching)
        }
      }
    }

    "must have removeTradingPremisesBusinessTypes method which" when {
      "called with activities to remove" must {
        "return updated trading premises" in new Fixture {
          val businessMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(
            HighValueDealing,
            AccountancyServices,
            EstateAgentBusinessService,
            MoneyServiceBusiness,
            TrustAndCompanyServices,
            BillPaymentServices))),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[BusinessMatching](Some(businessMatching), Some(BusinessMatching.key))

          val activitiesToRemove = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(
              HighValueDealing,
              AccountancyServices,
              EstateAgentBusinessService,
              MoneyServiceBusiness,
              TrustAndCompanyServices
            )))

          val testTradingPremises = Seq(TradingPremises(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(
            activities = Set(
            HighValueDealing,
            AccountancyServices,
            EstateAgentBusinessService,
            MoneyServiceBusiness,
            TrustAndCompanyServices,
            BillPaymentServices)))))

          val newTradingPremises = Seq(TradingPremises(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(
              activities = Set(BillPaymentServices))),
            hasAccepted = true))

          mockCacheUpdate[Seq[TradingPremises]](Some(TradingPremises.key), newTradingPremises)

          helper.removeTradingPremisesBusinessTypes(activitiesToRemove).returnsSome(newTradingPremises)
        }
      }
    }
  }

  "removing Responsible People types" when {
    "there is more than one business type" when {
      "the buisness is TCSP and they answered yes to F&P then do not remove the responsible people approval" in new Fixture {

        val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

        val startResultRP = Seq(ResponsiblePerson(
          approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true)),
          hasAccepted = true,
          hasChanged = true))

        val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, BillPaymentServices))),
          businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo),
          hasAccepted = true,
          hasChanged = true)

        mockCacheFetch[BusinessMatching](
          Some(startResultMatching),
          Some(BusinessMatching.key))

        mockCacheUpdate(Some(BusinessMatching.key), startResultMatching)

        mockCacheFetch[Seq[ResponsiblePerson]](
          Some(startResultRP),
          Some(ResponsiblePerson.key))

        val expectedResultRP = Seq(ResponsiblePerson(
          approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true)),
          hasAccepted = true,
          hasChanged = true))


        mockCacheUpdate(Some(ResponsiblePerson.key), expectedResultRP)

        helper.removeFitAndProper(model).returnsSome(expectedResultRP)
      }

      "the buisness is TCSP and they answered no to F&P then do remove the responsible people approval" in new Fixture {

        val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

        val startResultRP = Seq(ResponsiblePerson(
          approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false), hasAlreadyPaidApprovalCheck = Some(true)),
          hasAccepted = true,
          hasChanged = true))

        val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, BillPaymentServices))),
          businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo),
          hasAccepted = true,
          hasChanged = true)

        mockCacheFetch[BusinessMatching](
          Some(startResultMatching),
          Some(BusinessMatching.key))

        mockCacheUpdate(Some(BusinessMatching.key), startResultMatching)

        mockCacheFetch[Seq[ResponsiblePerson]](
          Some(startResultRP),
          Some(ResponsiblePerson.key))

        val expectedResultRP = Seq(ResponsiblePerson(
          approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false), hasAlreadyPaidApprovalCheck = None),
          hasAccepted = true,
          hasChanged = true))

        mockCacheUpdate(Some(ResponsiblePerson.key), expectedResultRP)

        helper.removeFitAndProper(model).returnsSome(expectedResultRP)
      }
    }
  }
}

