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

package utils

import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService._
import models.businessmatching._
import models.flowmanagement.AddBusinessTypeFlowModel
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import models.tradingpremises.Address
import play.api.mvc.{AnyContentAsEmpty, Request}
import views.Fixture
import views.html.businessmatching.updateservice.add.UpdateServicesSummaryView

/** Trait to hold the fixtures used for data setup to mixin with the UpdateServicesSummarySpec class.
  *
  * Holds traits required to test the check your answers page given FX.
  */
trait UpdateServicesSummaryFixtures extends AmlsViewSpec {

  /** ViewFixture.
    *
    * Extends Fixture and handles oAuth. All other traits will extend to inherit oAuth
    */
  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  /** SimpleFlowModelViewFixture.
    *
    * View in simple form with high value dealing.
    */
  trait SimpleFlowModelViewFixture extends ViewFixture {
    lazy val update_services_summary = app.injector.instanceOf[UpdateServicesSummaryView]
    override def view                = update_services_summary(AddBusinessTypeFlowModel(activity = Some(HighValueDealing)))
  }

  /** SimpleTCSPViewFixture.
    *
    * View to include TrustAndCompanyServices.
    */
  trait SimpleTCSPViewFixture extends ViewFixture {
    lazy val update_services_summary = app.injector.instanceOf[UpdateServicesSummaryView]
    val completePersonName           = Some(PersonName("Katie", None, "Test"))
    val completePersonName2          = Some(PersonName("David", None, "Test"))
    val completeRp1                  = ResponsiblePerson(completePersonName)
    val completeRp2                  = ResponsiblePerson(completePersonName2)
    override def view                = update_services_summary(AddBusinessTypeFlowModel(activity = Some(TrustAndCompanyServices)))
  }

  /** SimpleTCSPNoFitAndProperViewFixture.
    *
    * View to include TrustAndCompanyServices.
    */
  trait SimpleTCSPNoFitAndProperViewFixture extends ViewFixture {
    lazy val update_services_summary = app.injector.instanceOf[UpdateServicesSummaryView]
    override def view                = update_services_summary(
      AddBusinessTypeFlowModel(
        activity = Some(TrustAndCompanyServices)
      )
    )
  }

  /** MSBViewFixture.
    *
    * View to include MoneyServiceBusiness. Has single CurrencyExchange sub service.
    */
  trait MSBViewFixture extends ViewFixture {
    lazy val update_services_summary = app.injector.instanceOf[UpdateServicesSummaryView]
    override def view                = update_services_summary(
      AddBusinessTypeFlowModel(
        activity = Some(MoneyServiceBusiness),
        subSectors = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
      )
    )
  }

  /** MSBViewFixture.
    *
    * View to include MoneyServiceBusiness. Has single CurrencyExchange sub service.
    */
  trait MSBViewNoPremisesFixture extends ViewFixture {
    lazy val update_services_summary = app.injector.instanceOf[UpdateServicesSummaryView]
    override def view                = update_services_summary(
      AddBusinessTypeFlowModel(
        activity = Some(MoneyServiceBusiness),
        subSectors = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
      )
    )
  }

  /** MSBAllViewFixture.
    *
    * View to include MoneyServiceBusiness. Has all sub services. Has PSR No.
    */
  trait MSBAllViewFixture extends ViewFixture {
    lazy val update_services_summary = app.injector.instanceOf[UpdateServicesSummaryView]
    val completePersonName           = Some(PersonName("Katie", None, "Test"))
    val completeRp1                  = ResponsiblePerson(completePersonName)
    val address                      = Address("1", None, None, None, "AA1 1BB", None)
    override def view                = update_services_summary(
      AddBusinessTypeFlowModel(
        activity = Some(MoneyServiceBusiness),
        subSectors = Some(
          BusinessMatchingMsbServices(
            Set(
              TransmittingMoney,
              CurrencyExchange,
              ForeignExchange,
              ChequeCashingNotScrapMetal,
              ChequeCashingScrapMetal
            )
          )
        ),
        businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("111111"))
      )
    )
  }

  /** MSBSingleViewFixture.
    *
    * View to include MoneyServiceBusiness. Has a single sub services. Has PSR No.
    */
  trait MSBSingleViewFixture extends ViewFixture {
    lazy val update_services_summary = app.injector.instanceOf[UpdateServicesSummaryView]
    override def view                = update_services_summary(
      AddBusinessTypeFlowModel(
        activity = Some(MoneyServiceBusiness),
        subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
        businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("111111"))
      )
    )
  }

  /** MSBNoPSRViewFixture.
    *
    * View to include MoneyServiceBusiness. Has single CurrencyExchange sub service. Has no PSR No.
    */
  trait MSBNoPSRViewFixture extends ViewFixture {
    lazy val update_services_summary = app.injector.instanceOf[UpdateServicesSummaryView]
    override def view                = update_services_summary(
      AddBusinessTypeFlowModel(
        activity = Some(MoneyServiceBusiness),
        subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
        businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo)
      )
    )
  }

  /** MSBViewFixture.
    *
    * View to include MoneyServiceBusiness. Has single CurrencyExchange sub service. Has a PSR No.
    */
  trait SingleSubSectorPSRMSBViewFixture extends ViewFixture {
    lazy val update_services_summary = app.injector.instanceOf[UpdateServicesSummaryView]
    override def view                = update_services_summary(
      AddBusinessTypeFlowModel(
        activity = Some(MoneyServiceBusiness),
        subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
        businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("111111"))
      )
    )
  }
}
