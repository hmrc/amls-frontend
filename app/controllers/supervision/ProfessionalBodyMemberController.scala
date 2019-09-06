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

package controllers.supervision

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.supervision.{ProfessionalBodyMember, ProfessionalBodyMemberNo, ProfessionalBodyMemberYes, Supervision}
import play.api.mvc.Result
import utils.AuthAction
import views.html.supervision.member_of_professional_body

import scala.concurrent.{ExecutionContext, Future}

class ProfessionalBodyMemberController @Inject()(
                                                  val dataCacheConnector: DataCacheConnector,
                                                  val authAction: AuthAction,
                                                  val ds: CommonPlayDependencies) extends AmlsBaseController(ds) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[Supervision](request.credId, Supervision.key) map {
        response =>
          val form: Form2[ProfessionalBodyMember] = (for {
            supervision <- response
            members <- supervision.professionalBodyMember
          } yield Form2[ProfessionalBodyMember](members)).getOrElse(EmptyForm)
          Ok(member_of_professional_body(form, edit))
      }
  }

  def post(edit : Boolean = false) = authAction.async {
    implicit request =>
      Form2[ProfessionalBodyMember](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(member_of_professional_body(f, edit)))
        case ValidForm(_, data) => {
          for {
            supervision <- dataCacheConnector.fetch[Supervision](request.credId, Supervision.key)
            updatedSupervision <- updateSupervisionFromIncomingData(data, supervision)
            _ <- dataCacheConnector.save[Supervision](request.credId, Supervision.key, updatedSupervision)
          } yield redirectTo(updatedSupervision, edit)
        }
      }
  }

  def updateSupervisionFromIncomingData(data: ProfessionalBodyMember, supervision: Option[Supervision])(implicit ec: ExecutionContext) = {
    Future[Supervision](data match {
      case ProfessionalBodyMemberNo => supervision.professionalBodyMember(data).copy(professionalBodies = None)
      case _ => supervision.professionalBodyMember(data)
    })
  }

  def redirectTo(supervision: Supervision, edit: Boolean): Result = {
    (supervision.professionalBodyMember, edit) match {
      case (Some(ProfessionalBodyMemberYes), _) if !supervision.isComplete => Redirect(routes.WhichProfessionalBodyController.get(edit))
      case (Some(ProfessionalBodyMemberNo), false) => Redirect(routes.PenalisedByProfessionalController.get())
      case _ => Redirect(routes.SummaryController.get())
    }
  }
}
