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

package controllers.tradingpremises

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.tradingpremises._
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

@Singleton
class AgentCompanyNameController @Inject()(val dataCacheConnector: DataCacheConnector,
                                           val authConnector: AuthConnector,
                                           override val messagesApi: MessagesApi)extends RepeatingSection with BaseController {

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>

      getData[TradingPremises](index) map {

        case Some(tp) => {
          val form = tp.agentCompanyDetails match {
            case Some(data) => Form2[AgentCompanyDetails](data)
            case None => EmptyForm
          }
          Ok(views.html.tradingpremises.agent_company_name(form, index, edit))
        }
        case None => NotFound(notFoundView)
      }
  }

  def post(index: Int ,edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[AgentCompanyName](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.agent_company_name(f, index,edit)))
        case ValidForm(_, data) => {
          for {
            result <- fetchAllAndUpdateStrict[TradingPremises](index) { (_, tp) =>
              TradingPremises(tp.registeringAgentPremises,
                tp.yourTradingPremises,
                tp.businessStructure, None, Some(data) , None, tp.whatDoesYourBusinessDoAtThisAddress,
                tp.msbServices, true, tp.lineId, tp.status, tp.endDate)
            }
          } yield edit match {
            case true => Redirect(routes.YourTradingPremisesController.getIndividual(index))
            case false => TPControllerHelper.redirectToNextPage(result, index, edit)
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
    }
  }
}
