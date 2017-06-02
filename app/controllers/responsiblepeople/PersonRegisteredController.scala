/*
 * Copyright 2017 HM Revenue & Customs
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

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.{PersonRegistered, VATRegistered, ResponsiblePeople}
import utils.{StatusConstants, RepeatingSection}
import views.html.responsiblepeople._

import scala.concurrent.Future

trait PersonRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, flow: Option[String] = None) =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) map {
          case Some(data) =>
            val count = data.count(x => {
              !x.status.contains(StatusConstants.Deleted) &&
              x.personName.isDefined
            })
            Ok(person_registered(EmptyForm, count, flow))
          case _ => Ok(person_registered(EmptyForm, index, flow))
        }
  }

  def post(index: Int, flow: Option[String] = None) =
      Authorised.async {
        implicit authContext => implicit request =>
          Form2[PersonRegistered](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(person_registered(f, index, flow)))
            case ValidForm(_, data) =>
               data.registerAnotherPerson match {
                case true => Future.successful(Redirect(routes.ResponsiblePeopleAddController.get(false)))
                case false => Future.successful(Redirect(routes.SummaryController.get(flow)))
              }
          }
      }
}

object PersonRegisteredController extends PersonRegisteredController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
