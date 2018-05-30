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
import models.declaration.release7.RoleWithinBusinessRelease7
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import models.{UpdateSave4LaterResponse, ViewResponse}
import play.api.libs.json.Format
import play.api.mvc.Results.Ok
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateSave4LaterService @Inject()(val cacheConnector: DataCacheConnector) {

  def update(filename: String)
            (implicit hc: HeaderCarrier, authContext: AuthContext, ex: ExecutionContext): Future[Any] = {
    val updateSave4LaterResponse = getDataFromStubs(filename)

    Future.sequence(Seq(
      fn(ViewResponse.key, updateSave4LaterResponse.view),
      fn(BusinessMatching.key, updateSave4LaterResponse.businessMatching),
      fn(TradingPremises.key, updateSave4LaterResponse.tradingPremises),
      fn(BusinessActivities.key, updateSave4LaterResponse.businessActivities),
      fn(Tcsp.key, updateSave4LaterResponse.tcspSection),
      fn(BankDetails.key, updateSave4LaterResponse.bankDetails),
      fn(AddPerson.key, updateSave4LaterResponse.aboutYouSection),
      fn(ResponsiblePerson.key, updateSave4LaterResponse.responsiblePeopleSection),
      fn(Asp.key, updateSave4LaterResponse.aspSection),
      fn(MoneyServiceBusiness.key, updateSave4LaterResponse.msbSection),
      fn(Hvd.key, updateSave4LaterResponse.hvdSection),
      fn(Supervision.key, updateSave4LaterResponse.supervisionSection)
    )
    ) map { _ =>
      Ok
    }
  }

  def fn[T](key: String, m: Option[T])(implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[CacheMap] = m match {
    case Some(model) => cacheConnector.save[T](key, model)
    case _ => Future.successful(CacheMap("", Map.empty))
  }

  def getDataFromStubs(filename: String): UpdateSave4LaterResponse = {
    val x = UpdateSave4LaterResponse(
      Some(ViewResponse("test", BusinessMatching(), None, None, AboutTheBusiness(), Seq.empty, AddPerson("", None, "", RoleWithinBusinessRelease7(Set.empty)), BusinessActivities(), None, None, None, None, None, None)),
      Some(BusinessMatching(None, None, None, None, None, None, false, false, false)),
      None,
      Some(Seq(TradingPremises(None, None, None, None, None, None, None, None, false, None, None, None, None, None, false))),
      None,
      Some(Seq(BankDetails(None, None, None, false, false, None, false))),
      Some(AddPerson("", None, "", RoleWithinBusinessRelease7(Set.empty))),
      Some(BusinessActivities(None, None, None, None, None, None, None, None, None, None, None, None, None, None, false, false)),
      Some(Seq(ResponsiblePerson(None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, false, false, None, None, None, None))),
      Some(Tcsp(None, None, None, None, false, false)),
      Some(Asp(None, None, false, false)),
      Some(MoneyServiceBusiness(None, None, None, None, None, None, None, None, None, None, None, false, false)),
      Some(Hvd(None, None, None, None, None, None, None, None, None, false, false)),
      Some(Supervision(None, None, None, None, false, false)))
    x
  }

}