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

package services.flowmanagement.routing

import models.businessmatching.updateservice.{NewActivitiesAtTradingPremisesNo, NewActivitiesAtTradingPremisesYes, TradingPremisesActivities}
import models.businessmatching.{BillPaymentServices, BusinessActivities, HighValueDealing}
import models.flowmanagement._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.Redirect
import services.flowmanagement._
import services.flowmanagement.routings._
import play.api.test.Helpers._

class VariationAddServiceRouterSpec extends PlaySpec {

  trait Fixture {
    val routingFile = VariationAddServiceRouter.router
  }

  "getRoute" must {

    "return the 'trading premises' page (TradingPremisesController)" when {
      "given the 'BusinessActivities' model contains a single activity" in new Fixture {
        val model = AddServiceFlowModel(
          businessActivities = Some(BusinessActivities(Set(HighValueDealing))))
        val result = await(routingFile.getRoute(SelectActivitiesPageId, model))

        result mustBe Redirect(controllers.businessmatching.updateservice.add.routes.TradingPremisesController.get(0))
      }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "given the activity is not done at any trading premises" in new Fixture {
        val model = AddServiceFlowModel(
          businessActivities = Some(BusinessActivities(Set(HighValueDealing))),
          areNewActivitiesAtTradingPremises = Some(NewActivitiesAtTradingPremisesNo))
        val result = await(routingFile.getRoute(TradingPremisesPageId, model))

        result mustBe Redirect(controllers.businessmatching.updateservice.add.routes.UpdateServicesSummaryController.get())
      }
    }

    "return the 'which trading premises' page (WhichTradingPremisesController)" when {
      "given the 'NewActivitiesAtTradingPremisesYes' model contains HVD" in new Fixture {
        val model = AddServiceFlowModel(
          businessActivities = Some(BusinessActivities(Set(HighValueDealing))),
          areNewActivitiesAtTradingPremises = Some(NewActivitiesAtTradingPremisesYes(HighValueDealing)))
        val result = await(routingFile.getRoute(TradingPremisesPageId, model))

        result mustBe Redirect(controllers.businessmatching.updateservice.add.routes.WhichTradingPremisesController.get(0))
      }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "given a set of trading premises has been chosen" in new Fixture {
        val model = AddServiceFlowModel(
          businessActivities = Some(BusinessActivities(Set(HighValueDealing))),
          areNewActivitiesAtTradingPremises = Some(NewActivitiesAtTradingPremisesYes(HighValueDealing)),
          tradingPremisesNewActivities = Some(TradingPremisesActivities(Set(0,1,2)))
        )

        val result = await(routingFile.getRoute(WhichTradingPremisesPageId, model))

        result mustBe Redirect(controllers.businessmatching.updateservice.add.routes.UpdateServicesSummaryController.get())
      }
    }

    "return the 'Do you want add more activities' page (addMoreActivitiesController)" when {
      "we're on the summary page and the user selects continue" in new Fixture {
        val model = AddServiceFlowModel(
          businessActivities = Some(BusinessActivities(Set(HighValueDealing))),
          areNewActivitiesAtTradingPremises = Some(NewActivitiesAtTradingPremisesYes(HighValueDealing)))

        val result = await(routingFile.getRoute(UpdateServiceSummaryPageId, model))

        result mustBe Redirect(controllers.businessmatching.updateservice.add.routes.AddMoreActivitiesController.get())
      }
    }

    "return the 'Activities selection' page (SelectActivitiesController)" when {
      "we're on the 'Do you want at add more activities' page " +
        "and the use wants to add more activities" in new Fixture {
        val model = AddServiceFlowModel(
          businessActivities = Some(BusinessActivities(Set(HighValueDealing))),
          areNewActivitiesAtTradingPremises = Some(NewActivitiesAtTradingPremisesYes(HighValueDealing)),
          addMoreActivities = Some(true))

        val result = await(routingFile.getRoute(AddMoreAcivitiesPageId, model))

        result mustBe Redirect(controllers.businessmatching.updateservice.add.routes.SelectActivitiesController.get())
      }
    }

    "return the 'New Service questions' page (NewServiceInformationController)" when {
      "we're on the 'Do you want at add more activities' page " +
      "and the user has added Activities that require more questions" +
        "and the use doesn't want to add more activities" in new Fixture {
        val model = AddServiceFlowModel(
          businessActivities = Some(BusinessActivities(Set(HighValueDealing))),
          areNewActivitiesAtTradingPremises = Some(NewActivitiesAtTradingPremisesYes(HighValueDealing)),
          addMoreActivities = Some(false))

        val result = await(routingFile.getRoute(AddMoreAcivitiesPageId, model))

        result mustBe Redirect(controllers.businessmatching.updateservice.add.routes.NewServiceInformationController.get())
      }
    }

    "return the 'Registration progress' page (RegistrationProgressController)" when {
      "we're on the 'Do you want at add more activities' page " +
        "and the user has NOT added Activities that require more questions" +
        "and the use doesn't want to add more activities" in new Fixture {
        val model = AddServiceFlowModel(
          businessActivities = Some(BusinessActivities(Set(BillPaymentServices))),
          addMoreActivities = Some(false))

        val result = await(routingFile.getRoute(AddMoreAcivitiesPageId, model))

        result mustBe Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }

    "return the 'registration progress' page" when {
      "we're on the 'new service information' page" in new Fixture {
        val model = AddServiceFlowModel(
          businessActivities = Some(BusinessActivities(Set(HighValueDealing))),
          areNewActivitiesAtTradingPremises = Some(NewActivitiesAtTradingPremisesYes(HighValueDealing)))

        val result = await(routingFile.getRoute(NewServiceInformationPageId, model))

        result mustBe Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }

  }

}
