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
import controllers.responsiblepeople.address
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.address.MovedAddressFormProvider
import javax.inject.Inject
import models.responsiblepeople._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.address.MovedAddressView

import scala.concurrent.Future

class MovedAddressController @Inject() (
  override val messagesApi: MessagesApi,
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: MovedAddressFormProvider,
  view: MovedAddressView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetchAll(request.credId) map { optionalCache =>
      (for {
        cache <- optionalCache
        rp    <- getData[ResponsiblePerson](cache, index)
        addr  <- rp.addressHistory
      } yield addr.currentAddress match {
        case Some(addr) => Ok(view(formProvider(), addr.personAddress, index, ControllerHelper.rpTitleName(Some(rp))))
        case _          => Redirect(address.routes.CurrentAddressController.get(index, edit = true))
      }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
    }
  }

  def post(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          dataCacheConnector.fetchAll(request.credId) map { optionalCache =>
            (for {
              cache <- optionalCache
              rp    <- getData[ResponsiblePerson](cache, index)
              addr  <- rp.addressHistory
            } yield addr.currentAddress match {
              case Some(addr) =>
                BadRequest(view(formWithErrors, addr.personAddress, index, ControllerHelper.rpTitleName(Some(rp))))
              case _          => Redirect(address.routes.CurrentAddressController.get(index, edit = true))
            }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
          },
        {
          case true  => Future.successful(Redirect(address.routes.NewHomeAddressDateOfChangeController.get(index)))
          case false => Future.successful(Redirect(address.routes.CurrentAddressController.get(index, edit = true)))
        }
      )
  }

}
