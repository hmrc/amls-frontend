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
import forms.supervision.AnotherBodyFormProvider
import models.supervision.{AnotherBody, AnotherBodyNo, AnotherBodyYes, Supervision}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.AuthAction
import views.html.supervision.AnotherBodyView

import javax.inject.Inject
import scala.concurrent.Future

class AnotherBodyController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: AnotherBodyFormProvider,
  view: AnotherBodyView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[Supervision](request.credId, Supervision.key) map { response =>
      val form = (for {
        supervision <- response
        anotherBody <- supervision.anotherBody
      } yield formProvider().fill(anotherBody)).getOrElse(formProvider())
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
            supervision        <- dataCacheConnector.fetch[Supervision](request.credId, Supervision.key)
            _                  <- dataCacheConnector.save[Supervision](request.credId, Supervision.key, updateData(supervision, data))
            updatedSupervision <- dataCacheConnector.fetch[Supervision](request.credId, Supervision.key)
          } yield redirectTo(edit, updatedSupervision)
      )
  }

  private def updateData(supervision: Supervision, data: AnotherBody): Supervision = {
    def updatedAnotherBody = (supervision.anotherBody, data) match {
      case (_, d) if d.equals(AnotherBodyNo)                         => AnotherBodyNo
      case (Some(ab), d: AnotherBodyYes) if ab.equals(AnotherBodyNo) =>
        AnotherBodyYes(d.supervisorName, None, None, None)
      case (Some(ab), d: AnotherBodyYes)                             => ab.asInstanceOf[AnotherBodyYes].supervisorName(d.supervisorName)
      case (None, d: AnotherBodyYes)                                 => AnotherBodyYes(d.supervisorName, None, None, None)

    }

    supervision.anotherBody(updatedAnotherBody).copy(hasAccepted = true)
  }

  private def redirectTo(edit: Boolean, maybeSupervision: Option[Supervision]): Result = {

    import utils.ControllerHelper.{anotherBodyComplete, isAnotherBodyYes}

    maybeSupervision match {
      case Some(supervision) =>
        val anotherBody = anotherBodyComplete(supervision)

        supervision.isComplete match {
          case false if isAnotherBodyYes(anotherBody) => Redirect(routes.SupervisionStartController.get())
          case false                                  => Redirect(routes.ProfessionalBodyMemberController.get())
          case true                                   => Redirect(routes.SummaryController.get())
        }
      case _                 => InternalServerError("Could not fetch the data")
    }
  }
}
