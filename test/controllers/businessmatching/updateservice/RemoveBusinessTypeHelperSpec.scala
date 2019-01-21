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

import config.ApplicationConfig
import models.asp.Asp
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessActivities => BMBusinessActivities, _}
import models.estateagentbusiness.EstateAgentBusiness
import models.moneyservicebusiness.{ExpectedThroughput, MoneyServiceBusiness => MoneyServiceBusinessSection}
import models.flowmanagement.RemoveBusinessTypeFlowModel
import models.hvd.Hvd
import models.responsiblepeople.{ApprovalFlags, ResponsiblePerson}
import models.tcsp.Tcsp
import models.tradingpremises.{CurrencyExchange, TradingPremises, TradingPremisesMsbServices, WhatDoesYourBusinessDo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils._
import play.api.test.Helpers._
import org.mockito.Mockito.{never, verify}
import org.mockito.Matchers.{any, eq => eqTo}
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.{Application, Mode}
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext.Implicits.global

class RemoveBusinessTypeHelperSpec extends AmlsSpec with FutureAssertions with MockitoSugar with ScalaFutures {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.phase-2-changes" -> false)
    .build()

  val MSBOnlyModel = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness)))

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val helper = new RemoveBusinessTypeHelper(
      self.authConnector,
      mockCacheConnector
    )

    val businessMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(MoneyServiceBusiness, HighValueDealing, TrustAndCompanyServices))))
    mockCacheFetch(Some(businessMatching), Some(BusinessMatching.key))
  }

  "removing BusinessMatching business types" when {

    "there is more than one business type" when {

      "removing an MSB" should {

        "remove the BusinessMatching Business Activity MSB (Type)" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

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

          helper.removeBusinessMatchingBusinessTypes(model).returnsSome(endResultMatching)
        }

        "remove the BusinessMatching MSB Services" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

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

          helper.removeBusinessMatchingBusinessTypes(model).returnsSome(endResultMatching)
        }

        "remove the BusinessMatching PSR" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

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

          helper.removeBusinessMatchingBusinessTypes(model).returnsSome(endResultMatching)
        }
      }

      "removing EAB" should {

        "remove the BusinessMatching Business Activity EAB (Type)" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(EstateAgentBusinessService, BillPaymentServices)))

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

          helper.removeBusinessMatchingBusinessTypes(model).returnsSome(endResultMatching)
        }
      }

      "removing TCSP" should {

        "remove the BusinessMatching Business Activity TCSP (Type)" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

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

          helper.removeBusinessMatchingBusinessTypes(model).returnsSome(endResultMatching)
        }
      }

      "removing BP" should {

        "remove the BusinessMatching Business Activity BP (Type)" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

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

          helper.removeBusinessMatchingBusinessTypes(model).returnsSome(endResultMatching)
        }
      }

      "removing TDI" should {

        "remove the BusinessMatching Business Activity TDI (Type)" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, TelephonePaymentService)))

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

          helper.removeBusinessMatchingBusinessTypes(model).returnsSome(endResultMatching)
        }
      }

      "removing ASP" should {

        "remove the BusinessMatching Business Activity ASP (Type)" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, AccountancyServices)))

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

          helper.removeBusinessMatchingBusinessTypes(model).returnsSome(endResultMatching)
        }
      }

      "removing HVD" should {

        "remove the BusinessMatching Business Activity HVD (Type)" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, HighValueDealing)))

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

          helper.removeBusinessMatchingBusinessTypes(model).returnsSome(endResultMatching)
        }
      }
    }
  }

  "removing TradingPremises business types" when {

    "there is more than one business type" when {

      "removing an MSB" should {

        "remove the TradingPremises Business Activity MSB (Type)" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

          val startResultTP = Seq(TradingPremises(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, MoneyServiceBusiness))),
            hasAccepted = true,
            hasChanged = true))

          val endResultTP = Seq(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true))

          mockCacheFetch[Seq[TradingPremises]](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessTypes(model).returnsSome(endResultTP)
        }

        "remove the TradingPremises MSB Services" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

          val startResultTP = Seq(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, MoneyServiceBusiness))),
            msbServices = Some(TradingPremisesMsbServices(Set(CurrencyExchange))),
            hasAccepted = true,
            hasChanged = true))

          val endResultTP = Seq(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            msbServices = None,
            hasAccepted = true,
            hasChanged = true))

          mockCacheFetch[Seq[TradingPremises]](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessTypes(model).returnsSome(endResultTP)
        }
      }

      "removing an EAB" should {

        "remove the TradingPremises Business Activity EAB (Type)" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(EstateAgentBusinessService, BillPaymentServices)))

          val startResultTP = Seq(TradingPremises(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, EstateAgentBusinessService))),
            hasAccepted = true,
            hasChanged = true))

          val endResultTP = Seq(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true))

          mockCacheFetch[Seq[TradingPremises]](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessTypes(model).returnsSome(endResultTP)
        }
      }

      "removing an TCSP" should {

        "remove the TradingPremises Business Activity TCSP (Type)" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

          val startResultTP = Seq(TradingPremises(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, TrustAndCompanyServices))),
            hasAccepted = true,
            hasChanged = true))

          val endResultTP = Seq(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true))

          mockCacheFetch[Seq[TradingPremises]](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessTypes(model).returnsSome(endResultTP)
        }
      }

      "removing an BP" should {

        "remove the TradingPremises Business Activity BP (Type)" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(BillPaymentServices, BillPaymentServices)))

          val startResultTP = Seq(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, BillPaymentServices))),
            hasAccepted = true,
            hasChanged = true))

          val endResultTP = Seq(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true))

          mockCacheFetch[Seq[TradingPremises]](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessTypes(model).returnsSome(endResultTP)
        }
      }

      "removing an TDI" should {

        "remove the TradingPremises Business Activity TDI (Type)" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(TelephonePaymentService, BillPaymentServices)))

          val startResultTP = Seq(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, TelephonePaymentService))),
            hasAccepted = true,
            hasChanged = true))

          val endResultTP = Seq(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true))

          mockCacheFetch[Seq[TradingPremises]](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessTypes(model).returnsSome(endResultTP)
        }
      }

      "removing an ASP" should {

        "remove the TradingPremises Business Activity ASP (Type)" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(AccountancyServices, BillPaymentServices)))

          val startResultTP = Seq(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(AccountancyServices, HighValueDealing))),
            hasAccepted = true,
            hasChanged = true))

          val endResultTP = Seq(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
            hasAccepted = true,
            hasChanged = true))

          mockCacheFetch[Seq[TradingPremises]](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessTypes(model).returnsSome(endResultTP)
        }
      }

      "removing an HVD" should {

        "remove the TradingPremises Business Activity HVD (Type)" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(HighValueDealing, BillPaymentServices)))

          val startResultTP = Seq(TradingPremises(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing, EstateAgentBusinessService))),
            hasAccepted = true,
            hasChanged = true))

          val endResultTP = Seq(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(EstateAgentBusinessService))),
            hasAccepted = true,
            hasChanged = true))

          mockCacheFetch[Seq[TradingPremises]](
            Some(startResultTP),
            Some(TradingPremises.key))

          mockCacheUpdate(Some(TradingPremises.key), startResultTP)

          helper.removeTradingPremisesBusinessTypes(model).returnsSome(endResultTP)
        }
      }

      "removing all of the services" should {
        "pre-populate the 'what does your business do question" when {
          "the business itself only has one remaining registered service" in new Fixture {
            override val businessMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(MoneyServiceBusiness, HighValueDealing))))

            mockCacheFetch(Some(businessMatching), Some(BusinessMatching.key))

            val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(HighValueDealing)))

            val startResultTP = Seq(TradingPremises(
              whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))),
              hasAccepted = true,
              hasChanged = true))

            val endResultTP = Seq(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(MoneyServiceBusiness))),
              hasAccepted = true,
              hasChanged = true))

            mockCacheFetch[Seq[TradingPremises]](
              Some(startResultTP),
              Some(TradingPremises.key))

            mockCacheUpdate(Some(TradingPremises.key), startResultTP)

            helper.removeTradingPremisesBusinessTypes(model).returnsSome(endResultTP)
          }
        }
      }
    }
  }

  "removing Responsible People types" when {
    "there is more than one business type" when {
      "removing an MSB" should {
        "remove the ResponsiblePeople fit and proper if there is no TCSP and phase-2-changes toggle is false" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

          val startResultRP = Seq(ResponsiblePerson(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
            hasAccepted = true,
            hasChanged = true))

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(HighValueDealing, MoneyServiceBusiness))),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo),
            hasAccepted = true,
            hasChanged = true)

          mockCacheFetch[BusinessMatching](Some(startResultMatching), Some(BusinessMatching.key))
          mockCacheFetch[Seq[ResponsiblePerson]](Some(startResultRP), Some(ResponsiblePerson.key))
          mockCacheUpdate(Some(BusinessMatching.key), startResultMatching)
          mockCacheUpdate(Some(ResponsiblePerson.key), startResultRP)

          val endResultRP = Seq(ResponsiblePerson(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = None, hasAlreadyPaidApprovalCheck = None),
            hasAccepted = true,
            hasChanged = true))

          helper.removeFitAndProper(model).returnsSome(endResultRP)
        }

        "not remove the ResponsiblePeople fit and proper if there is TCSP and phase-2-changes toggle is false" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

          val startResultRP = Seq(ResponsiblePerson(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
            hasAccepted = true,
            hasChanged = true))

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices, MoneyServiceBusiness))),
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

          mockCacheUpdate(Some(ResponsiblePerson.key), startResultRP)

          helper.removeFitAndProper(model).returnsSome(startResultRP)
        }
      }

      "removing an TCSP" should {

        "remove the ResponsiblePeople fit and proper if there is no MSB and phase-2-changes toggle is false" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

          val startResultRP = Seq(ResponsiblePerson(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true)),
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

          mockCacheUpdate(Some(ResponsiblePerson.key), startResultRP)

          val endResultRP = Seq(ResponsiblePerson(
            hasAccepted = true,
            hasChanged = true))

          helper.removeFitAndProper(model).returnsSome(endResultRP)
        }

        "not remove the ResponsiblePeople fit and proper if there is MSB and phase-2-changes is false" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

          val startResultRP = Seq(ResponsiblePerson(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
            hasAccepted = true,
            hasChanged = true))

          val startResultMatching = BusinessMatching(activities = Some(BMBusinessActivities(Set(TrustAndCompanyServices, MoneyServiceBusiness))),
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

          mockCacheUpdate(Some(ResponsiblePerson.key), startResultRP)

          helper.removeFitAndProper(model).returnsSome(startResultRP)
        }
      }
    }
  }

  "date of change" should {

    "not be applicable" when {


      "there is one service to remove and it hasn't been submitted" in new Fixture {
        val justRemoved = Set[BusinessActivity](BillPaymentServices)
        val justAdded = ServiceChangeRegister(addedActivities = Some(Set(BillPaymentServices)))

        mockCacheFetch[ServiceChangeRegister](
          Some(justAdded),
          Some(ServiceChangeRegister.key))

        helper.dateOfChangeApplicable(justRemoved).returnsSome(false)

      }

      "there are multiple services to remove and none of them have been submitted" in new Fixture {
        val justRemoved = Set[BusinessActivity](BillPaymentServices, MoneyServiceBusiness)
        val justAdded = ServiceChangeRegister(addedActivities = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

        mockCacheFetch[ServiceChangeRegister](
          Some(justAdded),
          Some(ServiceChangeRegister.key))

        helper.dateOfChangeApplicable(justRemoved).returnsSome(false)

      }

    }

    "be applicable" when {

      "there are multiple services to remove which have not been submitted" in new Fixture {

        val justRemoved = Set[BusinessActivity](BillPaymentServices, MoneyServiceBusiness, TrustAndCompanyServices)
        val justAdded = ServiceChangeRegister(addedActivities = Some(Set(MoneyServiceBusiness, BillPaymentServices)))

        mockCacheFetch[ServiceChangeRegister](
          Some(justAdded),
          Some(ServiceChangeRegister.key))

        helper.dateOfChangeApplicable(justRemoved).returnsSome(true)

      }

      "there are multiple services to remove which have all been submitted" in new Fixture {

        val justRemoved = Set[BusinessActivity](BillPaymentServices, MoneyServiceBusiness, TrustAndCompanyServices)
        val justAdded = ServiceChangeRegister(None)

        mockCacheFetch[ServiceChangeRegister](
          Some(justAdded),
          Some(ServiceChangeRegister.key))

        helper.dateOfChangeApplicable(justRemoved).returnsSome(true)

      }

      "the services to remove are not newly added and are different" in new Fixture{

        val justRemoved = Set[BusinessActivity](MoneyServiceBusiness, BillPaymentServices)
        val justAdded = ServiceChangeRegister(addedActivities = Some(Set(TrustAndCompanyServices)))

        mockCacheFetch[ServiceChangeRegister](
          Some(justAdded),
          Some(ServiceChangeRegister.key))

        helper.dateOfChangeApplicable(justRemoved).returnsSome(true)

      }
    }
  }

  "Removing the section data" should {
    "clear the data from the cache" when {
      "removing MSB" in new Fixture {
        mockCacheRemoveByKey[MoneyServiceBusinessSection]

        val result = await(helper.removeSectionData(RemoveBusinessTypeFlowModel(Some(Set(MoneyServiceBusiness)))).value)

        verify(mockCacheConnector).removeByKey[MoneyServiceBusinessSection](
          eqTo(MoneyServiceBusinessSection.key)
        )(any(), any(), any())
      }

      "removing HVD" in new Fixture {
        mockCacheRemoveByKey[Hvd]

        val result = await(helper.removeSectionData(RemoveBusinessTypeFlowModel(Some(Set(HighValueDealing)))).value)

        verify(mockCacheConnector).removeByKey[Hvd](
          eqTo(Hvd.key)
        )(any(), any(), any())
      }

      "removing TCSP" in new Fixture {
        mockCacheRemoveByKey[Tcsp]

        val result = await(helper.removeSectionData(RemoveBusinessTypeFlowModel(Some(Set(TrustAndCompanyServices)))).value)

        verify(mockCacheConnector).removeByKey[Tcsp](
          eqTo(Tcsp.key)
        )(any(), any(), any())
      }

      "removing ASP" in new Fixture {
        mockCacheRemoveByKey[Asp]

        val result = await(helper.removeSectionData(RemoveBusinessTypeFlowModel(Some(Set(AccountancyServices)))).value)

        verify(mockCacheConnector).removeByKey[Asp](
          eqTo(Asp.key)
        )(any(), any(), any())
      }

      "removing EAB" in new Fixture {
        mockCacheRemoveByKey[EstateAgentBusiness]

        val result = await(helper.removeSectionData(RemoveBusinessTypeFlowModel(Some(Set(EstateAgentBusinessService)))).value)

        verify(mockCacheConnector).removeByKey[EstateAgentBusiness](
          eqTo(EstateAgentBusiness.key)
        )(any(), any(), any())
      }

      "removing multiple services" in new Fixture {
        mockCacheRemoveByKey[EstateAgentBusiness]
        mockCacheRemoveByKey[Tcsp]

        val result = await(helper.removeSectionData(
          RemoveBusinessTypeFlowModel(Some(Set(EstateAgentBusinessService, TrustAndCompanyServices)))).value)

        verify(mockCacheConnector).removeByKey[EstateAgentBusiness](
          eqTo(EstateAgentBusiness.key)
        )(any(), any(), any())

        verify(mockCacheConnector).removeByKey[Tcsp](
          eqTo(Tcsp.key)
        )(any(), any(), any())

        verify(mockCacheConnector, never).save[MoneyServiceBusinessSection](eqTo(MoneyServiceBusinessSection.key), any())(any(), any(), any())
        verify(mockCacheConnector, never).save[Asp](eqTo(Asp.key), any())(any(), any(), any())
        verify(mockCacheConnector, never).save[Hvd](eqTo(Hvd.key), any())(any(), any(), any())
      }
    }
  }

  "Removing the flow model data" should {
    "empty the data model from the cache" in new Fixture {
      mockCacheSave[RemoveBusinessTypeFlowModel](RemoveBusinessTypeFlowModel(), Some(RemoveBusinessTypeFlowModel.key))

      helper.removeFlowData returnsSome RemoveBusinessTypeFlowModel()
    }
  }
}

class RemoveBusinessTypeHelperSpecForPhase2 extends AmlsSpec with FutureAssertions with MockitoSugar with ScalaFutures {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.phase-2-changes" -> true)
    .build()

  val MSBOnlyModel = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(MoneyServiceBusiness)))

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val helper = new RemoveBusinessTypeHelper(
      self.authConnector,
      mockCacheConnector
    )
  }

  "removing Responsible People types" when {
    "there is more than one business type" when {
      "the buisness is TCSP and they answered yes to F&P then do not remove the responsible people approval if the phase-2-changes toggle is true" in new Fixture {

        val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

        val startResultRP = Seq(ResponsiblePerson(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true)),
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

        val expectedResultRP = Seq(ResponsiblePerson(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true)),
          hasAccepted = true,
          hasChanged = true))


        mockCacheUpdate(Some(ResponsiblePerson.key), expectedResultRP)

        helper.removeFitAndProper(model).returnsSome(expectedResultRP)
      }
      "the buisness is TCSP and they answered no to F&P then do remove the responsible people approval if the phase-2-changes toggle is true" in new Fixture {

        val model = RemoveBusinessTypeFlowModel(activitiesToRemove = Some(Set(TrustAndCompanyServices, BillPaymentServices)))

        val startResultRP = Seq(ResponsiblePerson(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false), hasAlreadyPaidApprovalCheck = Some(true)),
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

        val expectedResultRP = Seq(ResponsiblePerson(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false), hasAlreadyPaidApprovalCheck = None),
          hasAccepted = true,
          hasChanged = true))

        mockCacheUpdate(Some(ResponsiblePerson.key), expectedResultRP)

        helper.removeFitAndProper(model).returnsSome(expectedResultRP)
      }
    }
  }
}

