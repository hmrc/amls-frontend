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

import javax.inject.Inject
import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessactivities.TransactionTypesFormProvider
import models.businessactivities.{BusinessActivities, TransactionTypes}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.businessactivities.TransactionTypesView

import scala.concurrent.Future

class TransactionTypesController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cacheConnector: DataCacheConnector,
  val cc: MessagesControllerComponents,
  formProvider: TransactionTypesFormProvider,
  view: TransactionTypesView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    {
      def form(ba: BusinessActivities): Form[TransactionTypes] =
        ba.transactionRecordTypes.fold(formProvider())(formProvider().fill)

      for {
        ba <- OptionT(cacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key))
      } yield Ok(view(form(ba), edit))
    } getOrElse InternalServerError("Cannot fetch business activities")
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    lazy val redirect = Redirect(if (edit) {
      routes.SummaryController.get
    } else {
      routes.IdentifySuspiciousActivityController.get()
    })

    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          {
            for {
              bm <- OptionT(cacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key))
              _  <-
                OptionT.liftF(
                  cacheConnector
                    .save[BusinessActivities](request.credId, BusinessActivities.key, bm.transactionRecordTypes(data))
                )
            } yield redirect
          } getOrElse InternalServerError("Unable to update Business Activities Transaction Types")
      )
  }
}
