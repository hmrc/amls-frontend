/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.supervision

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.EmptyForm
import javax.inject.Inject
import models.supervision.Supervision
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction

import utils.ControllerHelper
import views.html.supervision.summary

class SummaryController  @Inject() (val dataCacheConnector: DataCacheConnector,
                                    val authAction: AuthAction,
                                    val ds: CommonPlayDependencies,
                                    val cc: MessagesControllerComponents,
                                    val summary: summary,
                                    implicit val error: views.html.error) extends AmlsBaseController(ds, cc) {

  def get() = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[Supervision](request.credId, Supervision.key) map {
        case Some(data@Supervision(Some(anotherBody), Some(_), _, Some(_), _, _)) if ControllerHelper.isAbComplete(anotherBody) =>
          Ok(summary(EmptyForm, data))
        case _ =>
          Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }

  def post = authAction.async {
    implicit request => (for {
      supervision <- OptionT(dataCacheConnector.fetch[Supervision](request.credId, Supervision.key))
      _ <- OptionT.liftF(dataCacheConnector.save[Supervision](request.credId, Supervision.key, supervision.copy(hasAccepted = true)))
    } yield Redirect(controllers.routes.RegistrationProgressController.get)) getOrElse InternalServerError("Could not update supervision")
  }
}
