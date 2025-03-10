/*
 * Copyright 2024 HM Revenue & Customs
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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessdetails.VATRegisteredFormProvider
import models.businessdetails._
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany}
import models.businessmatching.{BusinessMatching, BusinessType}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.cache.Cache
import utils.{AuthAction, ControllerHelper}
import views.html.businessdetails.VATRegisteredView

import scala.concurrent.Future

class VATRegisteredController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: VATRegisteredFormProvider,
  vat_registered: VATRegisteredView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key) map { response =>
      val form: Form[VATRegistered] = (for {
        businessDetails <- response
        vatRegistered   <- businessDetails.vatRegistered
      } yield formProvider().fill(vatRegistered)).getOrElse(formProvider())
      Ok(vat_registered(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(vat_registered(formWithErrors, edit))),
        data => {
          val redirect = for {
            cache        <- dataCacheConnector.fetchAll(request.credId)
            businessType <- Future.successful(getBusinessType(cache))
            _            <- dataCacheConnector.update[BusinessDetails](request.credId, BusinessDetails.key) {
                              case Some(m) => m.vatRegistered(data)
                              case _       => BusinessDetails().vatRegistered(data)
                            }
          } yield (businessType, edit) match {
            case (_, true)                          => Redirect(routes.SummaryController.get)
            case (Some(LimitedCompany | LPrLLP), _) => Redirect(routes.CorporationTaxRegisteredController.get())
            case (_, false)                         => Redirect(routes.ConfirmRegisteredOfficeController.get(edit))
          }

          redirect.map(identity)
        }
      )
  }

  private def getBusinessType(maybeCache: Option[Cache]): Option[BusinessType] = for {
    cache        <- maybeCache
    businessType <- ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
  } yield businessType
}
