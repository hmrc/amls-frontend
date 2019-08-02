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

package utils

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import models.businessmatching.BusinessMatching
import play.api.Logger
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

object BusinessName {

  private val warn: String => Unit = msg => Logger.warn(s"[BusinessName] $msg")

  @deprecated("To be removed when auth implementation is complete")
  def getNameFromCache(implicit hc: HeaderCarrier, ac: AuthContext, cache: DataCacheConnector, ec: ExecutionContext): OptionT[Future, String] =
    for {
      bm <- OptionT(cache.fetch[BusinessMatching](BusinessMatching.key))
      rd <- OptionT.fromOption[Future](bm.reviewDetails)
    } yield {
      Logger.debug(s"Found business name in cache: ${rd.businessName}")
      rd.businessName
    }

  def getNameFromCache(credId: String)(implicit hc: HeaderCarrier, cache: DataCacheConnector, ec: ExecutionContext): OptionT[Future, String] =
    for {
      bm <- OptionT(cache.fetch[BusinessMatching](credId, BusinessMatching.key))
      rd <- OptionT.fromOption[Future](bm.reviewDetails)
    } yield {
      Logger.debug(s"Found business name in cache: ${rd.businessName}")
      rd.businessName
    }

  @deprecated("To be removed when auth implementation is complete")
  def getNameFromAmls(safeId: String)
                     (implicit hc: HeaderCarrier, ac: AuthContext, amls: AmlsConnector, ec: ExecutionContext, dc: DataCacheConnector) = {
    OptionT(amls.registrationDetails(safeId) map { r =>
      Option(r.companyName)
    } recover {
      case ex =>
        warn(s"Call to registrationDetails failed: ${ex.getMessage}. Falling back to cache..")
        None
    })
  }

  def getNameFromAmls(safeId: String, accountTypeId: (String, String))
                     (implicit hc: HeaderCarrier, amls: AmlsConnector, ec: ExecutionContext, dc: DataCacheConnector) = {
    OptionT(amls.registrationDetails(safeId, accountTypeId) map { r =>
      Option(r.companyName)
    } recover {
      case ex =>
        warn(s"Call to registrationDetails failed: ${ex.getMessage}. Falling back to cache..")
        None
    })
  }

  @deprecated("To be removed when auth implementation is complete")
  def getName(safeId: Option[String])
             (implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext, cache: DataCacheConnector, amls: AmlsConnector) =
    safeId.fold(getNameFromCache)(v => getNameFromAmls(v) orElse getNameFromCache)

  def getName(credId: String, safeId: Option[String], accountTypeId: (String, String))
             (implicit hc: HeaderCarrier, ec: ExecutionContext, cache: DataCacheConnector, amls: AmlsConnector) =
    safeId.fold(getNameFromCache(credId))(v => getNameFromAmls(v, accountTypeId: (String, String)) orElse getNameFromCache(credId))

  def getBusinessNameFromAmls()(implicit hc: HeaderCarrier,
                                context: AuthContext,
                                amls: AmlsConnector,
                                ec: ExecutionContext,
                                dc: DataCacheConnector,
                                statusService: StatusService) = {
    for {
      (_, detailedStatus) <- OptionT.liftF(statusService.getDetailedStatus)
      businessName <- detailedStatus.fold[OptionT[Future, String]](OptionT.some("")) { r =>
        BusinessName.getName(r.safeId)
      } orElse OptionT.some("")
    } yield businessName
  }
}
