/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.responsiblepeople.{KnownBy, ResponsiblePerson}
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.known_by

import scala.concurrent.Future


@Singleton
class KnownByController @Inject()(val dataCacheConnector: DataCacheConnector,
                                  authAction: AuthAction,
                                  val ds: CommonPlayDependencies,
                                  val cc: MessagesControllerComponents,
                                  known_by: known_by,
                                  implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request =>
        getData[ResponsiblePerson](request.credId, index) map {
          case Some(ResponsiblePerson(Some(personName), _, _, Some(otherName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _))
          => Ok(known_by(Form2[KnownBy](otherName), edit, index, flow, personName.titleName))
          case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _))
          => Ok(known_by(EmptyForm, edit, index, flow, personName.titleName))
          case _
          => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request => {
        Form2[KnownBy](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(known_by(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              _ <- {
                data.hasOtherNames match {
                  case Some(true) => updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                    rp.knownBy(data)
                  }
                  case Some(false) => updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                    rp.knownBy(KnownBy(Some(false), None))
                  }
                }
              }
            } yield edit match {
        case true => Redirect (routes.DetailedAnswersController.get (index, flow))
        case false => Redirect (routes.DateOfBirthController.get (index, edit, flow))
        }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }
}

}
