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

package services

import config.ApplicationConfig
import connectors.DataCacheConnector

import javax.inject.{Inject, Singleton}
import models._
import models.amp.Amp
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessdetails.BusinessDetails
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.eab.Eab
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import play.api.libs.json.Format
import services.cache.Cache
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateMongoCacheService @Inject() (
  http: HttpClientV2,
  val cacheConnector: DataCacheConnector,
  val applicationConfig: ApplicationConfig
) {

  def update(credId: String, response: UpdateMongoCacheResponse)(implicit ex: ExecutionContext): Future[Any] =
    for {
      _ <- fn(credId, ViewResponse.key, response.view)
      _ <- fn(credId, BusinessMatching.key, response.businessMatching)
      _ <- fn(credId, TradingPremises.key, response.tradingPremises)
      _ <- fn(credId, BusinessActivities.key, response.businessActivities)
      _ <- fn(credId, Tcsp.key, response.tcsp)
      _ <- fn(credId, BankDetails.key, response.bankDetails)
      _ <- fn(credId, AddPerson.key, response.addPerson)
      _ <- fn(credId, ResponsiblePerson.key, response.responsiblePeople)
      _ <- fn(credId, Asp.key, response.asp)
      _ <- fn(credId, MoneyServiceBusiness.key, response.msb)
      _ <- fn(credId, Hvd.key, response.hvd)
      _ <- fn(credId, Amp.key, response.amp)
      _ <- fn(credId, Supervision.key, response.supervision)
      _ <- fn(credId, BusinessDetails.key, response.businessDetails)
      _ <- fn(credId, Eab.key, response.estateAgencyBusiness)
      _ <- fn(credId, SubscriptionResponse.key, response.Subscription)
      _ <- fn(credId, AmendVariationRenewalResponse.key, response.amendVariationResponse)
      _ <- fn(credId, DataImport.key, response.dataImport)
    } yield true

  def fn[T](credId: String, key: String, m: Option[T])(implicit fmt: Format[T]): Future[Cache] = m match {
    case Some(model) => cacheConnector.save[T](credId, key, model)
    case _           => Future.successful(Cache.empty)
  }

  def getMongoCacheData(
    fileName: String
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[UpdateMongoCacheResponse]] = {
    val requestUrl = url"${applicationConfig.amlsStubBaseUrl}/anti-money-laundering/update-mongoCache/$fileName"
    http
      .get(requestUrl)
      .execute[UpdateMongoCacheResponse]
      .map { r =>
        Some(r.copy(dataImport = Some(DataImport(fileName))))
      }
      .recover {
        case e: UpstreamErrorResponse if e.statusCode == 404 => None
        case e                                               => throw e
      }
  }
}
