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

import javax.inject.{Inject, Singleton}
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{Form2, _}
import models.businessmatching.BusinessMatching
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, ControllerHelper, RepeatingSection}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class RegisteringAgentPremisesController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                   val authAction: AuthAction,
                                                   val ds: CommonPlayDependencies,
                                                   override val messagesApi: MessagesApi,
                                                   val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(index: Int, edit: Boolean = false) = authAction.async {
   implicit request =>
      dataCacheConnector.fetchAll(request.credId).map {
        cache =>
          cache.map{ c =>
            getData[TradingPremises](c, index) match {
              case Some(tp) if ControllerHelper.isMSBSelected(c.getEntry[BusinessMatching](BusinessMatching.key)) => {
                val form = tp.registeringAgentPremises match {
                  case Some(service) => Form2[RegisteringAgentPremises](service)
                  case None => EmptyForm
                }
                Ok(views.html.tradingpremises.registering_agent_premises(form, index, edit))
              }
              case Some(tp) if !edit => {
                TPControllerHelper.redirectToNextPage(cache, index, edit)
              }
              case _ => NotFound(notFoundView)
            }
          } getOrElse NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false) = authAction.async {
    implicit request =>
      Form2[RegisteringAgentPremises](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.registering_agent_premises(f, index, edit)))
        case ValidForm(_, data) => {
          for {
            result <- fetchAllAndUpdateStrict[TradingPremises](request.credId, index) { (_,tp) =>
              resetAgentValues(tp.registeringAgentPremises(data), data)
            }
          } yield (data.agentPremises, edit) match {
            case (true, _) => Redirect(routes.BusinessStructureController.get(index,edit))
            case (false, true) => Redirect(routes.DetailedAnswersController.get(index))
            case (false, false) => TPControllerHelper.redirectToNextPage(result, index, edit)
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  private def resetAgentValues(tp:TradingPremises, data:RegisteringAgentPremises):TradingPremises = data.agentPremises match {
    case true => tp.registeringAgentPremises(data)
    case false => tp.copy(agentName=None,businessStructure=None,agentCompanyDetails=None,agentPartnership=None, hasChanged=true)
  }
}

