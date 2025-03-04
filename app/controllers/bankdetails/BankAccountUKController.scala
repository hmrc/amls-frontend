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
import forms.bankdetails.BankAccountUKFormProvider
import models.bankdetails.{BankAccount, BankDetails, UKAccount}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AuthAction, StatusConstants}
import views.html.bankdetails.BankAccountUKView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class BankAccountUKController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val auditConnector: AuditConnector,
  val statusService: StatusService,
  val ds: CommonPlayDependencies,
  val mcc: MessagesControllerComponents,
  formProvider: BankAccountUKFormProvider,
  view: BankAccountUKView,
  implicit val error: views.html.ErrorView
) extends BankDetailsController(ds, mcc) {

  def get(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    for {
      bankDetails <- getData[BankDetails](request.credId, index)
      status      <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
    } yield bankDetails match {
      case Some(x @ BankDetails(_, _, Some(BankAccount(_, _, Some(data @ UKAccount(_, _)))), _, _, _, _))
          if x.canEdit(status) =>
        Ok(view(formProvider().fill(data), edit, index))
      case Some(x) if x.canEdit(status) =>
        Ok(view(formProvider(), edit, index))
      case _                            => NotFound(notFoundView)
    }
  }

  def post(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    {

      lazy val sendAudit = for {
        details <- OptionT(getData[BankDetails](request.credId, index))
        result  <- OptionT.liftF(auditConnector.sendEvent(audit.AddBankAccountEvent(details)))
      } yield result

      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit, index))),
          data =>
            updateDataStrict[BankDetails](request.credId, index) { bd =>
              bd.copy(
                bankAccount = Option(bd.bankAccount.getOrElse(BankAccount(None, None, None)).account(data)),
                status = Some(if (edit) {
                  StatusConstants.Updated
                } else {
                  StatusConstants.Added
                })
              )
            }.flatMap { _ =>
              if (edit) {
                Future.successful(Redirect(routes.SummaryController.get(index)))
              } else {
                lazy val redirect = Redirect(routes.SummaryController.get(index))
                (sendAudit map { _ =>
                  redirect
                }) getOrElse redirect
              }
            }
        )
    }.recoverWith { case _: IndexOutOfBoundsException =>
      Future.successful(NotFound(notFoundView))
    }
  }

}
