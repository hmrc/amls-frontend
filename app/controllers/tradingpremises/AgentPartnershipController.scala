/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, RepeatingSection}
import views.html.tradingpremises.agent_partnership

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class AgentPartnershipController @Inject()(val dataCacheConnector: DataCacheConnector,
                                           val authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           override val messagesApi: MessagesApi,
                                           val cc: MessagesControllerComponents,
                                           agent_partnership: agent_partnership,
                                           implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

    def get(index: Int, edit: Boolean = false) = authAction.async {
      implicit request =>
        getData[TradingPremises](request.credId, index) map {
          case Some(tp) => {
            val form = tp.agentPartnership match {
              case Some(data) => Form2[AgentPartnership](data)
              case None => EmptyForm
            }
            Ok(agent_partnership(form, index, edit))
          }
          case None => NotFound(notFoundView)
        }
    }

   def post(index: Int ,edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[AgentPartnership](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(agent_partnership(f, index,edit)))
        case ValidForm(_, data) => {
          for {
            result <- fetchAllAndUpdateStrict[TradingPremises](request.credId, index) { (_,tp) =>
                TradingPremises(tp.registeringAgentPremises,
                  tp.yourTradingPremises, tp.businessStructure,
                  None, None, Some(data),tp.whatDoesYourBusinessDoAtThisAddress,
                  tp.msbServices, true, tp.lineId, tp.status, tp.endDate)
            }
          } yield edit match {
            case true => Redirect(routes.DetailedAnswersController.get(index))
            case false => TPControllerHelper.redirectToNextPage(result, index, edit)
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
    }
  }
}

