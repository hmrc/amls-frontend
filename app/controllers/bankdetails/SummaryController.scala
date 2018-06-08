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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import javax.inject.{Inject, Singleton}
import models.bankdetails.BankDetails
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

@Singleton
class SummaryController @Inject()(
                                   val dataCacheConnector: DataCacheConnector,
                                   val authConnector: AuthConnector = AMLSAuthConnector
                                 ) extends BaseController with RepeatingSection {

  def get(index: Int) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        bankDetails <- getData[BankDetails](index)
      } yield bankDetails match {
        case Some(data) =>
          Ok(views.html.bankdetails.summary(data, index))
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }

  def post(index: Int) = Authorised.async {
    implicit authContext => implicit request =>
      (for {
        _ <- updateDataStrict[BankDetails](index) { bd => bd.copy(hasAccepted = true) }
      } yield Redirect(controllers.bankdetails.routes.YourBankAccountsController.get())) recoverWith {
        case _: Throwable => Future.successful(InternalServerError("Unable to save data and get redirect link"))
      }
  }

  private def hasBankAccount(bankDetails: Seq[BankDetails]): Boolean = {
    bankDetails.exists(_.bankAccount.isDefined)
  }
}