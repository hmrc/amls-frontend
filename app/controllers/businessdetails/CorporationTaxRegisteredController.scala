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

package controllers.businessdetails

import cats.data.OptionT
import cats.implicits._
import com.google.inject.Inject
import connectors.{BusinessMatchingConnector, DataCacheConnector}
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessdetails.{BusinessDetails, CorporationTaxRegistered, CorporationTaxRegisteredYes}
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthAction, ControllerHelper}
import views.html.businessdetails.corporation_tax_registered

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CorporationTaxRegisteredController @Inject () (
                                                      val dataCacheConnector: DataCacheConnector,
                                                      val businessMatchingConnector: BusinessMatchingConnector,
                                                      val authAction: AuthAction
                                                    ) extends DefaultBaseController {

  val failedResult = InternalServerError("Failed to update the business corporation tax number")

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      filterByBusinessType ( request.credId, cache =>
        cache.getEntry[BusinessDetails](BusinessDetails.key) match {
          case _ =>
            (for {
              bm <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
              details <- OptionT.fromOption[Future](bm.reviewDetails)
              utr <- OptionT.fromOption[Future](details.utr)
              _ <- updateCache(request.credId, cache, CorporationTaxRegisteredYes(utr))
            } yield getRedirectLocation(edit)) getOrElse Ok(corporation_tax_registered(EmptyForm, edit))
        }
      )
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      filterByBusinessType ( request.credId, cache =>
        Form2[CorporationTaxRegistered](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(corporation_tax_registered(f, edit)))
          case ValidForm(_, data) =>
            updateCache(request.credId, cache, data) map { _ =>
              getRedirectLocation(edit)
            } getOrElse failedResult
        }
      )
    }
  }

  private def filterByBusinessType(cacheId: String, fn: CacheMap => Future[Result])(implicit hc:HeaderCarrier, request: Request[_]): Future[Result] = {
    OptionT(dataCacheConnector.fetchAll(cacheId)) flatMap { cache =>
      ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key)) match {
        case Some((LPrLLP | LimitedCompany)) => OptionT.liftF(fn(cache))
        case _ => OptionT.pure[Future, Result](NotFound(notFoundView))
      }
    } getOrElse InternalServerError("Could not retrieve business type")
  }

  private def updateCache(cacheId: String, cache: CacheMap, data: CorporationTaxRegistered)(implicit hc: HeaderCarrier) = for {
    businessDetails <- OptionT.fromOption[Future](cache.getEntry[BusinessDetails](BusinessDetails.key))
    cacheMap <- OptionT.liftF(dataCacheConnector.save[BusinessDetails](cacheId, BusinessDetails.key, businessDetails.corporationTaxRegistered(data)))
  } yield cacheMap

  private def getRedirectLocation(edit: Boolean) = if (edit) {
    Redirect(routes.SummaryController.get())
  } else {
    Redirect(routes.ConfirmRegisteredOfficeController.get())
  }
}
