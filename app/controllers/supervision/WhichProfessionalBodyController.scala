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

package controllers.supervision

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.supervision.WhichProfessionalBodyFormProvider
import models.supervision.Supervision
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.supervision.WhichProfessionalBodyView

import javax.inject.Inject
import scala.concurrent.Future

class WhichProfessionalBodyController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: WhichProfessionalBodyFormProvider,
  view: WhichProfessionalBodyView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[Supervision](request.credId, Supervision.key) map { response =>
      val form = (for {
        supervision   <- response
        businessTypes <- supervision.professionalBodies
      } yield formProvider().fill(businessTypes)) getOrElse formProvider()

      Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          for {
            supervision <- dataCacheConnector.fetch[Supervision](request.credId, Supervision.key)
            _           <- dataCacheConnector
                             .save[Supervision](request.credId, Supervision.key, supervision.professionalBodies(Some(data)))
          } yield
            if (edit) {
              Redirect(routes.SummaryController.post())
            } else {
              Redirect(routes.PenalisedByProfessionalController.post(edit))
            }
      )
  }

}
