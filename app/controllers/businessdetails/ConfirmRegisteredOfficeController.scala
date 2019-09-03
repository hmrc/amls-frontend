/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.businessdetails

import cats.data.OptionT
import cats.implicits._
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businesscustomer.Address
import models.businessdetails.{BusinessDetails, ConfirmRegisteredOffice, RegisteredOffice, RegisteredOfficeUK}
import models.businessmatching.BusinessMatching
import utils.AuthAction
import views.html.businessdetails._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConfirmRegisteredOfficeController @Inject () (
                                                   val dataCache: DataCacheConnector,
                                                   val authAction: AuthAction
                                                   ) extends DefaultBaseController {


  def updateBMAddress(bm: BusinessMatching): Option[RegisteredOffice] = {
    bm.reviewDetails.fold[Option[RegisteredOffice]](None)(dtls => Some(RegisteredOfficeUK(
      dtls.businessAddress.line_1,
      dtls.businessAddress.line_2,
      dtls.businessAddress.line_3,
      dtls.businessAddress.line_4,
      dtls.businessAddress.postcode.getOrElse("")
    )))
  }

  def getAddress(businessMatching: Future[Option[BusinessMatching]]): Future[Option[Address]] = {
    businessMatching map {
      case Some(bm) => bm.reviewDetails.fold[Option[Address]](None)(r => Some(r.businessAddress))
      case _ => None
    }
  }

  def hasRegisteredAddress(businessDetails: Future[Option[BusinessDetails]]) : Future[Option[Boolean]] = {
    businessDetails.map {
      case Some(atb) => Some(atb.registeredOffice.isDefined)
      case _ => Some(false)
    }
  }


  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      (for {
        hra <- OptionT.liftF(hasRegisteredAddress(dataCache.fetch[BusinessDetails](request.credId, BusinessDetails.key)))
        bma <- OptionT.liftF(getAddress(dataCache.fetch[BusinessMatching](request.credId, BusinessMatching.key)))
      } yield (hra,bma) match {
        case (Some(false),Some(data)) => Ok(confirm_registered_office_or_main_place(EmptyForm, data))
        case _ => Redirect(routes.RegisteredOfficeIsUKController.get(edit))
      }).getOrElse(Redirect(routes.RegisteredOfficeIsUKController.get(edit)))
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
      Form2[ConfirmRegisteredOffice](request.body) match {
        case f: InvalidForm =>
          getAddress(dataCache.fetch[BusinessMatching](request.credId, BusinessMatching.key)) map {
            case Some(data) => BadRequest(confirm_registered_office_or_main_place(f, data))
            case _ => Redirect(routes.RegisteredOfficeIsUKController.get(edit))
          }
        case ValidForm(_, data) =>

          def updateRegisteredOfficeAndRedirect(bm: BusinessMatching,
                                                businessDetails: BusinessDetails) = {

            val address = if (data.isRegOfficeOrMainPlaceOfBusiness) {
              updateBMAddress(bm)
            } else {
              None
            }

            dataCache.save[BusinessDetails](request.credId, BusinessDetails.key, businessDetails.copy(registeredOffice = address)) map { _ =>
              if (data.isRegOfficeOrMainPlaceOfBusiness) {
                Redirect(routes.ContactingYouController.get(edit))
              } else {
                Redirect(routes.RegisteredOfficeIsUKController.get(edit))
              }
            }
          }

          (for {
            cache <- OptionT(dataCache.fetchAll(request.credId))
            bm <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
            businessDetails <- OptionT.fromOption[Future](cache.getEntry[BusinessDetails](BusinessDetails.key))
            result <- OptionT.liftF(updateRegisteredOfficeAndRedirect(bm, businessDetails))
          } yield {
            result
          }).getOrElse(Redirect(routes.RegisteredOfficeIsUKController.get(edit)))
      }
  }
}