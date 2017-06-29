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

import cats.data.OptionT
import cats.implicits._
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.Country
import models.responsiblepeople._
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.person_residence_type

import scala.concurrent.Future

trait PersonResidentTypeController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(Some(personName), Some(residencyType),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(person_residence_type(Form2[PersonResidenceType](residencyType), edit, index, flow, personName.titleName))
          case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(person_residence_type(EmptyForm, edit, index, flow, personName.titleName))
          case _ => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[PersonResidenceType](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePeople](index) map { rp =>
              BadRequest(person_residence_type(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            val residency = data.isUKResidence
            (for {
              cache <- OptionT(fetchAllAndUpdateStrict[ResponsiblePeople](index) { (_, rp) =>
                val nationality = rp.personResidenceType.fold[Option[Country]](None)(x => x.nationality)
                val countryOfBirth = rp.personResidenceType.fold[Option[Country]](None)(x => x.countryOfBirth)
                val updatedData = data.copy(countryOfBirth = countryOfBirth, nationality = nationality)
                residency match {
                  case UKResidence(_) => rp.personResidenceType(updatedData).copy(ukPassport = None, nonUKPassport = None, dateOfBirth = None)
                  case NonUKResidence => rp.personResidenceType(updatedData)
                }
              })
              rp <- OptionT.fromOption[Future](cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key))
            } yield {
              redirectGivenResidency(residency, rp, index, edit, flow)
            }) getOrElse NotFound(notFoundView)
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
  }

  private def redirectGivenResidency(
                                      isUKResidence: Residency,
                                      rp: Seq[ResponsiblePeople],
                                      index: Int,
                                      edit: Boolean = false,
                                      flow: Option[String]
                                    ) = {

    val existingPassport = rp(index - 1).ukPassport

    isUKResidence match {
      case UKResidence(_) if edit => Redirect(routes.DetailedAnswersController.get(index))
      case UKResidence(_) => Redirect(routes.CountryOfBirthController.get(index, edit, flow))
      case NonUKResidence if existingPassport.isEmpty => Redirect(routes.PersonUKPassportController.get(index, edit, flow))
      case NonUKResidence if edit => Redirect(routes.DetailedAnswersController.get(index))
      case NonUKResidence => Redirect(routes.PersonUKPassportController.get(index, edit, flow))
    }

  }
}

object PersonResidentTypeController extends PersonResidentTypeController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector

  override def dataCacheConnector = DataCacheConnector
}

