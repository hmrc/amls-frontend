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

package controllers.tradingpremises

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.tradingpremises.BusinessStructureFormProvider
import models.tradingpremises.BusinessStructure._
import models.tradingpremises._
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.cache.Cache
import utils.{AuthAction, RepeatingSection}
import views.html.tradingpremises.BusinessStructureView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class BusinessStructureController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  override val messagesApi: MessagesApi,
  val cc: MessagesControllerComponents,
  formProvider: BusinessStructureFormProvider,
  view: BusinessStructureView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    getData[TradingPremises](request.credId, index) map { response =>
      val form = (for {
        tp       <- response
        services <- tp.businessStructure
      } yield formProvider().fill(services)).getOrElse(formProvider())

      Ok(view(form, index, edit))
    }
  }

  private def redirectToPage(
    data: BusinessStructure,
    edit: Boolean,
    index: Int,
    result: Option[Cache]
  )(implicit request: Request[AnyContent]): Result =
    data match {
      case SoleProprietor                                 => Redirect(routes.AgentNameController.get(index, edit))
      case LimitedLiabilityPartnership | IncorporatedBody =>
        Redirect(routes.AgentCompanyDetailsController.get(index, edit))
      case Partnership                                    => Redirect(routes.AgentPartnershipController.get(index, edit))
      case UnincorporatedBody if edit                     => Redirect(routes.CheckYourAnswersController.get(index))
      case _                                              => TPControllerHelper.redirectToNextPage(result, index, edit)
    }

  def post(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithError => Future.successful(BadRequest(view(formWithError, index, edit))),
        data =>
          for {
            result <- fetchAllAndUpdateStrict[TradingPremises](request.credId, index) { (_, tp) =>
                        resetAgentValues(tp.businessStructure(data), data)
                      }
          } yield redirectToPage(data, edit, index, result)
      )
  }

  private def resetAgentValues(tp: TradingPremises, data: BusinessStructure): TradingPremises = data match {
    case UnincorporatedBody =>
      tp.copy(agentName = None, agentCompanyDetails = None, agentPartnership = None, hasChanged = true)
    case _                  => tp.businessStructure(data)
  }
}
