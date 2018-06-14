/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.bankdetails._
import play.api.mvc.{Request, Result}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection, StatusConstants}

import scala.concurrent.Future

@Singleton
class BankAccountTypeController @Inject()(
                                           val authConnector: AuthConnector = AMLSAuthConnector,
                                           val dataCacheConnector: DataCacheConnector,
                                           implicit val statusService: StatusService
                                         ) extends BankDetailsController {

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        for {
          bankDetail <- getData[BankDetails](index)
          status <- statusService.getStatus
        } yield bankDetail match {
          case Some(details@BankDetails(Some(data), _, _, _, _, _, _)) if details.canEdit(status) =>
            Ok(view.apply(Form2[Option[BankAccountType]](Some(data)), edit, index))
          case Some(details) if details.canEdit(status) =>
            Ok(view.apply(EmptyForm, edit, index))
          case _ => NotFound(notFoundView)
        }
      }
  }

  def post(index: Int, edit: Boolean = false, count: Int = 0) = Authorised.async {
    implicit authContext =>
      implicit request => {
        Form2[Option[BankAccountType]](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(view(request)(f, edit, index)))
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[BankDetails](index) { bd =>
                data match {
                  case Some(NoBankAccountUsed) => bd.bankAccountType(data).bankAccount(None)
                  case _ => bd.bankAccountType(data)
                }
              }
            } yield router(data, edit, index)
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
      }
  }

  private def view(implicit request: Request[_]) = views.html.bankdetails.bank_account_types.apply _

  private val router = (data: Option[BankAccountType], edit: Boolean, index: Int) => data match {
    case Some(NoBankAccountUsed) => Redirect(routes.SummaryController.get(index))
    case Some(_) if !edit => Redirect(routes.BankAccountIsUKController.get(index))
    case _ => Redirect(routes.SummaryController.get(index))
  }

}