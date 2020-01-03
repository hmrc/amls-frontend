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

package controllers.bankdetails

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.CommonPlayDependencies
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.bankdetails.{BankAccount, BankAccountHasIban, BankDetails}
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AuthAction, StatusConstants}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class BankAccountHasIbanController @Inject()( val dataCacheConnector: DataCacheConnector,
                                              val authAction: AuthAction,
                                              val auditConnector: AuditConnector,
                                              val statusService: StatusService,
                                              val ds: CommonPlayDependencies,
                                              val mcc: MessagesControllerComponents) extends BankDetailsController(ds, mcc) {

  def get(index: Int, edit: Boolean = false) = authAction.async{
      implicit request =>
        for {
          bankDetails <- getData[BankDetails](request.credId, index)
          status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
        } yield bankDetails match {
          case Some(x@BankDetails(_, _, Some(BankAccount(_, Some(data@BankAccountHasIban(_)), _)), _, _, _, _)) if x.canEdit(status) =>
            Ok(views.html.bankdetails.bank_account_account_has_iban(Form2[BankAccountHasIban](data), edit, index))
          case Some(x) if x.canEdit(status) =>
            Ok(views.html.bankdetails.bank_account_account_has_iban(EmptyForm, edit, index))
          case _ => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false) = authAction.async {
      implicit request => {

        lazy val sendAudit = for {
          details <- OptionT(getData[BankDetails](request.credId, index))
          result <- OptionT.liftF(auditConnector.sendEvent(audit.AddBankAccountEvent(details)))
        } yield result

        Form2[BankAccountHasIban](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.bankdetails.bank_account_account_has_iban(f, edit, index)))
          case ValidForm(_, data) =>
            updateDataStrict[BankDetails](request.credId, index) { bd =>
              bd.copy(
                bankAccount = Option(bd.bankAccount.getOrElse(BankAccount(None, None, None)).hasIban(data)),
                status = Some(if (edit) {
                  StatusConstants.Updated
                } else {
                  StatusConstants.Added
                })
              )
            }.map { _ =>
                if(data.hasIban) {
                  Redirect(routes.BankAccountIbanController.get(index))
                } else {
                  Redirect(routes.BankAccountNonUKController.get(index))
                }
            }
        }
      }.recoverWith {
        case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
      }
  }
}
