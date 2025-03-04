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
import forms.supervision.MemberOfProfessionalBodyFormProvider
import models.supervision.{ProfessionalBodyMember, ProfessionalBodyMemberNo, ProfessionalBodyMemberYes, Supervision}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.AuthAction
import views.html.supervision.MemberOfProfessionalBodyView

import javax.inject.Inject
import scala.concurrent.Future

class ProfessionalBodyMemberController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: MemberOfProfessionalBodyFormProvider,
  view: MemberOfProfessionalBodyView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[Supervision](request.credId, Supervision.key) map { response =>
      val form = (for {
        supervision <- response
        members     <- supervision.professionalBodyMember
      } yield formProvider().fill(members)).getOrElse(formProvider())
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
            updatedSupervision <- updateSupervisionFromIncomingData(data, supervision)
            _                  <- dataCacheConnector.save[Supervision](request.credId, Supervision.key, updatedSupervision)
          } yield redirectTo(updatedSupervision, edit)
      )
  }

  def updateSupervisionFromIncomingData(
    data: ProfessionalBodyMember,
    supervision: Option[Supervision]
  ): Future[Supervision] =
    Future[Supervision](data match {
      case ProfessionalBodyMemberNo => supervision.professionalBodyMember(data).copy(professionalBodies = None)
      case _                        => supervision.professionalBodyMember(data)
    })

  def redirectTo(supervision: Supervision, edit: Boolean): Result =
    (supervision.professionalBodyMember, edit) match {
      case (Some(ProfessionalBodyMemberYes), _) if !supervision.isComplete =>
        Redirect(routes.WhichProfessionalBodyController.get(edit))
      case (Some(ProfessionalBodyMemberNo), false)                         => Redirect(routes.PenalisedByProfessionalController.get())
      case _                                                               => Redirect(routes.SummaryController.get())
    }
}
