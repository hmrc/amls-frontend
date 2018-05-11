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

import models.businessmatching.{BusinessActivities => BMBusinessActivities, _}
import models.flowmanagement.RemoveServiceFlowModel
import models.responsiblepeople.ResponsiblePeople
import models.tradingpremises.{CurrencyExchange, TradingPremises, TradingPremisesMsbServices, WhatDoesYourBusinessDo}
import utils.{AuthorisedFixture, DependencyMocks, FutureAssertions, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global

class RemoveServiceHelperSpec extends GenericTestHelper with FutureAssertions {


  val MSBOnlyModel = RemoveServiceFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness)))

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val helper = new RemoveServiceHelper(
      self.authConnector,
      mockCacheConnector
    )
  }

  "removing BusinessMatching business types" when {

    "there is more than one business type" when {

      "removing an MSB" should {

        "remove the BusinessMatching Business Activity MSB (Type)" in new Fixture {

          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, MoneyServiceBusiness))),
            hasAccepted = true,
            hasChanged = true)

          val endResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[BusinessMatching](
            Some(startResultMatching),
            Some(BusinessMatching.key))

          mockCacheUpdate(Some(BusinessMatching.key), startResultMatching)

          helper.removeBusinessMatchingBusinessActivities(model).returnsSome(endResultMatching)
        }

        "remove the BusinessMatching MSB Services" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, MoneyServiceBusiness))),
            msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
            hasAccepted = true,
            hasChanged = true)

          val endResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing))),
            msbServices = None,
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[BusinessMatching](
            Some(startResultMatching),
            Some(BusinessMatching.key))

          mockCacheUpdate(Some(BusinessMatching.key), startResultMatching)

          helper.removeBusinessMatchingBusinessActivities(model).returnsSome(endResultMatching)
        }

        "remove the BusinessMatching PSR" in new Fixture {

          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, MoneyServiceBusiness))),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo),
            hasAccepted = true,
            hasChanged = true)

          val endResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing))),
            businessAppliedForPSRNumber = None,
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[BusinessMatching](
            Some(startResultMatching),
            Some(BusinessMatching.key))

          mockCacheUpdate(Some(BusinessMatching.key), startResultMatching)

          helper.removeBusinessMatchingBusinessActivities(model).returnsSome(endResultMatching)
        }
      }
    }
  }

  "removing TradingPremises business types" when {

    "there is more than one business type" when {

      "removing an MSB" should {

        "remove the TradingPremises Business Activity MSB (Type)" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

          val startResultMatching = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, MoneyServiceBusiness))),
            hasAccepted = true,
            hasChanged = true)

          val endResultMatching = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[TradingPremises](
            Some(startResultMatching),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultMatching)

          helper.removeTradingPremisesBusinessActivities(model).returnsSome(endResultMatching)
        }

        "remove the TradingPremises MSB Services" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

          val startResultMatching = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, MoneyServiceBusiness))),
            msbServices = Some(TradingPremisesMsbServices(Set(CurrencyExchange))),
            hasAccepted = true,
            hasChanged = true)

          val endResultMatching = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            msbServices = None,
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[TradingPremises](
            Some(startResultMatching),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultMatching)

          helper.removeTradingPremisesBusinessActivities(model).returnsSome(endResultMatching)
        }
      }
    }
  }

  "removing Responsible People types" when {

    "there is more than one business type" when {

      "removing an MSB" should {

        "remove the ResponsiblePeople fit and proper if there is no TCSP" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

          val startResultRP = ResponsiblePeople(hasAlreadyPassedFitAndProper = Some(true),
            hasAccepted = true,
            hasChanged = true)

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, MoneyServiceBusiness))),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[BusinessMatching](
            Some(startResultMatching),
            Some(BusinessMatching.key))

          mockCacheUpdate(Some(BusinessMatching.key), startResultMatching)

          mockCacheFetch[ResponsiblePeople](
            Some(startResultRP),
            Some(ResponsiblePeople.key))

          mockCacheUpdate(Some(ResponsiblePeople.key), startResultRP)

          val endResultRP = ResponsiblePeople(hasAlreadyPassedFitAndProper = None,
            hasAccepted = true,
            hasChanged = true)

          helper.removeFitAndProper(model).returnsSome(endResultRP)
        }

        "not remove the ResponsiblePeople fit and proper if there is TCSP" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

          val startResultRP = ResponsiblePeople(hasAlreadyPassedFitAndProper = Some(true),
            hasAccepted = true,
            hasChanged = true)

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices, MoneyServiceBusiness))),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[BusinessMatching](
            Some(startResultMatching),
            Some(BusinessMatching.key))

          mockCacheUpdate(Some(BusinessMatching.key), startResultMatching)

          mockCacheFetch[ResponsiblePeople](
            Some(startResultRP),
            Some(ResponsiblePeople.key))

          mockCacheUpdate(Some(ResponsiblePeople.key), startResultRP)

          helper.removeFitAndProper(model).returnsSome(startResultRP)
        }
      }
    }
  }

}


