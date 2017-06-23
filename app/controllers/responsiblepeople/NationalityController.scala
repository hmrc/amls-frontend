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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{Nationality, ResponsiblePeople}
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.nationality

import scala.concurrent.Future

trait NationalityController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(Some(personName),Some(residencyType),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
            => residencyType.nationality match {
                case Some(country) => Ok(nationality(Form2[Nationality](country), edit, index, fromDeclaration, personName.titleName))
                case _ => Ok(nationality(EmptyForm, edit, index, fromDeclaration, personName.titleName))
              }
            case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
            => Ok(nationality(EmptyForm, edit, index, fromDeclaration, personName.titleName))
            case _
            => NotFound(notFoundView)
          }
      }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
      Authorised.async {
        implicit authContext => implicit request =>

          Form2[Nationality](request.body) match {
            case f: InvalidForm =>
              getData[ResponsiblePeople](index) map {rp =>
                BadRequest(nationality(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
              }
            case ValidForm(_, data) => {
              for {
                _ <- updateDataStrict[ResponsiblePeople](index) { rp =>
                  val residenceType = rp.personResidenceType.map(x => x.copy(nationality = Some(data)))
                  rp.personResidenceType(residenceType)
                }
              } yield edit match {
                case true => Redirect(routes.DetailedAnswersController.get(index))
                case false => Redirect(routes.ContactDetailsController.get(index, edit, fromDeclaration))
              }
            }.recoverWith {
              case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
            }
          }
    }
}

object NationalityController extends NationalityController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector

  override def dataCacheConnector = DataCacheConnector
}

