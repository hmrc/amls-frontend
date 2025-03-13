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

package controllers.declaration

import javax.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.declaration.RenewRegistrationFormProvider
import models.declaration.{RenewRegistration, RenewRegistrationNo, RenewRegistrationYes}
import services.{ProgressService, RenewalService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, DeclarationHelper}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.declaration.RenewRegistrationView

import scala.concurrent.Future

class RenewRegistrationController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val progressService: ProgressService,
  implicit val statusService: StatusService,
  implicit val renewalService: RenewalService,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: RenewRegistrationFormProvider,
  view: RenewRegistrationView
) extends AmlsBaseController(ds, cc) {

  def get(): Action[AnyContent] = authAction.async { implicit request =>
    DeclarationHelper.statusEndDate(request.amlsRefNumber, request.accountTypeId, request.credId) flatMap {
      maybeEndDate =>
        dataCacheConnector.fetch[RenewRegistration](request.credId, RenewRegistration.key) map { renewOpt =>
          val form = renewOpt.fold(formProvider())(formProvider().fill)
          Ok(view(form, maybeEndDate))
        }

    }
  }

  def post(): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          DeclarationHelper.statusEndDate(request.amlsRefNumber, request.accountTypeId, request.credId) flatMap {
            maybeEndDate =>
              Future.successful(BadRequest(view(formWithErrors, maybeEndDate)))
          },
        data =>
          dataCacheConnector.save[RenewRegistration](request.credId, RenewRegistration.key, data).flatMap { _ =>
            redirectDependingOnResponse(data, request.amlsRefNumber, request.accountTypeId, request.credId)
          }
      )
  }

  private def redirectDependingOnResponse(
    data: RenewRegistration,
    amlsRefNo: Option[String],
    accountTypeId: (String, String),
    credId: String
  )(implicit hc: HeaderCarrier) = data match {
    case RenewRegistrationYes => Future.successful(Redirect(controllers.renewal.routes.WhatYouNeedController.get))
    case RenewRegistrationNo  => resolveDeclarationDest(amlsRefNo, accountTypeId, credId)
  }

  private def resolveDeclarationDest(amlsRefNo: Option[String], accountTypeId: (String, String), credId: String)(
    implicit hc: HeaderCarrier
  ) =
    progressService.getSubmitRedirect(amlsRefNo, accountTypeId, credId) map {
      case Some(url) => Redirect(url)
      case _         => InternalServerError("Could not get data for redirect")
    }
}
