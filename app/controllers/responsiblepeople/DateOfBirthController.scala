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

package controllers.responsiblepeople

import javax.inject.Inject
import cats.data.OptionT
import config.AppConfig
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{DateOfBirth, ResponsiblePerson}
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.date_of_birth

import scala.concurrent.Future

class DateOfBirthController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       val dataCacheConnector: DataCacheConnector,
                                       val authConnector: AuthConnector,
                                       val appConfig: AppConfig
                                     ) extends RepeatingSection with BaseController {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getData[ResponsiblePerson](index) map {
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,Some(dateOfBirth),_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
            Ok(date_of_birth(Form2[DateOfBirth](dateOfBirth), edit, index, flow, personName.titleName))
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
            Ok(date_of_birth(EmptyForm, edit, index, flow, personName.titleName))
          case _ => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[DateOfBirth](request.body) match {
          case f: InvalidForm => getData[ResponsiblePerson](index) map { rp =>
            BadRequest(date_of_birth(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
          }
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePerson](index) { rp =>
                rp.dateOfBirth(data)
              }
            } yield edit match {
              case true => Redirect(routes.DetailedAnswersController.get(index, flow))
              case false if appConfig.phase2ChangesToggle => Redirect(routes.PersonResidentTypeController.get(index, edit, flow))
              case false => Redirect(routes.CountryOfBirthController.get(index, edit, flow))
            }

          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
  }
}