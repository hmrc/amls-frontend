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

package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{TransmittingMoney, MsbServices, BusinessMatching}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait ServicesController extends BaseController {

  def cache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      cache.fetch[BusinessMatching](BusinessMatching.key) map {
        response =>
          val form = (for {
            bm <- response
            services <- bm.msbServices
          } yield Form2[MsbServices](services)).getOrElse(EmptyForm)

          Ok(views.html.businessmatching.services(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    import jto.validation.forms.Rules._
    implicit authContext => implicit request =>
      Form2[MsbServices](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.businessmatching.services(f, edit)))
        case ValidForm(_, data) =>
          for {
            bm <- cache.fetch[BusinessMatching](BusinessMatching.key)
             _ <- cache.save[BusinessMatching](BusinessMatching.key,
               data.msbServices.contains(TransmittingMoney) match {
                 case true => bm.msbServices(data)
                 case false => bm.copy(msbServices = Some(data), businessAppliedForPSRNumber = None)
               }
             )
          } yield data.msbServices.contains(TransmittingMoney) match {
            case true => Redirect(routes.BusinessAppliedForPSRNumberController.get(edit))
            case false => Redirect(routes.SummaryController.get())
          }
      }
  }
}

object ServicesController extends ServicesController {
  // $COVERAGE-OFF$
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override val cache = DataCacheConnector
}
