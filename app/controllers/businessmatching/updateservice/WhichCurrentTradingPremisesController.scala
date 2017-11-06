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

package controllers.businessmatching.updateservice

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.DateOfChange
import models.businessmatching.updateservice.{TradingPremisesActivities => TradingPremisesForm}
import models.businessmatching.{BusinessActivities, BusinessActivity}
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{RepeatingSection, StatusConstants}
import views.html.businessmatching.updateservice.which_current_trading_premises
import routes._

import scala.collection.immutable.SortedSet

class WhichCurrentTradingPremisesController @Inject()(val authConnector: AuthConnector,
                                                      val dataCacheConnector: DataCacheConnector,
                                                      businessMatchingService: BusinessMatchingService) extends BaseController with RepeatingSection {

  private val failure = InternalServerError("Could not get form data")

  def get(index: Int = 0) = Authorised.async {
    implicit authContext => implicit request =>
      formData map { case (tp, _, act) =>
        Ok(which_current_trading_premises(EmptyForm, tp, BusinessActivities.getValue(act.toSeq(index)), index))
      } getOrElse failure
  }

  def post(index: Int = 0) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[TradingPremisesForm](request.body) match {
        case f: InvalidForm => {
          for {
            (tradingPremises, _, act) <- formData
          } yield BadRequest(which_current_trading_premises(f, tradingPremises, BusinessActivities.getValue(act.toSeq(index)), index))
        } getOrElse failure

        case ValidForm(_, data) => {
          for {
            (tp, _, act) <- formData
            _ <- OptionT.liftF(dataCacheConnector.save[Seq[TradingPremises]](TradingPremises.key, fixActivities(tp.map(_._1), data.index, act.toList(index))))
            fitAndProperRequired <- businessMatchingService.fitAndProperRequired
          } yield if(activitiesToIterate(index, act)) {
            Redirect(CurrentTradingPremisesController.get(index + 1))
          } else if(fitAndProperRequired) {
            Redirect(FitAndProperController.get())
          } else {
            Redirect(NewServiceInformationController.get())
          }
        } getOrElse failure
      }
  }

  private def fixActivities(tp: Seq[TradingPremises], selected: Set[Int], activity: BusinessActivity): Seq[TradingPremises] = tp.zipWithIndex.collect {
    case (m, i) if !selected.contains(i) && m.whatDoesYourBusinessDoAtThisAddress.isDefined =>
      updateActivities(m, m.whatDoesYourBusinessDoAtThisAddress.get.activities.filter(_.equals(activity)))
    case (m, _) if m.whatDoesYourBusinessDoAtThisAddress.isDefined =>
      updateActivities(m, m.whatDoesYourBusinessDoAtThisAddress.get.activities + activity)
    case (m, _) => m
  }

  private def updateActivities(tp: TradingPremises, activities: Set[BusinessActivity]) =
    tp.whatDoesYourBusinessDoAtThisAddress(WhatDoesYourBusinessDo(activities, tp.whatDoesYourBusinessDoAtThisAddress.fold(none[DateOfChange])(_.dateOfChange)))

  private def activitiesToIterate(index: Int, activities: Set[BusinessActivity]) =
    activities.size > index + 1

  private def formData(implicit hc: HeaderCarrier, ac: AuthContext) = for {
    tp <- getTradingPremises
    activities <- businessMatchingService.getSubmittedBusinessActivities
    } yield (tp, activities, activities)

  private def getTradingPremises(implicit hc: HeaderCarrier, ac: AuthContext) =
    OptionT.liftF(getData[TradingPremises].map{ tradingpremises =>
      tradingpremises.zipWithIndex.filterNot{ case (tp, _) =>
        tp.status.contains(StatusConstants.Deleted) | !tp.isComplete
      }
    })

}
