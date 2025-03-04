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

import cats.data.OptionT
import cats.implicits._
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.PersonResidentTypeFormProvider
import models.responsiblepeople._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.responsiblepeople.PersonResidentTypeService
import utils.{AuthAction, ControllerHelper}
import views.html.responsiblepeople.PersonResidenceTypeView

import javax.inject.Inject
import scala.concurrent.Future

class PersonResidentTypeController @Inject() (
  override val messagesApi: MessagesApi,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  personResidenceTypeService: PersonResidentTypeService,
  formProvider: PersonResidentTypeFormProvider,
  view: PersonResidenceTypeView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc) {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      personResidenceTypeService.getResponsiblePerson(request.credId, index) map { responsiblePerson =>
        responsiblePerson.fold(NotFound(notFoundView)) { person =>
          (person.personName, person.personResidenceType) match {
            case (Some(name), Some(residenceType)) =>
              Ok(view(formProvider().fill(residenceType), edit, index, flow, name.titleName))
            case (Some(name), _)                   => Ok(view(formProvider(), edit, index, flow, name.titleName))
            case _                                 => NotFound(notFoundView)
          }
        }
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithError =>
            personResidenceTypeService.getResponsiblePerson(request.credId, index) map { rp =>
              BadRequest(view(formWithError, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            },
          data =>
            {
              val residency = data.isUKResidence
              (for {
                cache <- personResidenceTypeService.getCache(data, request.credId, index)
                rp    <- OptionT.fromOption[Future](cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key))
              } yield redirectGivenResidency(residency, rp, index, edit, flow)) getOrElse NotFound(notFoundView)
            }.recoverWith { case _: IndexOutOfBoundsException =>
              Future.successful(NotFound(notFoundView))
            }
        )
  }

  private def redirectGivenResidency(
    isUKResidence: Residency,
    rp: Seq[ResponsiblePerson],
    index: Int,
    edit: Boolean,
    flow: Option[String]
  ) = {

    val existingPassport = rp(index - 1).ukPassport

    isUKResidence match {
      case UKResidence(_) if edit                     => Redirect(routes.DetailedAnswersController.get(index, flow))
      case UKResidence(_)                             => Redirect(routes.CountryOfBirthController.get(index, edit, flow))
      case NonUKResidence if existingPassport.isEmpty =>
        Redirect(routes.PersonUKPassportController.get(index, edit, flow))
      case NonUKResidence if edit                     => Redirect(routes.DetailedAnswersController.get(index, flow))
      case NonUKResidence                             => Redirect(routes.PersonUKPassportController.get(index, edit, flow))
    }

  }
}
