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

import cats.data.OptionT
import cats.implicits._
import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.{BusinessMatchingConnector, DataCacheConnector}
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutthebusiness.{AboutTheBusiness, CorporationTaxRegistered, CorporationTaxRegisteredYes}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import views.html.aboutthebusiness.corporation_tax_registered

import scala.concurrent.Future

trait CorporationTaxRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector
  val businessMatchingConnector: BusinessMatchingConnector
  val failedResult = InternalServerError("Failed to update the business corporation tax number")

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) flatMap {
        case Some(response) if response.corporationTaxRegistered.isDefined =>
          Future.successful(Ok(corporation_tax_registered(Form2[CorporationTaxRegistered](response.corporationTaxRegistered.get), edit)))

        case _ if ApplicationConfig.businessMatchingDetailsToggle =>
          businessMatchingConnector.getReviewDetails flatMap {
            case Some(details) if details.utr.isDefined =>
              updateCache(CorporationTaxRegisteredYes(details.utr.get)) map { _ =>
                getRedirectLocation(edit)
              } getOrElse failedResult
            case _ => Future.successful(Ok(corporation_tax_registered(EmptyForm, edit)))
          }

        case _ => Future.successful(Ok(corporation_tax_registered(EmptyForm, edit)))

      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CorporationTaxRegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(corporation_tax_registered(f, edit)))
        case ValidForm(_, data) =>
          updateCache(data) map { _ =>
            getRedirectLocation(edit)
          } getOrElse failedResult
      }
    }
  }

  private def updateCache(data: CorporationTaxRegistered)(implicit auth: AuthContext, hc: HeaderCarrier) = for {
    aboutTheBusiness <- OptionT(dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key))
    cacheMap <- OptionT.liftF(dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key, aboutTheBusiness.corporationTaxRegistered(data)))
  } yield cacheMap

  private def getRedirectLocation(edit: Boolean) = if (edit) {
    Redirect(routes.SummaryController.get())
  } else {
    Redirect(routes.ConfirmRegisteredOfficeController.get())
  }
}

object CorporationTaxRegisteredController extends CorporationTaxRegisteredController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val businessMatchingConnector = BusinessMatchingConnector
}
