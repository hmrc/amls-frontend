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

package models

import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import play.api.libs.json.Json

case class ViewResponse(
                         etmpFormBundleNumber:String,
                         businessMatchingSection: BusinessMatching,
                         eabSection: Option[EstateAgentBusiness],
                         tradingPremisesSection: Option[Seq[TradingPremises]],
                         aboutTheBusinessSection: AboutTheBusiness,
                         bankDetailsSection: Seq[BankDetails],
                         aboutYouSection: AddPerson,
                         businessActivitiesSection: BusinessActivities,
                         responsiblePeopleSection: Option[Seq[ResponsiblePeople]],
                         tcspSection: Option[Tcsp],
                         aspSection: Option[Asp],
                         msbSection: Option[MoneyServiceBusiness],
                         hvdSection: Option[Hvd],
                         supervisionSection: Option[Supervision]
                               )

object ViewResponse {

  val key = "Subscription"

  implicit val format = Json.format[ViewResponse]
}
