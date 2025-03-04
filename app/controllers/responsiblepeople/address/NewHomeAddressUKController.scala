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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.address.NewHomeAddressUKFormProvider
import models.responsiblepeople._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, ControllerHelper}
import views.html.responsiblepeople.address.NewHomeAddressUKView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class NewHomeAddressUKController @Inject() (
  authAction: AuthAction,
  val dataCacheConnector: DataCacheConnector,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: NewHomeAddressUKFormProvider,
  view: NewHomeAddressUKView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with AddressHelper {

  def get(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    getData[ResponsiblePerson](request.credId, index) map { responsiblePerson =>
      responsiblePerson.fold(NotFound(notFoundView)) { person =>
        person.personName match {
          case Some(name) => Ok(view(formProvider(), index, name.titleName))
          case _          => NotFound(notFoundView)
        }
      }
    }
  }

  def post(index: Int): Action[AnyContent] =
    authAction.async { implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(view(formWithErrors, index, ControllerHelper.rpTitleName(rp)))
            },
          data =>
            for {
              moveDate <- dataCacheConnector.fetch[NewHomeDateOfChange](request.credId, NewHomeDateOfChange.key)
              _        <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                            rp.addressHistory(convertToCurrentAddress(data, moveDate, rp))
                          }
              _        <- dataCacheConnector
                            .save[NewHomeDateOfChange](request.credId, NewHomeDateOfChange.key, NewHomeDateOfChange(None))
              _        <- dataCacheConnector.removeByKey(request.credId, NewHomeAddress.key)
            } yield Redirect(controllers.responsiblepeople.routes.DetailedAnswersController.get(index))
        )
        .recoverWith { case _: IndexOutOfBoundsException =>
          Future.successful(NotFound(notFoundView))
        }
    }
}
