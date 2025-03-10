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
import forms.tradingpremises.AgentCompanyDetailsFormProvider
import models.tradingpremises._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, RepeatingSection}
import views.html.tradingpremises.AgentCompanyDetailsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AgentCompanyDetailsController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  override val messagesApi: MessagesApi,
  val cc: MessagesControllerComponents,
  formProvider: AgentCompanyDetailsFormProvider,
  view: AgentCompanyDetailsView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int, edit: Boolean = false): Action[AnyContent] =
    authAction.async { implicit request =>
      getData[TradingPremises](request.credId, index) map {
        case Some(tp) =>
          val form = tp.agentCompanyDetails match {
            case Some(data) => formProvider().fill(data)
            case None       => formProvider()
          }
          Ok(view(form, index, edit))
        case None     => NotFound(notFoundView)
      }
    }

  def post(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, index, edit))),
        data =>
          {
            for {
              result <- fetchAllAndUpdateStrict[TradingPremises](request.credId, index) { (_, tp) =>
                          TradingPremises(
                            tp.registeringAgentPremises,
                            tp.yourTradingPremises,
                            tp.businessStructure,
                            None,
                            Some(data),
                            None,
                            tp.whatDoesYourBusinessDoAtThisAddress,
                            tp.msbServices,
                            true,
                            tp.lineId,
                            tp.status,
                            tp.endDate
                          )
                        }
            } yield
              if (edit) {
                Redirect(routes.CheckYourAnswersController.get(index))
              } else {
                TPControllerHelper.redirectToNextPage(result, index, edit)
              }
          }.recoverWith { case _: IndexOutOfBoundsException =>
            Future.successful(NotFound(notFoundView))
          }
      )
  }
}
