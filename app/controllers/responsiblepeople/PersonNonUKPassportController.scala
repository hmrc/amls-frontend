/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.responsiblepeople.PersonNonUKPassportFormProvider
import models.responsiblepeople.ResponsiblePerson
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.PersonNonUKPassportView

import javax.inject.Inject
import scala.concurrent.Future

class PersonNonUKPassportController @Inject()(override val messagesApi: MessagesApi,
                                              val dataCacheConnector: DataCacheConnector,
                                              authAction: AuthAction,
                                              val ds: CommonPlayDependencies,
                                              val cc: MessagesControllerComponents,
                                              formProvider: PersonNonUKPassportFormProvider,
                                              view: PersonNonUKPassportView,
                                              implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(index:Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map { responsiblePerson =>
        responsiblePerson.fold(NotFound(notFoundView)) { person =>
          (person.personName, person.nonUKPassport) match {
            case (Some(name), Some(nonUKPassport)) => Ok(view(formProvider().fill(nonUKPassport), edit, index, flow, name.titleName))
            case (Some(name), _) => Ok(view(formProvider(), edit, index, flow, name.titleName))
            case _ => NotFound(notFoundView)
          }
        }
      }
  }


  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider().bindFromRequest().fold(
        formWithErrors => getData[ResponsiblePerson](request.credId, index) map { rp =>
          BadRequest(view(formWithErrors, edit, index, flow, ControllerHelper.rpTitleName(rp)))
        },
        data => {
          for {
            result <- fetchAllAndUpdateStrict[ResponsiblePerson](request.credId, index) { (_, rp) =>
              rp.nonUKPassport(data)
            }
          } yield redirectToNextPage(result, index, edit, flow)

        } recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      )
  }

  private def redirectToNextPage(result: Option[CacheMap], index: Int, edit: Boolean, flow: Option[String])
                                (implicit request: Request[AnyContent]) = {
    (for {
      cache <- result
      rp <- getData[ResponsiblePerson](cache, index)
    } yield (rp.dateOfBirth.isDefined, edit) match {
      case (true, true) => Redirect(routes.DetailedAnswersController.get(index, flow))
      case (true, false) => Redirect(routes.CountryOfBirthController.get(index, edit, flow))
      case(false, _) => Redirect(routes.DateOfBirthController.get(index, edit, flow))
    }).getOrElse(NotFound(notFoundView))
  }
}
