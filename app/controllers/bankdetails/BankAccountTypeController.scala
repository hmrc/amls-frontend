/*
 * Copyright 2017 HM Revenue & Customs
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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.bankdetails.{BankAccountType, BankDetails}
import models.status.{NotCompleted, SubmissionReady}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{ControllerHelper, RepeatingSection, StatusConstants}

import scala.concurrent.Future

trait BankAccountTypeController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector
  implicit val statusService: StatusService

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        bankDetail <- getData[BankDetails](index)
        count <- getData[BankDetails].map(x => x.count(!_.status.contains(StatusConstants.Deleted)))
        allowedToEdit <- ControllerHelper.allowedToEdit(edit)
      } yield bankDetail match {
        case Some(BankDetails(Some(data), _, _,_,_)) if allowedToEdit =>
          Ok(views.html.bankdetails.bank_account_types(Form2[Option[BankAccountType]](Some(data)), edit, index, count))
        case Some(_) if allowedToEdit =>
          Ok(views.html.bankdetails.bank_account_types(EmptyForm, edit, index, count))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, count: Int = 0) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[Option[BankAccountType]](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.bankdetails.bank_account_types(f, edit, index, count)))
        case ValidForm(_, data) => {
          for {
            result <- updateDataStrict[BankDetails](index) { bd =>
              bd.bankAccountType(data)
            }
          } yield {
            data match {
              case Some(_) => Redirect(routes.BankAccountController.get(index, edit))
              case _ => Redirect(routes.SummaryController.get(false))
            }
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
    }
  }

}

object BankAccountTypeController extends BankAccountTypeController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override implicit val statusService = StatusService
}
