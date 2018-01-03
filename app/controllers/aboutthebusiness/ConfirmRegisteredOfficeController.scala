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

package controllers.aboutthebusiness

import cats.data.OptionT
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutthebusiness.{AboutTheBusiness, ConfirmRegisteredOffice, RegisteredOffice, RegisteredOfficeUK}
import models.businesscustomer.Address
import models.businessmatching.BusinessMatching
import views.html.aboutthebusiness._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.implicits._

trait ConfirmRegisteredOfficeController extends BaseController {

  def dataCache: DataCacheConnector

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

  def hasRegisteredAddress(aboutTheBusiness: Future[Option[AboutTheBusiness]]) : Future[Option[Boolean]] = {
    aboutTheBusiness.map {
      case Some(atb) => Some(atb.registeredOffice.isDefined)
      case _ => Some(false)
    }
  }


  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          hra <- OptionT.liftF(hasRegisteredAddress(dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)))
          bma <- OptionT.liftF(getAddress(dataCache.fetch[BusinessMatching](BusinessMatching.key)))
        } yield (hra,bma) match {
          case (Some(false),Some(data)) => Ok(confirm_registered_office_or_main_place(EmptyForm, data))
          case _ => Redirect(routes.RegisteredOfficeController.get(edit))
        }).getOrElse(Redirect(routes.RegisteredOfficeController.get(edit)))

  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[ConfirmRegisteredOffice](request.body) match {
          case f: InvalidForm =>
            getAddress(dataCache.fetch[BusinessMatching](BusinessMatching.key)) map {
              case Some(data) => BadRequest(confirm_registered_office_or_main_place(f, data))
              case _ => Redirect(routes.RegisteredOfficeController.get(edit))
            }
          case ValidForm(_, data) =>

            def updateRegisteredOfficeAndRedirect(bm: BusinessMatching,
                                                  aboutTheBusiness: AboutTheBusiness) = {

              val address = if (data.isRegOfficeOrMainPlaceOfBusiness) {
                updateBMAddress(bm)
              } else {
                None
              }

              dataCache.save[AboutTheBusiness](AboutTheBusiness.key, aboutTheBusiness.copy(registeredOffice = address)) map { _ =>
                if (data.isRegOfficeOrMainPlaceOfBusiness) {
                  Redirect(routes.ContactingYouController.get(edit))
                } else {
                  Redirect(routes.RegisteredOfficeController.get(edit))
                }
              }
            }

            (for {
              cache <- OptionT(dataCache.fetchAll)
              bm <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
              aboutTheBusiness <- OptionT.fromOption[Future](cache.getEntry[AboutTheBusiness](AboutTheBusiness.key))
              result <- OptionT.liftF(updateRegisteredOfficeAndRedirect(bm, aboutTheBusiness))
            } yield {
              result
            }).getOrElse(Redirect(routes.RegisteredOfficeController.get(edit)))
        }
  }
}

object ConfirmRegisteredOfficeController extends ConfirmRegisteredOfficeController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
