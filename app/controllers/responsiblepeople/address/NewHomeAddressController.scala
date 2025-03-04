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
import forms.responsiblepeople.address.NewHomeAddressFormProvider
import models.responsiblepeople._
import play.api.mvc._
import utils.{AuthAction, ControllerHelper}
import views.html.responsiblepeople.address.NewHomeAddressView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class NewHomeAddressController @Inject() (
  authAction: AuthAction,
  val dataCacheConnector: DataCacheConnector,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: NewHomeAddressFormProvider,
  view: NewHomeAddressView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with AddressHelper {

  def get(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    for {
      rp         <- getData[ResponsiblePerson](request.credId, index)
      newAddress <- dataCacheConnector.fetch[NewHomeAddress](request.credId, NewHomeAddress.key)
      nameOpt    <- Future.successful(rp.map(_.personName))
    } yield (nameOpt.flatten, newAddress) match {
      case (Some(name), Some(newHomeAddress)) => Ok(view(formProvider().fill(newHomeAddress), index, name.titleName))
      case (Some(name), None)                 => Ok(view(formProvider(), index, name.titleName))
      case _                                  => NotFound(notFoundView)
    }
  }

  def post(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          getData[ResponsiblePerson](request.credId, index) map { rp =>
            BadRequest(view(formWithErrors, index, ControllerHelper.rpTitleName(rp)))
          },
        data => processFormAndRedirect(data, index, request.credId)
      )
      .recoverWith { case _: IndexOutOfBoundsException =>
        Future.successful(NotFound(notFoundView))
      }
  }

  private def processFormAndRedirect(data: NewHomeAddress, index: Int, credId: String)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    for {
      redirect <- dataCacheConnector.save[NewHomeAddress](credId, NewHomeAddress.key, data) map { _ =>
                    data.personAddress match {
                      case _: PersonAddressUK    => Redirect(routes.NewHomeAddressUKController.get(index))
                      case _: PersonAddressNonUK => Redirect(routes.NewHomeAddressNonUKController.get(index))
                      case _                     => NotFound(notFoundView)
                    }
                  }
    } yield redirect
}
