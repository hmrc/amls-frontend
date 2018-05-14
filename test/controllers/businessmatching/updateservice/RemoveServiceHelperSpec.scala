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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils._

import scala.concurrent.ExecutionContext.Implicits.global

class RemoveServiceHelperSpec extends AmlsSpec with FutureAssertions with MockitoSugar with ScalaFutures {


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
            msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo),
            hasAccepted = true,
            hasChanged = true)

          val endResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing))),
            msbServices = None,
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

      "removing EAB" should {

        "remove the BusinessMatching Business Activity EAB (Type)" in new Fixture {

          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(EstateAgentBusinessService, BillPaymentServices)))

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, EstateAgentBusinessService))),
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
      }

      "removing TCSP" should {

        "remove the BusinessMatching Business Activity TCSP (Type)" in new Fixture {

          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, TrustAndCompanyServices))),
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
      }

      "removing BP" should {

        "remove the BusinessMatching Business Activity BP (Type)" in new Fixture {

          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, BillPaymentServices))),
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
      }

      "removing TDI" should {

        "remove the BusinessMatching Business Activity TDI (Type)" in new Fixture {

          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, TelephonePaymentService)))

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, TelephonePaymentService))),
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
      }

      "removing ASP" should {

        "remove the BusinessMatching Business Activity ASP (Type)" in new Fixture {

          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, AccountancyServices)))

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, AccountancyServices))),
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
      }

      "removing HVD" should {

        "remove the BusinessMatching Business Activity HVD (Type)" in new Fixture {

          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, HighValueDealing)))

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, AccountancyServices))),
            hasAccepted = true,
            hasChanged = true)

          val endResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(AccountancyServices))),
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

          val startResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, MoneyServiceBusiness))),
            hasAccepted = true,
            hasChanged = true)

          val endResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[TradingPremises](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessActivities(model).returnsSome(endResultTP)
        }

        "remove the TradingPremises MSB Services" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

          val startResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, MoneyServiceBusiness))),
            msbServices = Some(TradingPremisesMsbServices(Set(CurrencyExchange))),
            hasAccepted = true,
            hasChanged = true)

          val endResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            msbServices = None,
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[TradingPremises](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessActivities(model).returnsSome(endResultTP)
        }
      }

      "removing an EAB" should {

        "remove the TradingPremises Business Activity EAB (Type)" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(EstateAgentBusinessService, BillPaymentServices)))

          val startResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, EstateAgentBusinessService))),
            hasAccepted = true,
            hasChanged = true)

          val endResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[TradingPremises](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessActivities(model).returnsSome(endResultTP)
        }
      }

      "removing an TCSP" should {

        "remove the TradingPremises Business Activity TCSP (Type)" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

          val startResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, TrustAndCompanyServices))),
            hasAccepted = true,
            hasChanged = true)

          val endResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[TradingPremises](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessActivities(model).returnsSome(endResultTP)
        }
      }

      "removing an BP" should {

        "remove the TradingPremises Business Activity BP (Type)" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(BillPaymentServices, BillPaymentServices)))

          val startResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, BillPaymentServices))),
            hasAccepted = true,
            hasChanged = true)

          val endResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[TradingPremises](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessActivities(model).returnsSome(endResultTP)
        }
      }

      "removing an TDI" should {

        "remove the TradingPremises Business Activity TDI (Type)" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(TelephonePaymentService, BillPaymentServices)))

          val startResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, TelephonePaymentService))),
            hasAccepted = true,
            hasChanged = true)

          val endResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[TradingPremises](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessActivities(model).returnsSome(endResultTP)
        }
      }

      "removing an ASP" should {

        "remove the TradingPremises Business Activity ASP (Type)" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(AccountancyServices, BillPaymentServices)))

          val startResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(AccountancyServices, HighValueDealing))),
            hasAccepted = true,
            hasChanged = true)

          val endResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[TradingPremises](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessActivities(model).returnsSome(endResultTP)
        }
      }

      "removing an HVD" should {

        "remove the TradingPremises Business Activity HVD (Type)" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(HighValueDealing, BillPaymentServices)))

          val startResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, EstateAgentBusinessService))),
            hasAccepted = true,
            hasChanged = true)

          val endResultTP = TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(EstateAgentBusinessService))),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[TradingPremises](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessActivities(model).returnsSome(endResultTP)
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

      "removing an TCSP" should {

        "remove the ResponsiblePeople fit and proper if there is no MSB" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

          val startResultRP = ResponsiblePeople(hasAlreadyPassedFitAndProper = Some(true),
            hasAccepted = true,
            hasChanged = true)

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, BillPaymentServices))),
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

        "not remove the ResponsiblePeople fit and proper if there is MSB" in new Fixture {
          val model = RemoveServiceFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

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


