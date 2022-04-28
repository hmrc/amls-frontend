/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Write}
import models.FormTypes
import models.bankdetails.BankDetails
import play.api.mvc._
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, StatusConstants}
import views.html.bankdetails.bank_account_name

import scala.concurrent.Future

@Singleton
class BankAccountNameController @Inject()(
                                           val authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           val dataCacheConnector: DataCacheConnector,
                                           val statusService: StatusService,
                                           val mcc: MessagesControllerComponents,
                                           bank_account_name: bank_account_name,
                                           implicit val error: views.html.error) extends BankDetailsController(ds, mcc) {

  implicit def write: Write[String, UrlFormEncoded] = Write { data =>
    Map("accountName" -> Seq(data))
  }

  implicit val read = From[UrlFormEncoded] { __ =>
    (__ \ "accountName").read(FormTypes.accountNameType)
  }

  def getNoIndex: Action[AnyContent] = authAction.async {
    implicit request =>
      request.request.session.get("itemIndex") match {
        case Some(idx) => Future.successful(Redirect(controllers.bankdetails.routes.BankAccountNameController.getIndex(idx.toInt)))
        case _ => handleGet(None, false, request.amlsRefNumber, request.accountTypeId, request.credId)
      }
  }

  def getIndex(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      handleGet(Some(index), edit, request.amlsRefNumber, request.accountTypeId, request.credId)
  }

  def postNoIndex: Action[AnyContent] = authAction.async {
    implicit request =>
      handlePost(request.credId)
  }

  def postIndex(index: Int, edit: Boolean = false) = authAction.async {
    implicit request =>
      handlePost(request.credId, Some(index), edit)
  }

  private def handleGet(index: Option[Int], edit: Boolean, amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String)
                       (implicit hc: HeaderCarrier, request: Request[_]): Future[Result] = {
    index match {
      case Some(i) => for {
        status <- statusService.getStatus(amlsRegistrationNo, accountTypeId, credId)
        data <- getData[BankDetails](credId, i)
      } yield data match {
        case Some(x) if !x.canEdit(status) => NotFound(notFoundView)
        case Some(BankDetails(_, Some(name), _, _, _, _, _)) =>
          Ok(bank_account_name(Form2[String](name), edit, Some(i)))
        case Some(_) =>
          Ok(bank_account_name(EmptyForm, edit, Some(i)))
      }

      case _ => Future.successful(Ok(bank_account_name(EmptyForm, edit, None)))
    }
  }

  private def handlePost(credId: String, index: Option[Int] = None, edit: Boolean = false)
                        (implicit hc: HeaderCarrier, request: Request[AnyContent]) = {
    Form2[String](request.body) match {
      case f: InvalidForm =>
        Future.successful(BadRequest(bank_account_name(f, edit, index)))
      case ValidForm(_, data) =>
        val newBankDetails = BankDetails(accountName = Some(data))
        index match {
          case Some(i) => updateDataStrict[BankDetails](credId, i) { bd =>
            bd.copy(
              accountName = Some(data),
              status = Some(if (edit) {
                StatusConstants.Updated
              } else {
                StatusConstants.Added
              })
            )
          } map { _ =>
            if (edit) {
              Redirect(routes.SummaryController.get(i))
            } else {
              Redirect(routes.BankAccountTypeController.get(i))
            }
          }
          case _ => dataCacheConnector.fetch[Seq[BankDetails]](credId, BankDetails.key) flatMap { maybeBankDetails =>
            val newList = maybeBankDetails.getOrElse(Seq.empty) ++ Seq(newBankDetails.copy(status = Some(StatusConstants.Added)))
            dataCacheConnector.save(credId, BankDetails.key, newList) map { _ =>
              val redirect = Redirect(routes.BankAccountTypeController.get(newList.size))
              redirect.addingToSession("itemIndex" -> newList.size.toString)
            }
          }
        }
    }
  } recoverWith {
    case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
  }
}