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

package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities._
import javax.inject.Inject
import utils.BooleanFormReadWrite._
import scala.concurrent.Future

class TransactionRecordController @Inject()
(
  val authConnector: AuthConnector,
  val dataCacheConnector: DataCacheConnector
) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form: Form2[Boolean] = (for {
            businessActivities <- response
            transactionRecord <- businessActivities.transactionRecord
          } yield Form2[Boolean](transactionRecord)(formWrites("isRecorded"))).getOrElse(EmptyForm)

          Ok(customer_transaction_records(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[Boolean](request.body)(formRule("isRecorded", "error.required.ba.select.transaction.record")) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(customer_transaction_records(f, edit)))
        case ValidForm(_, data) => {
          for {
            businessActivity <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key, businessActivity.transactionRecord(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.IdentifySuspiciousActivityController.get())
          }
        }
      }
  }
}
