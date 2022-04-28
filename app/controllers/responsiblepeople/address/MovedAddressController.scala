/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.responsiblepeople._
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.address.moved_address


import scala.concurrent.Future


class MovedAddressController @Inject()(override val messagesApi: MessagesApi,
                                       val dataCacheConnector: DataCacheConnector,
                                       authAction: AuthAction,
                                       val ds: CommonPlayDependencies,
                                       val cc: MessagesControllerComponents,
                                       moved_address: moved_address) extends AmlsBaseController(ds, cc) with RepeatingSection {


  def get(index: Int) = authAction.async {
    implicit request =>
      dataCacheConnector.fetchAll(request.credId) map {
        optionalCache =>
          (for {
            cache <- optionalCache
            rp <- getData[ResponsiblePerson](cache, index)
            addr <- rp.addressHistory
          } yield {
            addr.currentAddress match {
              case Some(addr) => Ok(moved_address(EmptyForm, addr.personAddress, index, ControllerHelper.rpTitleName(Some(rp))))
              case _ => Redirect(address.routes.CurrentAddressController.get(index, true))
            }
          }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get)
      }
  }

  def post(index: Int) = authAction.async {
    implicit request =>
      Form2[MovedAddress](request.body) match {
        case f: InvalidForm =>
          dataCacheConnector.fetchAll(request.credId) map {
            optionalCache =>
              (for {
                cache <- optionalCache
                rp <- getData[ResponsiblePerson](cache, index)
                addr <- rp.addressHistory
              } yield {
                addr.currentAddress match {
                  case Some(addr) => BadRequest(moved_address(f, addr.personAddress, index, ControllerHelper.rpTitleName(Some(rp))))
                  case _ => Redirect(address.routes.CurrentAddressController.get(index, true))
                }
              }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get)
          }
        case ValidForm(_, data) =>
          data.movedAddress match {
            case true => Future.successful(Redirect(address.routes.NewHomeAddressDateOfChangeController.get(index)))
            case false => Future.successful(Redirect(address.routes.CurrentAddressController.get(index, true)))
          }
      }
  }

}
