/*
 * Copyright 2026 HM Revenue & Customs
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
import forms.tradingpremises.AgentCRNFormProvider
import models.tradingpremises.*
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, RepeatingSection}
import views.html.tradingpremises.AgentCRNView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AgentCRNController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  override val messagesApi: MessagesApi,
  val cc: MessagesControllerComponents,
  formProvider: AgentCRNFormProvider,
  view: AgentCRNView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int, edit: Boolean = false): Action[AnyContent] =
    authAction.async { implicit request =>
      getData[TradingPremises](request.credId, index).map {
        case Some(tp) =>
          val form =
            tp.agentCompanyDetails
              .flatMap(_.companyRegistrationNumber) match {
              case Some(crn) =>
                formProvider().fill(crn)

              case None =>
                formProvider()
            }

          Ok(view(form, index, edit))

        case None =>
          NotFound(notFoundView)
      }
    }

  def post(index: Int, edit: Boolean = false): Action[AnyContent] =
    authAction.async { implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, index, edit))
            ),
          companyRegistrationNumber =>
            fetchAllAndUpdateStrict[TradingPremises](
              request.credId,
              index
            ) { (_, tp) =>

              val existingDetails =
                tp.agentCompanyDetails.getOrElse {
                  throw new IllegalStateException(
                    s"Agent company details missing for trading premises index $index"
                  )
                }

              val updatedAgentCompanyDetails =
                existingDetails.copy(
                  companyRegistrationNumber = Some(companyRegistrationNumber)
                )

              tp.copy(
                agentCompanyDetails = Some(updatedAgentCompanyDetails)
              )
            }.map { result =>
              if (edit) {
                Redirect(
                  routes.CheckYourAnswersController.get(index)
                )
              } else {
                TPControllerHelper.redirectToNextPage(
                  result,
                  index,
                  edit
                )
              }
            }.recover { case _: IndexOutOfBoundsException =>
              NotFound(notFoundView)
            }
        )
    }
}
