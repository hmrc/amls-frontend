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

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.aboutthebusiness._
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany, Partnership}
import models.businessmatching.BusinessMatching
import uk.gov.hmrc.http.cache.client.CacheMap
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

          val redirect = for {
            cache <- dataCacheConnector.fetchAll
            businessType <- Future.successful(getBusinessType(cache))
            _ <- dataCacheConnector.update[AboutTheBusiness](AboutTheBusiness.key) {
              case Some(m) => m.vatRegistered(data)
              case _ => AboutTheBusiness().vatRegistered(data)
            }
          } yield (businessType, edit) match {
            case (_,true) => Redirect(routes.SummaryController.get())
            case (Some(LimitedCompany | LPrLLP), _) => Redirect(routes.CorporationTaxRegisteredController.get(edit))
            case (_, false) => Redirect(routes.ConfirmRegisteredOfficeController.get(edit))
          }

          redirect.map(identity)
      }
    }
  }

  private def getBusinessType(maybeCache: Option[CacheMap]) = for {
    cache <- maybeCache
    businessType <- ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
  } yield businessType
}

object VATRegisteredController extends VATRegisteredController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
