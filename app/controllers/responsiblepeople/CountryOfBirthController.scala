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

import javax.inject.{Inject, Singleton}
import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.Country
import models.responsiblepeople.{CountryOfBirth, PersonResidenceType, ResponsiblePerson}
import play.api.mvc.MessagesControllerComponents
import services.AutoCompleteService
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.country_of_birth

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class CountryOfBirthController @Inject()(authAction: AuthAction,
                                         val ds: CommonPlayDependencies,
                                         val dataCacheConnector: DataCacheConnector,
                                         val autoCompleteService: AutoCompleteService,
                                         val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request =>
        getData[ResponsiblePerson](request.credId, index) map {
          case Some(ResponsiblePerson(Some(personName),_,_,_,Some(personResidenceType),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
            personResidenceType.countryOfBirth match {
              case Some(country) =>
                Ok(country_of_birth(
                  Form2[CountryOfBirth](getCountryOfBirth(country)),
                  edit,
                  index,
                  flow,
                  personName.titleName,
                  autoCompleteService.getCountries))

              case _ =>
                Ok(country_of_birth(EmptyForm, edit, index, flow, personName.titleName, autoCompleteService.getCountries))
            }
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
            Ok(country_of_birth(EmptyForm, edit, index, flow, personName.titleName, autoCompleteService.getCountries))
          case _ => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request =>
        Form2[CountryOfBirth](request.body) match {
          case f: InvalidForm => getData[ResponsiblePerson](request.credId, index) map { rp =>
            BadRequest(country_of_birth(f, edit, index, flow, ControllerHelper.rpTitleName(rp), autoCompleteService.getCountries))
          }
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                rp.personResidenceType(updateCountryOfBirth(rp.personResidenceType, data))
              }
            } yield edit match {
              case true => Redirect(routes.DetailedAnswersController.get(index, flow))
              case false => Redirect(routes.NationalityController.get(index, edit, flow))
            }
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
  }

  private def getCountryOfBirth(countryOfBirth: Country): CountryOfBirth = {
    if(countryOfBirth.code != "GB") {
      CountryOfBirth(false, Some(countryOfBirth))
    } else {
      CountryOfBirth(true, None)
    }
  }

  private def updateCountryOfBirth(personResidenceType: Option[PersonResidenceType], data: CountryOfBirth): Option[PersonResidenceType] = {
    val countryOfBirth = if (data.bornInUk) {
      Some(Country("United Kingdom", "GB"))
    } else {
      data.country
    }
    personResidenceType.fold[Option[PersonResidenceType]](None)(pType => Some(pType.copy(countryOfBirth = countryOfBirth)))
  }
}
