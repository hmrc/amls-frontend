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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.CommonPlayDependencies
import forms.bankdetails.BankAccountIsUKFormProvider
import models.bankdetails.{BankAccount, BankDetails}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AuthAction, StatusConstants}
import views.html.bankdetails.BankAccountIsUKView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class BankAccountIsUKController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val auditConnector: AuditConnector,
  val statusService: StatusService,
  val mcc: MessagesControllerComponents,
  formProvider: BankAccountIsUKFormProvider,
  view: BankAccountIsUKView,
  implicit val error: views.html.ErrorView
) extends BankDetailsController(ds, mcc) {

  def get(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    for {
      bankDetails <- getData[BankDetails](request.credId, index)
      status      <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
    } yield bankDetails match {
      case Some(x @ BankDetails(_, _, Some(data), _, _, _, _)) if x.canEdit(status) =>
        val form = data.isUk.fold(formProvider())(formProvider().fill)
        Ok(view(form, edit, index))
      case Some(x) if x.canEdit(status)                                             =>
        Ok(view(formProvider(), edit, index))
      case _                                                                        => NotFound(notFoundView)
    }
  }

  def post(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    {

      for {
        details <- OptionT(getData[BankDetails](request.credId, index))
        result  <- OptionT.liftF(auditConnector.sendEvent(audit.AddBankAccountEvent(details)))
      } yield result

      formProvider()
        .bindFromRequest()
        .fold(
          formWithError => Future.successful(BadRequest(view(formWithError, edit, index))),
          data =>
            updateDataStrict[BankDetails](request.credId, index) { bd =>
              bd.copy(
                bankAccount = Option(bd.bankAccount.getOrElse(BankAccount(None, None, None)).isUk(data)),
                status = Some(if (edit) {
                  StatusConstants.Updated
                } else {
                  StatusConstants.Added
                })
              )
            }.map { _ =>
              if (data.isUk) {
                Redirect(routes.BankAccountUKController.get(index))
              } else {
                Redirect(routes.BankAccountHasIbanController.get(index))
              }
            }
        )
    }.recoverWith { case _: IndexOutOfBoundsException =>
      Future.successful(NotFound(notFoundView))
    }
  }

}
