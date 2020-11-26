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
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateMongoCacheService @Inject()(http: HttpClient, val cacheConnector: DataCacheConnector, val applicationConfig: ApplicationConfig) {

  def update(credId: String, response: UpdateMongoCacheResponse)
            (implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Any] = {

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
  }

  def fn[T](credId: String, key: String, m: Option[T])(implicit hc: HeaderCarrier, fmt: Format[T]): Future[CacheMap] = m match {
    case Some(model) => cacheConnector.save[T](credId, key, model)
    case _ => Future.successful(CacheMap("", Map.empty))
  }

  def getMongoCacheData(fileName: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[UpdateMongoCacheResponse]] = {
    val requestUrl = s"${applicationConfig.mongoCacheUpdateUrl}$fileName"

    http.GET[UpdateMongoCacheResponse](requestUrl)
      .map { r =>
        import utils.Strings._
        println(Json.prettyPrint(Json.toJson(r)) in Console.YELLOW)
        Some(r.copy(dataImport = Some(DataImport(fileName)))) }
      .recover {
        case e => throw e
        case _: NotFoundException => None
      }
  }
}

