/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.CountryOfBirthFormProvider
import models.Country
import models.responsiblepeople.{CountryOfBirth, PersonResidenceType, ResponsiblePerson}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AutoCompleteService
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.CountryOfBirthView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class CountryOfBirthController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val autoCompleteService: AutoCompleteService,
  val cc: MessagesControllerComponents,
  formProvider: CountryOfBirthFormProvider,
  view: CountryOfBirthView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map { responsiblePerson =>
        responsiblePerson.fold(NotFound(notFoundView)) { person =>
          (person.personName, person.personResidenceType) match {
            case (Some(name), Some(PersonResidenceType(_, Some(countryOfBirth), _))) =>
              Ok(
                view(
                  formProvider().fill(getCountryOfBirth(countryOfBirth)),
                  edit,
                  index,
                  flow,
                  name.titleName,
                  autoCompleteService.formOptions
                )
              )
            case (Some(name), _)                                                     =>
              Ok(view(formProvider(), edit, index, flow, name.titleName, autoCompleteService.formOptions))
            case _                                                                   => NotFound(notFoundView)
          }
        }
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(
                view(
                  formWithErrors,
                  edit,
                  index,
                  flow,
                  ControllerHelper.rpTitleName(rp),
                  autoCompleteService.formOptions
                )
              )
            },
          data =>
            {
              for {
                _ <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                       rp.personResidenceType(updateCountryOfBirth(rp.personResidenceType, data))
                     }
              } yield edit match {
                case true  => Redirect(routes.DetailedAnswersController.get(index, flow))
                case false => Redirect(routes.NationalityController.get(index, edit, flow))
              }
            }.recoverWith { case _: IndexOutOfBoundsException =>
              Future.successful(NotFound(notFoundView))
            }
        )
  }

  private def getCountryOfBirth(countryOfBirth: Country): CountryOfBirth =
    if (countryOfBirth.code != "GB") {
      CountryOfBirth(false, Some(countryOfBirth))
    } else {
      CountryOfBirth(true, None)
    }

  private def updateCountryOfBirth(
    personResidenceType: Option[PersonResidenceType],
    data: CountryOfBirth
  ): Option[PersonResidenceType] = {
    val countryOfBirth = if (data.bornInUk) {
      Some(Country("United Kingdom", "GB"))
    } else {
      data.country
    }
    personResidenceType.fold[Option[PersonResidenceType]](None)(pType =>
      Some(pType.copy(countryOfBirth = countryOfBirth))
    )
  }
}
