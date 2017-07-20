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

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.bankdetails.{BankDetails, NoBankAccountUsed}
import models.responsiblepeople.BankAccountRegistered
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import utils.StatusConstants

import scala.concurrent.Future

trait BankAccountRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int) =
    Authorised.async {
      implicit authContext =>
        implicit request => {

          val filter: BankDetails => Boolean = details =>
            details.bankAccountType.isDefined &&
              !details.bankAccountType.contains(NoBankAccountUsed) &&
              !details.status.contains(StatusConstants.Deleted)

          dataCacheConnector.fetch[Seq[BankDetails]](BankDetails.key) map {
            case Some(data) => Ok(views.html.bankdetails.bank_account_registered(EmptyForm, data.filter(filter).size))
            case _ => Ok(views.html.bankdetails.bank_account_registered(EmptyForm, index))
          }
        }
    }

  def post(index: Int) =
     Authorised.async {
        implicit authContext => implicit request =>
          Form2[BankAccountRegistered](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(views.html.bankdetails.bank_account_registered(f, index)))
            case ValidForm(_, data) =>
               data.registerAnotherBank match {
                case true => Future.successful(Redirect(routes.BankAccountAddController.get(false)))
                case false => Future.successful(Redirect(routes.SummaryController.get(false)))
              }
          }
      }
}

object BankAccountRegisteredController extends BankAccountRegisteredController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
