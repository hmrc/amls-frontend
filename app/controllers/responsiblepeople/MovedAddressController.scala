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

import javax.inject.Inject

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businesscustomer.{Address => BusinessCustomerAddress}
import models.businessmatching.BusinessMatching
import models.responsiblepeople._
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.moved_address

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class MovedAddressController @Inject()(override val messagesApi: MessagesApi,
                                       val dataCacheConnector: DataCacheConnector,
                                       val authConnector: AuthConnector) extends RepeatingSection with BaseController {


  def get(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetchAll map {
          optionalCache =>
            (for {
              cache <- optionalCache
              rp <- getData[ResponsiblePeople](cache, index)
              addr <- rp.addressHistory
            } yield {
              addr.currentAddress match {
                case Some(addr) => Ok(moved_address(EmptyForm, addr.personAddress, index, ControllerHelper.rpTitleName(Some(rp))))
                case _ => Redirect(routes.CurrentAddressController.get(index,true))
              }
            }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
        }
  }

  def post(index: Int) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MovedAddress](request.body) match {
        case f: InvalidForm =>
          dataCacheConnector.fetchAll map {
            optionalCache =>
              (for {
                cache <- optionalCache
                rp <- getData[ResponsiblePeople](cache, index)
                addr <- rp.addressHistory
              } yield {
                addr.currentAddress match {
                  case Some(addr) => BadRequest(views.html.responsiblepeople.moved_address(f, addr.personAddress,
                    index, ControllerHelper.rpTitleName(Some(rp))))
                  case _ => Redirect(routes.CurrentAddressController.get(index, true))
                }
              }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
          }
        case ValidForm(_, data) =>
          data.movedAddress match {
            case true => Future.successful(Redirect(routes.NewHomeAddressDateOfChangeController.get(index)))
            case false => Future.successful(Redirect(routes.CurrentAddressController.get(index,true)))
          }
      }
  }

}
