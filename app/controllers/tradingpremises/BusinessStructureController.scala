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

package controllers.tradingpremises

import javax.inject.{Inject, Singleton}

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.tradingpremises._
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}

import scala.concurrent.Future

@Singleton
class BusinessStructureController @Inject()(val dataCacheConnector: DataCacheConnector,
                                            val authConnector: AuthConnector,
                                            override val messagesApi: MessagesApi) extends RepeatingSection with BaseController {

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        response =>
          val form = (for {
            tp <- response
            services <- tp.businessStructure
          } yield Form2[BusinessStructure](services)).getOrElse(EmptyForm)

          Ok(views.html.tradingpremises.business_structure(form, index, edit))
      }
  }

  def redirectToPage(data: BusinessStructure, edit: Boolean, index: Int, result: Option[CacheMap])(implicit request: Request[AnyContent]) = {
    data match {
      case SoleProprietor => Redirect(routes.AgentNameController.get(index, edit))
      case LimitedLiabilityPartnership | IncorporatedBody if ApplicationConfig.release7 => Redirect(routes.AgentCompanyDetailsController.get(index, edit))
      case Partnership => Redirect(routes.AgentPartnershipController.get(index, edit))
      case UnincorporatedBody => edit match {
        case true => Redirect(routes.SummaryController.getIndividual(index))
        case false => TPControllerHelper.redirectToNextPage(result, index, edit)
      }
      case _ => Redirect(routes.AgentCompanyNameController.get(index, edit))
    }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[BusinessStructure](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.business_structure(f, index, edit)))
        case ValidForm(_, data) =>
          for {
            result <- fetchAllAndUpdateStrict[TradingPremises](index) { (_, tp) =>
              resetAgentValues(tp.businessStructure(data), data)
            }
          } yield redirectToPage(data, edit, index, result)
      }
  }

  private def resetAgentValues(tp: TradingPremises, data: BusinessStructure): TradingPremises = data match {
    case UnincorporatedBody => tp.copy(agentName = None, agentCompanyDetails = None, agentPartnership = None, hasChanged = true)
    case _ => tp.businessStructure(data)
  }
}

