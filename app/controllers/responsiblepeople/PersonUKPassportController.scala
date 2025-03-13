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
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.PersonUKPassportFormProvider
import models.responsiblepeople.{ResponsiblePerson, UKPassport, UKPassportNo, UKPassportYes}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.PersonUKPassportView

import javax.inject.Inject
import scala.concurrent.Future

class PersonUKPassportController @Inject() (
  override val messagesApi: MessagesApi,
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: PersonUKPassportFormProvider,
  view: PersonUKPassportView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map { responsiblePerson =>
        responsiblePerson.fold(NotFound(notFoundView)) { person =>
          (person.personName, person.ukPassport) match {
            case (Some(name), Some(passport)) =>
              Ok(view(formProvider().fill(passport), edit, index, flow, name.titleName))
            case (Some(name), _)              => Ok(view(formProvider(), edit, index, flow, name.titleName))
            case _                            => NotFound(notFoundView)
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
              BadRequest(view(formWithErrors, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            },
          data =>
            {
              (for {
                cache <- OptionT(fetchAllAndUpdateStrict[ResponsiblePerson](request.credId, index) { (_, rp) =>
                           data match {
                             case UKPassportYes(_) => rp.ukPassport(data).copy(nonUKPassport = None)
                             case _                => rp.ukPassport(data)
                           }
                         })
                rp    <- OptionT.fromOption[Future](cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key))
              } yield redirectTo(rp, data, index, edit, flow)) getOrElse NotFound(notFoundView)
            } recoverWith { case _: IndexOutOfBoundsException =>
              Future.successful(NotFound(notFoundView))
            }
        )
  }

  private def redirectTo(
    rp: Seq[ResponsiblePerson],
    data: UKPassport,
    index: Int,
    edit: Boolean,
    flow: Option[String]
  ) = {
    val responsiblePerson = rp(index - 1)
    data match {
      case UKPassportYes(_) if responsiblePerson.dateOfBirth.isEmpty                   =>
        Redirect(routes.CountryOfBirthController.get(index, edit, flow))
      case UKPassportYes(_) if edit                                                    => Redirect(routes.DetailedAnswersController.get(index, flow))
      case UKPassportYes(_)                                                            => Redirect(routes.CountryOfBirthController.get(index, edit, flow))
      case UKPassportNo if edit && responsiblePerson.ukPassport.contains(UKPassportNo) =>
        Redirect(routes.DetailedAnswersController.get(index, flow))
      case UKPassportNo                                                                => Redirect(routes.PersonNonUKPassportController.get(index, edit, flow))
    }
  }
}
