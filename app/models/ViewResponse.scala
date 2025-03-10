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

package models

import models.amp.Amp
import models.businessdetails.BusinessDetails
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.eab.Eab
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class ViewResponse(
  etmpFormBundleNumber: String,
  businessMatchingSection: BusinessMatching,
  eabSection: Option[Eab],
  tradingPremisesSection: Option[Seq[TradingPremises]],
  businessDetailsSection: BusinessDetails,
  bankDetailsSection: Seq[BankDetails],
  aboutYouSection: AddPerson,
  businessActivitiesSection: BusinessActivities,
  responsiblePeopleSection: Option[Seq[ResponsiblePerson]],
  tcspSection: Option[Tcsp],
  aspSection: Option[Asp],
  msbSection: Option[MoneyServiceBusiness],
  hvdSection: Option[Hvd],
  ampSection: Option[Amp],
  supervisionSection: Option[Supervision]
)

object ViewResponse {

  val key = "View"

  implicit val jsonWrites: OWrites[ViewResponse] = Json.writes[ViewResponse]

  def constructReads(
    etmpFormBundleNumber: String,
    businessMatchingSection: BusinessMatching,
    eabSection: Option[Eab],
    tradingPremisesSection: Option[Seq[TradingPremises]],
    businessDetailsSection: Option[BusinessDetails],
    oldBusinessDetailsSection: Option[BusinessDetails],
    bankDetailsSection: Seq[BankDetails],
    aboutYouSection: AddPerson,
    businessActivitiesSection: BusinessActivities,
    responsiblePeopleSection: Option[Seq[ResponsiblePerson]],
    tcspSection: Option[Tcsp],
    aspSection: Option[Asp],
    msbSection: Option[MoneyServiceBusiness],
    hvdSection: Option[Hvd],
    ampSection: Option[Amp],
    supervisionSection: Option[Supervision]
  ) = {

    def readBusinessDetails =
      businessDetailsSection.getOrElse(
        oldBusinessDetailsSection.getOrElse(
          throw new Exception("[ViewResponse][constructReads] - Invalid Index (businessDetailsSection)")
        )
      )

    apply(
      etmpFormBundleNumber,
      businessMatchingSection,
      eabSection,
      tradingPremisesSection,
      readBusinessDetails,
      bankDetailsSection,
      aboutYouSection,
      businessActivitiesSection,
      responsiblePeopleSection,
      tcspSection,
      aspSection,
      msbSection,
      hvdSection,
      ampSection,
      supervisionSection
    )
  }

  implicit val jsonReads: Reads[ViewResponse] = {
    (__ \ "etmpFormBundleNumber").read[String] and
      (__ \ "businessMatchingSection").read[BusinessMatching] and
      (__ \ "eabSection").readNullable[Eab] and
      (__ \ "tradingPremisesSection").readNullable[Seq[TradingPremises]] and
      (__ \ "businessDetailsSection").readNullable[BusinessDetails] and
      (__ \ "aboutTheBusinessSection").readNullable[BusinessDetails] and
      (__ \ "bankDetailsSection").read[Seq[BankDetails]] and
      (__ \ "aboutYouSection").read[AddPerson] and
      (__ \ "businessActivitiesSection").read[BusinessActivities] and
      (__ \ "responsiblePeopleSection").readNullable[Seq[ResponsiblePerson]] and
      (__ \ "tcspSection").readNullable[Tcsp] and
      (__ \ "aspSection").readNullable[Asp] and
      (__ \ "msbSection").readNullable[MoneyServiceBusiness] and
      (__ \ "hvdSection").readNullable[Hvd] and
      (__ \ "ampSection").readNullable[Amp] and
      (__ \ "supervisionSection").readNullable[Supervision]
  }.apply(ViewResponse.constructReads _)

  implicit val formatOption: Reads[Option[ViewResponse]] = Reads.optionWithNull[ViewResponse]

}
