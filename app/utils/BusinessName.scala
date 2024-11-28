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

package utils

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import models.businessmatching.BusinessMatching
import play.api.Logging
import play.api.i18n.Messages
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

object BusinessName extends Logging {

  private val warn: String => Unit = msg => logger.warn(s"[BusinessName] $msg")

  def getNameFromCache(credId: String)(implicit cache: DataCacheConnector, ec: ExecutionContext): OptionT[Future, String] =
    for {
      bm <- OptionT(cache.fetch[BusinessMatching](credId, BusinessMatching.key))
      rd <- OptionT.fromOption[Future](bm.reviewDetails)
    } yield {
      // $COVERAGE-OFF$
      logger.debug(s"Found business name in cache: ${rd.businessName}")
      // $COVERAGE-ON$
      rd.businessName
    }

  def getNameFromAmls(accountTypeId: (String, String), safeId: String)
                     (implicit hc: HeaderCarrier, amlsConnector: AmlsConnector, ec: ExecutionContext): OptionT[Future, String] = {
    OptionT(amlsConnector.registrationDetails(accountTypeId, safeId) map { r =>
      Option(r.companyName)
    } recover {
      case ex =>
        warn(s"Call to registrationDetails failed: ${ex.getMessage}. Falling back to cache..")
        None
    })
  }

  def getName(credId: String, safeId: Option[String], accountTypeId: (String, String))
             (implicit hc: HeaderCarrier, ec: ExecutionContext, cache: DataCacheConnector, amls: AmlsConnector): OptionT[Future, String] =
    safeId.fold(getNameFromCache(credId))(v => getNameFromAmls(accountTypeId, v) orElse getNameFromCache(credId))

  def getBusinessNameFromAmls(amlsRegistrationNumber: Option[String], accountTypeId: (String, String), cacheId: String)
                             (implicit hc: HeaderCarrier, amls: AmlsConnector, ec: ExecutionContext,
                              dc: DataCacheConnector, statusService: StatusService, messages: Messages): OptionT[Future, String] = {
    for {
      (_, detailedStatus) <- OptionT.liftF(statusService.getDetailedStatus(amlsRegistrationNumber, accountTypeId, cacheId))
      businessName <- detailedStatus.fold[OptionT[Future, String]](OptionT.some("")) { r =>
        BusinessName.getName(cacheId, r.safeId, accountTypeId)
      } orElse OptionT.some("")
    } yield businessName
  }
}
