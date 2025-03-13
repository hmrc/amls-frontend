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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.NationalityFormProvider
import models.responsiblepeople.{PersonResidenceType, ResponsiblePerson}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AutoCompleteService
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.NationalityView

import scala.concurrent.Future

class NationalityController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val autoCompleteService: AutoCompleteService,
  val cc: MessagesControllerComponents,
  formProvider: NationalityFormProvider,
  view: NationalityView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map { responsiblePerson =>
        responsiblePerson.fold(NotFound(notFoundView)) { person =>
          (person.personName, person.personResidenceType) match {
            case (Some(name), Some(PersonResidenceType(_, _, Some(nationality)))) =>
              Ok(
                view(
                  formProvider().fill(nationality),
                  edit,
                  index,
                  flow,
                  name.titleName,
                  autoCompleteService.formOptionsExcludeUK
                )
              )
            case (Some(name), _)                                                  =>
              Ok(view(formProvider(), edit, index, flow, name.titleName, autoCompleteService.formOptionsExcludeUK))
            case _                                                                => NotFound(notFoundView)
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
                  autoCompleteService.formOptionsExcludeUK
                )
              )
            },
          data =>
            {
              for {
                _ <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                       val residenceType = rp.personResidenceType.map(x => x.copy(nationality = Some(data)))
                       rp.personResidenceType(residenceType)
                     }
              } yield
                if (edit) {
                  Redirect(routes.DetailedAnswersController.get(index, flow))
                } else {
                  Redirect(routes.ContactDetailsController.get(index, edit, flow))
                }
            }.recoverWith { case _: IndexOutOfBoundsException =>
              Future.successful(NotFound(notFoundView))
            }
        )
  }
}
