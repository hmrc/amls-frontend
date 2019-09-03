/*
 * Copyright 2019 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.BusinessActivities

import views.html.businessactivities._
import javax.inject.Inject
import utils.AuthAction
import utils.BooleanFormReadWrite._

import scala.concurrent.Future

class TransactionRecordController @Inject()(val authAction: AuthAction,
                                            val dataCacheConnector: DataCacheConnector) extends DefaultBaseController {

  val fieldName = "isRecorded"
  implicit val reader = formRule(fieldName, "error.required.ba.select.transaction.record")
  implicit val writer = formWrites(fieldName)

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
        response =>
          val form: Form2[Boolean] = (for {
            businessActivities <- response
            transactionRecord <- businessActivities.transactionRecord
          } yield Form2[Boolean](transactionRecord)).getOrElse(EmptyForm)

          Ok(customer_transaction_records(form, edit))
      }
  }

  def post(edit : Boolean = false) = authAction.async {
    implicit request =>
      Form2[Boolean](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(customer_transaction_records(f, edit)))
        case ValidForm(_, data) => {
          for {
            businessActivity <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key, businessActivity.transactionRecord(data))
          } yield (edit, data) match {
            case (false, true) => Redirect(routes.TransactionTypesController.get())
            case (false, false) => Redirect(routes.IdentifySuspiciousActivityController.get())
            case (true, true) if businessActivity.transactionRecordTypes.isEmpty => Redirect(routes.TransactionTypesController.get(edit))
            case _ => Redirect(routes.SummaryController.get())
          }
        }
      }
  }
}
