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
import models.{UpdateSave4LaterResponse, ViewResponse}
import play.api.libs.json.{Format}
import play.api.mvc.Results.Ok
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateSave4LaterService @Inject()(http: HttpGet, val cacheConnector: DataCacheConnector) {

  def update(response: UpdateSave4LaterResponse)
            (implicit hc: HeaderCarrier, authContext: AuthContext, ex: ExecutionContext): Future[Any] = {

    Future.sequence(Seq(
      fn(ViewResponse.key, response.view),
      fn(BusinessMatching.key, response.businessMatching map{x => x.copy(hasChanged = true)}),
      fn(TradingPremises.key, response.tradingPremises),
      fn(BusinessActivities.key, response.businessActivities),
      fn(Tcsp.key, response.tcsp),
      fn(BankDetails.key, response.bankDetails),
      fn(AddPerson.key, response.addPerson),
      fn(ResponsiblePerson.key, response.responsiblePeople),
      fn(Asp.key, response.asp),
      fn(MoneyServiceBusiness.key, response.msb),
      fn(Hvd.key, response.hvd),
      fn(Supervision.key, response.supervision),
      fn(AboutTheBusiness.key, response.aboutTheBusiness),
      fn(EstateAgentBusiness.key, response.estateAgencyBusiness)
    )
    ) map { _ =>
      Ok
    }
  }

  def fn[T](key: String, m: Option[T])(implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[CacheMap] = m match {
    case Some(model) => cacheConnector.save[T](key, model)
    case _ => Future.successful(CacheMap("", Map.empty))
  }

  def getSaveForLaterData(fileName: String)(implicit hc: HeaderCarrier, ac: AuthContext, ex: ExecutionContext): Future[Option[UpdateSave4LaterResponse]] = {
    val requestUrl = s"http://localhost:8941/anti-money-laundering/saveforlater/getfile/${fileName}"
    http.GET[UpdateSave4LaterResponse](requestUrl)
      .map {Some(_)}
        .recover {
          case _:NotFoundException => None
        }
  }
}

