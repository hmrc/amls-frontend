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

package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.businessmatching.BusinessType._
import play.api.Logger
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching._

import scala.concurrent.Future

trait BusinessTypeController extends BaseController {

  private[controllers] def dataCache: DataCacheConnector

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[BusinessMatching](BusinessMatching.key) map {
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

  def post() = Authorised.async {
    implicit authContext => implicit request =>
      Form2[BusinessType](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_type(f)))
        case ValidForm(_, data) =>
          dataCache.fetch[BusinessMatching](BusinessMatching.key) flatMap {
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
                  dataCache.save[BusinessMatching](BusinessMatching.key, updatedDetails) map {
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
                // TODO error and logging
                Redirect(routes.RegisterServicesController.get())
              }
          }
      }
  }
}

object BusinessTypeController extends BusinessTypeController {
  // $COVERAGE-OFF$
  override private[controllers] def dataCache: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
