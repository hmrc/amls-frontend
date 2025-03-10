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

package controllers.bankdetails

import connectors.DataCacheConnector
import controllers.CommonPlayDependencies
import forms.bankdetails.BankAccountTypeFormProvider
import models.bankdetails.BankAccountType._
import models.bankdetails._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import utils.AuthAction
import views.html.bankdetails.BankAccountTypesView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class BankAccountTypeController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val statusService: StatusService,
  val mcc: MessagesControllerComponents,
  formProvider: BankAccountTypeFormProvider,
  view: BankAccountTypesView,
  implicit val error: views.html.ErrorView
) extends BankDetailsController(ds, mcc) {

  def get(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    for {
      bankDetail <- getData[BankDetails](request.credId, index)
      status     <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
    } yield bankDetail match {
      case Some(details @ BankDetails(Some(data), _, _, _, _, _, _)) if details.canEdit(status) =>
        Ok(view(formProvider().fill(data), edit, index))
      case Some(details) if details.canEdit(status)                                             =>
        Ok(view(formProvider(), edit, index))
      case _                                                                                    => NotFound(notFoundView)
    }
  }

  def post(index: Int, edit: Boolean = false, count: Int = 0): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit, index))),
          data =>
            {
              for {
                _ <- updateDataStrict[BankDetails](request.credId, index) { bd =>
                       data match {
                         case NoBankAccountUsed => bd.bankAccountType(Some(data)).bankAccount(None)
                         case _                 => bd.bankAccountType(Some(data))
                       }
                     }
              } yield router(data, edit, index)
            }.recoverWith { case _: IndexOutOfBoundsException =>
              Future.successful(NotFound(notFoundView))
            }
        )
  }

  private val router = (data: BankAccountType, edit: Boolean, index: Int) =>
    data match {
      case NoBankAccountUsed                                                     => Redirect(routes.SummaryController.get(index))
      case PersonalAccount | BelongsToBusiness | BelongsToOtherBusiness if !edit =>
        Redirect(routes.BankAccountIsUKController.get(index))
      case _                                                                     => Redirect(routes.SummaryController.get(index))
    }

}
