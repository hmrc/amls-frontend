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

package utils

import cats.implicits._
import cats.data.OptionT
import config.ApplicationConfig
import connectors.{AmlsConnector, DataCacheConnector}
import models.businessmatching.BusinessMatching
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

object BusinessName {

  def getNameFromCache(implicit hc: HeaderCarrier, ac: AuthContext, cache: DataCacheConnector, ec: ExecutionContext) =
    for {
      bm <- OptionT(cache.fetch[BusinessMatching](BusinessMatching.key))
      rd <- OptionT.fromOption[Future](bm.reviewDetails)
    } yield rd.businessName

  def getNameFromAmls(safeId: String)(implicit hc: HeaderCarrier, ac: AuthContext, amls: AmlsConnector, ec: ExecutionContext) =
    OptionT.liftF(amls.registrationDetails(safeId)) map {_.companyName}

  def getName(safeId: Option[String])(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext, cache: DataCacheConnector, amls: AmlsConnector) = {
    if (ApplicationConfig.businessNameLookup) {
      safeId.fold(getNameFromCache)(v => getNameFromAmls(v))
    } else {
      getNameFromCache
    }
  }

}
