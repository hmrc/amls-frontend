/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.tradingpremises

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms._
import javax.inject.{Inject, Singleton}
import models.tradingpremises._
import play.api.i18n.MessagesApi
import play.api.mvc.{MessagesControllerComponents, Request}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, RepeatingSection}
import views.html.tradingpremises.activity_start_date

@Singleton
class ActivityStartDateController @Inject()(override val messagesApi: MessagesApi,
                                            val authAction: AuthAction,
                                            val ds: CommonPlayDependencies,
                                            val dataCacheConnector: DataCacheConnector,
                                            val cc: MessagesControllerComponents,
                                            activity_start_date: activity_start_date,
                                            implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(index: Int, edit: Boolean = false) = authAction.async {
    implicit request =>
        getData[TradingPremises](request.credId, index) map {
          case Some(tpSection) =>
            tpSection.yourTradingPremises match {
              case Some(YourTradingPremises(_, tradingPremisesAddress, _, None, _)) =>
                Ok(activity_start_date(EmptyForm, index, edit, tradingPremisesAddress))
              case Some(YourTradingPremises(_, tradingPremisesAddress, _, Some(startDate), _)) =>
                Ok(activity_start_date(Form2[ActivityStartDate](ActivityStartDate(startDate)), index, edit, tradingPremisesAddress))
              case _ =>
               NotFound(notFoundView)
            }
          case _ =>
            NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[ActivityStartDate](request.body) match {
          case f: InvalidForm =>
            handleInvalidForm(request.credId, index, edit, f)
          case ValidForm(_, data) =>
            handleValidForm(request.credId, index, edit, data)

        }
  }

  private def handleValidForm(credId: String, index: Int, edit: Boolean, data: ActivityStartDate)
                             (implicit hc: HeaderCarrier) = {
    for {
      _ <- updateDataStrict[TradingPremises](credId, index) { tp =>
        val ytp = tp.yourTradingPremises.fold[Option[YourTradingPremises]](None)(x => Some(x.copy(startDate = Some(data.startDate))))
        tp.copy(yourTradingPremises = ytp, hasChanged = true)
      }
    } yield edit match {
      case true => Redirect(routes.DetailedAnswersController.get(index))
      case false => Redirect(routes.IsResidentialController.get(index, edit))
    }
  }

  private def handleInvalidForm(credId: String, index: Int, edit: Boolean, f: InvalidForm)
                               (implicit hc: HeaderCarrier,
                                request: Request[_]) = {
    for {
      tp <- getData[TradingPremises](credId, index)
    } yield tp.flatMap(_.yourTradingPremises) match {
      case Some(ytp) =>
        BadRequest(activity_start_date(f, index, edit, ytp.tradingPremisesAddress))
      case _ => NotFound(notFoundView)
    }
  }
}
