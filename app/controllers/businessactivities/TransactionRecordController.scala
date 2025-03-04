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

package controllers.businessactivities

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessactivities.TransactionRecordFormProvider
import models.businessactivities.BusinessActivities
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.businessactivities.CustomerTransactionRecordsView

import javax.inject.Inject
import scala.concurrent.Future

class TransactionRecordController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val cc: MessagesControllerComponents,
  formProvider: TransactionRecordFormProvider,
  view: CustomerTransactionRecordsView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map { response =>
      val form: Form[Boolean] = (for {
        businessActivities <- response
        transactionRecord  <- businessActivities.transactionRecord
      } yield formProvider().fill(transactionRecord)).getOrElse(formProvider())
      Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithError => Future.successful(BadRequest(view(formWithError, edit))),
        data =>
          for {
            businessActivity <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _                <- dataCacheConnector.save[BusinessActivities](
                                  request.credId,
                                  BusinessActivities.key,
                                  businessActivity.transactionRecord(data)
                                )
          } yield (edit, data) match {
            case (false, true)                                                   => Redirect(routes.TransactionTypesController.get())
            case (false, false)                                                  => Redirect(routes.IdentifySuspiciousActivityController.get())
            case (true, true) if businessActivity.transactionRecordTypes.isEmpty =>
              Redirect(routes.TransactionTypesController.get(edit))
            case _                                                               => Redirect(routes.SummaryController.get)
          }
      )
  }
}
