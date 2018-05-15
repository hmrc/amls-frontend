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
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{Nationality, ResponsiblePerson}
import services.AutoCompleteService
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.nationality
import play.api.Play

import scala.concurrent.Future

trait NationalityController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  val autoCompleteService: AutoCompleteService

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePerson](index) map {
            case Some(ResponsiblePerson(Some(personName),_,_,_,Some(residencyType),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
            => residencyType.nationality match {
                case Some(country) => Ok(nationality(Form2[Nationality](country), edit, index, flow, personName.titleName, autoCompleteService.getCountries))
                case _ => Ok(nationality(EmptyForm, edit, index, flow, personName.titleName, autoCompleteService.getCountries))
              }
            case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
            => Ok(nationality(EmptyForm, edit, index, flow, personName.titleName, autoCompleteService.getCountries))
            case _
            => NotFound(notFoundView)
          }
      }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
      Authorised.async {
        implicit authContext => implicit request =>

          Form2[Nationality](request.body) match {
            case f: InvalidForm =>
              getData[ResponsiblePerson](index) map { rp =>
                BadRequest(nationality(f, edit, index, flow, ControllerHelper.rpTitleName(rp), autoCompleteService.getCountries))
              }
            case ValidForm(_, data) => {
              for {
                _ <- updateDataStrict[ResponsiblePerson](index) { rp =>
                  val residenceType = rp.personResidenceType.map(x => x.copy(nationality = Some(data)))
                  rp.personResidenceType(residenceType)
                }
              } yield edit match {
                case true => Redirect(routes.DetailedAnswersController.get(index, edit, flow))
                case false => Redirect(routes.ContactDetailsController.get(index, edit, flow))
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
  override lazy val autoCompleteService = Play.current.injector.instanceOf(classOf[AutoCompleteService])
  override def dataCacheConnector = DataCacheConnector
}

