/*
 * Copyright 2018 HM Revenue & Customs
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
import views.html.responsiblepeople.confirm_address
import scala.concurrent.ExecutionContext.Implicits.global


class ConfirmAddressController @Inject()(override val messagesApi: MessagesApi,
                                         val dataCacheConnector: DataCacheConnector,
                                         val authConnector: AuthConnector)
  extends RepeatingSection with BaseController {

  def getAddress(businessMatching: BusinessMatching): Option[BusinessCustomerAddress] = {
      businessMatching.reviewDetails.fold[Option[BusinessCustomerAddress]](None)(r => Some(r.businessAddress))
  }

  def updateAddressFromBM(bmOpt: Option[BusinessMatching]) : Option[ResponsiblePersonAddressHistory] = {
    bmOpt match {
      case Some(bm) => bm.reviewDetails.fold[Option[ResponsiblePersonAddressHistory]](None)(r => {
        val UKAddress = PersonAddressUK(r.businessAddress.line_1,
          r.businessAddress.line_2,
          r.businessAddress.line_3,
          r.businessAddress.line_4,
          r.businessAddress.postcode.getOrElse(""))
        val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, None)
        Some(ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress)))
      })
      case _ => None
    }
  }

  def get(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetchAll map {
          optionalCache =>
            (for {
              cache <- optionalCache
              rp <- getData[ResponsiblePerson](cache, index)
              bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            } yield {
              rp.addressHistory.isDefined match {
                case true
                  => Redirect(routes.CurrentAddressController.get(index))
                case _
                  => {
                  getAddress(bm) match {
                    case Some(addr) => Ok(confirm_address(EmptyForm, addr, index, ControllerHelper.rpTitleName(Some(rp))))
                    case _ => Redirect(routes.CurrentAddressController.get(index))
                  }
                }
              }
            }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
        }
  }

  def post(index: Int) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ConfirmAddress](request.body) match {
        case f: InvalidForm =>
          dataCacheConnector.fetchAll map {
            optionalCache =>
              (for {
                cache <- optionalCache
                rp <- getData[ResponsiblePerson](cache, index)
                bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
              } yield {
                getAddress(bm) match {
                  case Some(addr) => BadRequest(views.html.responsiblepeople.confirm_address(f, addr,
                    index, ControllerHelper.rpTitleName(Some(rp))))
                  case _ => Redirect(routes.CurrentAddressController.get(index))
                }
              }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
          }
        case ValidForm(_, data) =>
          data.confirmAddress match {
            case true => {
              fetchAllAndUpdateStrict[ResponsiblePerson](index) { (cache, rp) =>
                rp.copy(addressHistory = updateAddressFromBM(cache.getEntry[BusinessMatching](BusinessMatching.key)))
              } map ( _ => Redirect(routes.TimeAtCurrentAddressController.get(index)))
            }
            case false => {
              fetchAllAndUpdateStrict[ResponsiblePerson](index) { (cache, rp) =>
                rp.copy(addressHistory = None)
              } map ( _ => Redirect(routes.CurrentAddressController.get(index)))
            }
          }
      }
  }

}
