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

package controllers.businessmatching

import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.BusinessType._
import models.businessmatching.{BusinessMatching, BusinessType}
import utils.AuthAction
import views.html.businessmatching._

import scala.concurrent.Future

class BusinessTypeController @Inject()(val dataCache: DataCacheConnector,
                                       authAction: AuthAction) extends DefaultBaseController {

  def get() = authAction.async {
    implicit request =>
      dataCache.fetch[BusinessMatching](request.credId, BusinessMatching.key) map {
        maybeBusinessMatching =>
          val redirect = for {
            businessMatching <- maybeBusinessMatching
            reviewDetails <- businessMatching.reviewDetails
            businessType <- reviewDetails.businessType
          } yield businessType match {
            case UnincorporatedBody =>
              Redirect(routes.TypeOfBusinessController.get())
            case LPrLLP | LimitedCompany =>
              Redirect(routes.CompanyRegistrationNumberController.get())
            case _ =>
              Redirect(routes.RegisterServicesController.get())
          }
          redirect getOrElse Ok(business_type(EmptyForm))
      }
  }

  def post() = authAction.async {
    implicit request =>
      Form2[BusinessType](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_type(f)))
        case ValidForm(_, data) =>
          dataCache.fetch[BusinessMatching](request.credId, BusinessMatching.key) flatMap {
            bm =>
              val updatedDetails = for {
                businessMatching <- bm
                reviewDetails <- businessMatching.reviewDetails
              } yield {
                businessMatching.copy(
                  reviewDetails = Some(
                    reviewDetails.copy(
                      businessType = Some(data)
                    )
                  )
                )
              }
              updatedDetails map {
                details =>
                  dataCache.save[BusinessMatching](request.credId, BusinessMatching.key, updatedDetails) map {
                    _ =>
                      data match {
                        case UnincorporatedBody =>
                          Redirect(routes.TypeOfBusinessController.get())
                        case LPrLLP | LimitedCompany =>
                          Redirect(routes.CompanyRegistrationNumberController.get())
                        case _ =>
                          Redirect(routes.RegisterServicesController.get())
                      }
                  }
              } getOrElse Future.successful {
                Redirect(routes.RegisterServicesController.get())
              }
          }
      }
  }
}

