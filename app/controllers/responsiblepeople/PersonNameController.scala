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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import play.api.i18n.Messages
import play.api.mvc.Request
import utils.RepeatingSection
import views.html.responsiblepeople.person_name

import scala.concurrent.Future

trait PersonNameController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getData[ResponsiblePerson](index) map {
          case Some(ResponsiblePerson(Some(name),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(person_name(Form2[PersonName](name), edit, index, flow))
          case Some(ResponsiblePerson(_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(person_name(EmptyForm, edit, index, flow))
          case _
          => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request => {
        Form2[PersonName](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.responsiblepeople.person_name(f, edit, index, flow)))
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePerson](index) { rp =>
                rp.personName(data)
              }
            } yield edit match {
              case true => Redirect(routes.DetailedAnswersController.get(index, edit, flow))
              case false => Redirect(routes.LegalNameController.get(index, edit, flow))
            }
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
      }
  }

}

object PersonNameController extends PersonNameController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
