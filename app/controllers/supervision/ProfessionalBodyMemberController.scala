/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.supervision.{ProfessionalBodyMember, ProfessionalBodyMemberNo, ProfessionalBodyMemberYes, Supervision}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.supervision.member_of_professional_body

import scala.concurrent.Future

class ProfessionalBodyMemberController @Inject()(
                                                  val dataCacheConnector: DataCacheConnector,
                                                  val authConnector: AuthConnector = AMLSAuthConnector
                                                ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Supervision](Supervision.key) map {
        response =>
          val form: Form2[ProfessionalBodyMember] = (for {
            supervision <- response
            members <- supervision.professionalBodyMember
          } yield Form2[ProfessionalBodyMember](members)).getOrElse(EmptyForm)
          Ok(member_of_professional_body(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ProfessionalBodyMember](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(member_of_professional_body(f, edit)))
        case ValidForm(_, data) => {
          for {
            supervision <- dataCacheConnector.fetch[Supervision](Supervision.key)
            _ <- dataCacheConnector.save[Supervision](Supervision.key, {
              data match {
                case ProfessionalBodyMemberNo
                  if supervision.professionalBodyMember.contains(ProfessionalBodyMemberYes) | supervision.professionalBodies.isDefined =>
                  supervision.professionalBodyMember(data).copy(professionalBodies = None)
                case _ => supervision.professionalBodyMember(data)
              }}
            )
          } yield redirectTo(data, supervision, edit)
        }
      }
  }

  def redirectTo(data: ProfessionalBodyMember, supervision: Supervision, edit: Boolean) =
    (data, edit) match {
      case (ProfessionalBodyMemberYes, _) if !supervision.professionalBodyMember.contains(ProfessionalBodyMemberYes) =>
        Redirect(routes.WhichProfessionalBodyController.get(edit))
      case (ProfessionalBodyMemberNo, false) => Redirect(routes.PenalisedByProfessionalController.get())
      case _ => Redirect(routes.SummaryController.get())
    }
}