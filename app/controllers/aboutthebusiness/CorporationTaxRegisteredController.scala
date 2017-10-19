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
import config.AMLSAuthConnector
import connectors.{BusinessMatchingConnector, DataCacheConnector}
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutthebusiness.{AboutTheBusiness, CorporationTaxRegistered, CorporationTaxRegisteredYes}
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany, Partnership}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.ControllerHelper
import views.html.aboutthebusiness.corporation_tax_registered

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait CorporationTaxRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector
  val businessMatchingConnector: BusinessMatchingConnector
  val failedResult = InternalServerError("Failed to update the business corporation tax number")

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      filterByBusinessType { cache =>
        cache.getEntry[AboutTheBusiness](AboutTheBusiness.key) match {
          case Some(response) if response.corporationTaxRegistered.isDefined =>
            Future.successful(Ok(corporation_tax_registered(Form2[CorporationTaxRegistered](response.corporationTaxRegistered.get), edit)))
          case _ =>
            (for {
              bm <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
              details <- OptionT.fromOption[Future](bm.reviewDetails)
              utr <- OptionT.fromOption[Future](details.utr)
              _ <- updateCache(cache, CorporationTaxRegisteredYes(utr))
            } yield getRedirectLocation(edit)) getOrElse Ok(corporation_tax_registered(EmptyForm, edit))
        }
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      filterByBusinessType { cache =>
        Form2[CorporationTaxRegistered](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(corporation_tax_registered(f, edit)))
          case ValidForm(_, data) =>
            updateCache(cache, data) map { _ =>
              getRedirectLocation(edit)
            } getOrElse failedResult
        }
      }
    }
  }

  private def filterByBusinessType(fn: CacheMap => Future[Result])(implicit hc:HeaderCarrier, ac:AuthContext, request: Request[_]): Future[Result] = {
    OptionT(dataCacheConnector.fetchAll) flatMap { cache =>
      ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key)) match {
        case Some((Partnership | LPrLLP | LimitedCompany)) => OptionT.liftF(fn(cache))
        case _ => OptionT.pure[Future, Result](NotFound(notFoundView))
      }
    } getOrElse InternalServerError("Could not retrieve business type")
  }

  private def updateCache(cache: CacheMap, data: CorporationTaxRegistered)(implicit auth: AuthContext, hc: HeaderCarrier) = for {
    aboutTheBusiness <- OptionT.fromOption[Future](cache.getEntry[AboutTheBusiness](AboutTheBusiness.key))
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
