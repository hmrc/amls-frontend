/*
 * Copyright 2018 HM Revenue & Customs
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
import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import models._
import play.api.libs.json.{Format, Json}
import play.api.mvc.Results.Ok
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateSave4LaterService @Inject()(http: HttpGet, val cacheConnector: DataCacheConnector) {

  def update(response: UpdateSave4LaterResponse)
            (implicit hc: HeaderCarrier, authContext: AuthContext, ex: ExecutionContext): Future[Any] = {

    for {
      _ <- fn(ViewResponse.key, response.view)
      _ <- fn(BusinessMatching.key, response.businessMatching)
      _ <- fn(TradingPremises.key, response.tradingPremises)
      _ <- fn(BusinessActivities.key, response.businessActivities)
      _ <- fn(Tcsp.key, response.tcsp)
      _ <- fn(BankDetails.key, response.bankDetails)
      _ <- fn(AddPerson.key, response.addPerson)
      _ <- fn(ResponsiblePerson.key, response.responsiblePeople)
      _ <- fn(Asp.key, response.asp)
      _ <- fn(MoneyServiceBusiness.key, response.msb)
      _ <- fn(Hvd.key, response.hvd)
      _ <- fn(Supervision.key, response.supervision)
      _ <- fn(AboutTheBusiness.key, response.aboutTheBusiness)
      _ <- fn(EstateAgentBusiness.key, response.estateAgencyBusiness)
      _ <- fn(SubscriptionResponse.key, response.Subscription)
      _ <- fn(AmendVariationRenewalResponse.key, response.amendVariationResponse)
      _ <- fn(DataImport.key, response.dataImport)
    } yield true
  }

  def fn[T](key: String, m: Option[T])(implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[CacheMap] = m match {
    case Some(model) => cacheConnector.save[T](key, model)
    case _ => Future.successful(CacheMap("", Map.empty))
  }

  def getSaveForLaterData(fileName: String)(implicit hc: HeaderCarrier, ac: AuthContext, ex: ExecutionContext): Future[Option[UpdateSave4LaterResponse]] = {
    val requestUrl = s"${ApplicationConfig.save4LaterUpdateUrl}$fileName"

    http.GET[UpdateSave4LaterResponse](requestUrl)
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

