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

package controllers.aboutthebusiness

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.aboutthebusiness._
import models.businessmatching.BusinessType.Partnership
import models.businessmatching.BusinessMatching
import utils.ControllerHelper
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait VATRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        response =>
          val form: Form2[VATRegistered] = (for {
            aboutTheBusiness <- response
            vatRegistered <- aboutTheBusiness.vatRegistered
          } yield Form2[VATRegistered](vatRegistered)).getOrElse(EmptyForm)
          Ok(vat_registered(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[VATRegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(vat_registered(f, edit)))
        case ValidForm(_, data) =>
          dataCacheConnector.fetchAll map {
            optionalCache =>
              (for {
                cache <- optionalCache
                businessType <- ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
                aboutTheBusiness <- cache.getEntry[AboutTheBusiness](AboutTheBusiness.key)
              } yield {
                dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key,
                  aboutTheBusiness.vatRegistered(data))
                (businessType, edit) match {
                  case (_,true) => Redirect(routes.SummaryController.get())
                  case (Partnership, false) => Redirect(routes.ConfirmRegisteredOfficeController.get(edit))
                  case (_, _) => Redirect(routes.CorporationTaxRegisteredController.get(edit))
                }
              }).getOrElse(Redirect(routes.ConfirmRegisteredOfficeController.get(edit)))
          }
      }
    }
  }
}

object VATRegisteredController extends VATRegisteredController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
