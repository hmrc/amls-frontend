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
import controllers.BaseController
import models.businessdetails.{BusinessDetails, CorporationTaxRegistered, CorporationTaxRegisteredYes}
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.ControllerHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

// This controller no longer has a vew or POST method. The UTR is acquired in BM and should be copied
// to Business Details only once pre-submission. API5 then populates this field from ETMP. The user
// should have no requirement to update it.
class CorporationTaxRegisteredController @Inject () (
                                                      val dataCacheConnector: DataCacheConnector,
                                                      val businessMatchingConnector: BusinessMatchingConnector,
                                                      val authConnector: AuthConnector
                                                    )extends BaseController {



  val failedResult = InternalServerError("Failed to update the business corporation tax number")

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        filterByBusinessType { cache =>
          cache.getEntry[BusinessDetails](BusinessDetails.key) match {
            case _ =>
              (for {
                bm <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
                details <- OptionT.fromOption[Future](bm.reviewDetails)
                // Only update Business Details from Business Matching where it exists; after that its maintained by API5
                _ <- if (details.utr.isDefined) {
                  updateCache(cache, CorporationTaxRegisteredYes(details.utr.getOrElse(
                    throw new Exception("[CorporationTaxRegisteredController][get]: Could not retrieve UTR from Business Matching")
                  )))
                } else {
                  OptionT.fromOption[Future](Some(cache))
                }
              } yield getRedirectLocation(edit)) getOrElse InternalServerError("Could not route from CorporationTaxRegisteredController")
          }
        }
  }

  private def filterByBusinessType(fn: CacheMap => Future[Result])(implicit hc:HeaderCarrier, ac:AuthContext, request: Request[_]): Future[Result] = {
    OptionT(dataCacheConnector.fetchAll) flatMap { cache =>
      ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key)) match {
        case Some((LPrLLP | LimitedCompany)) => OptionT.liftF(fn(cache))
        case _ => OptionT.pure[Future, Result](NotFound(notFoundView))
      }
    } getOrElse InternalServerError("Could not retrieve business type")
  }

  private def updateCache(cache: CacheMap, data: CorporationTaxRegistered)(implicit auth: AuthContext, hc: HeaderCarrier) = for {
    businessDetails <- OptionT.fromOption[Future](cache.getEntry[BusinessDetails](BusinessDetails.key))
    cacheMap <- OptionT.liftF(dataCacheConnector.save[BusinessDetails](BusinessDetails.key, businessDetails.corporationTaxRegistered(data)))
  } yield cacheMap

  private def getRedirectLocation(edit: Boolean) = if (edit) {
    Redirect(routes.SummaryController.get())
  } else {
    Redirect(routes.ConfirmRegisteredOfficeController.get())
  }
}
