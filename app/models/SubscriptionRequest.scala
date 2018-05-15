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

package models

import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.estateagentbusiness.EstateAgentBusiness
import models.asp.Asp
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import play.api.libs.json.{JsArray, Json, Writes}

import scala.collection.Seq

case class SubscriptionRequest(
                                businessMatchingSection: Option[BusinessMatching],
                                eabSection: Option[EstateAgentBusiness],
                                tradingPremisesSection: Option[Seq[TradingPremises]],
                                aboutTheBusinessSection: Option[AboutTheBusiness],
                                bankDetailsSection: Option[Seq[BankDetails]],
                                aboutYouSection: Option[AddPerson],
                                businessActivitiesSection: Option[BusinessActivities],
                                responsiblePeopleSection: Option[Seq[ResponsiblePerson]],
                                tcspSection: Option[Tcsp],
                                aspSection: Option[Asp],
                                msbSection: Option[MoneyServiceBusiness],
                                hvdSection: Option[Hvd],
                                supervisionSection: Option[Supervision]
                              )

object SubscriptionRequest {

  implicit def tpSequenceWrites(implicit tradingPremisesWrites: Writes[TradingPremises]): Writes[Seq[TradingPremises]] = {
    Writes(x => JsArray(x.filterNot(_ == TradingPremises()).map {
      tp => tradingPremisesWrites.writes(tp)
    }))
  }

  implicit def rpSequenceWrites(implicit responsiblePeopleWrites: Writes[ResponsiblePerson]): Writes[Seq[ResponsiblePerson]] = {
    Writes(x => JsArray(x.filterNot(_ == ResponsiblePerson()).map {
      rp => responsiblePeopleWrites.writes(rp)
    }))
  }

  implicit val format = Json.format[SubscriptionRequest]
}
