/*
 * Copyright 2020 HM Revenue & Customs
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

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessdetails._
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthAction, ControllerHelper}
import views.html.businessdetails._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class VATRegisteredController @Inject () (
                                           val dataCacheConnector: DataCacheConnector,
                                           val authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {



  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key) map {
        response =>
          val form: Form2[VATRegistered] = (for {
            businessDetails <- response
            vatRegistered <- businessDetails.vatRegistered
          } yield Form2[VATRegistered](vatRegistered)).getOrElse(EmptyForm)
          Ok(vat_registered(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[VATRegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(vat_registered(f, edit)))

        case ValidForm(_, data) =>

          val redirect = for {
            cache <- dataCacheConnector.fetchAll(request.credId)
            businessType <- Future.successful(getBusinessType(cache))
            _ <- dataCacheConnector.update[BusinessDetails](request.credId, BusinessDetails.key) {
              case Some(m) => m.vatRegistered(data)
              case _ => BusinessDetails().vatRegistered(data)
            }
          } yield (businessType, edit) match {
            case (_,true) => Redirect(routes.SummaryController.get())
            case (Some(LimitedCompany | LPrLLP), _) => Redirect(routes.CorporationTaxRegisteredController.get())
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
