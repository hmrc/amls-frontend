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
import forms.supervision.PenaltyDetailsFormProvider
import models.supervision.{ProfessionalBodyNo, ProfessionalBodyYes, Supervision}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import utils.CharacterCountParser.cleanData
import views.html.supervision.PenaltyDetailsView

import javax.inject.Inject
import scala.concurrent.Future

class PenaltyDetailsController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: PenaltyDetailsFormProvider,
  view: PenaltyDetailsView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[Supervision](request.credId, Supervision.key) map { response =>
      response.professionalBody match {
        case Some(value @ ProfessionalBodyYes(_)) =>
          Ok(view(formProvider().fill(value), edit))
        case Some(ProfessionalBodyNo)             =>
          Redirect(routes.PenalisedByProfessionalController.get(edit))
        case None                                 =>
          Ok(view(formProvider(), edit))
      }
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest(cleanData(request.body, "professionalBody"))
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          for {
            supervision <- dataCacheConnector.fetch[Supervision](request.credId, Supervision.key)
            _           <-
              dataCacheConnector.save[Supervision](request.credId, Supervision.key, supervision.professionalBody(data))
          } yield Redirect(routes.SummaryController.get())
      )
  }
}
