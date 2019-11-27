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

package controllers.responsiblepeople

import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.responsiblepeople.{PreviousName, ResponsiblePerson}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.legal_name_input

import scala.concurrent.Future

@Singleton
class LegalNameInputController @Inject()(val dataCacheConnector: DataCacheConnector,
                                         authAction: AuthAction) extends RepeatingSection with DefaultBaseController {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request =>
        getData[ResponsiblePerson](request.credId, index) map {
          case Some(ResponsiblePerson(Some(personName), Some(previous), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _))
          => Ok(legal_name_input(Form2[PreviousName](previous), edit, index, flow, personName.titleName))
          case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _))
          => Ok(legal_name_input(EmptyForm, edit, index, flow, personName.titleName))
          case _
          => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request => {
        Form2[PreviousName](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(views.html.responsiblepeople.legal_name_input(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                    rp.legalName(data)
                }
            } yield {
              Redirect(routes.LegalNameChangeDateController.get(index, edit, flow))
            }
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
      }
  }
}
