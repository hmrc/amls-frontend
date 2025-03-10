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
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.Seq

case class UpdateMongoCacheResponse(
  dataImport: Option[DataImport],
  view: Option[ViewResponse],
  businessMatching: Option[BusinessMatching],
  estateAgencyBusiness: Option[Eab],
  tradingPremises: Option[Seq[TradingPremises]],
  businessDetails: Option[BusinessDetails],
  bankDetails: Option[Seq[BankDetails]],
  addPerson: Option[AddPerson],
  businessActivities: Option[BusinessActivities],
  responsiblePeople: Option[Seq[ResponsiblePerson]],
  tcsp: Option[Tcsp],
  asp: Option[Asp],
  msb: Option[MoneyServiceBusiness],
  hvd: Option[Hvd],
  amp: Option[Amp],
  supervision: Option[Supervision],
  Subscription: Option[SubscriptionResponse],
  amendVariationResponse: Option[AmendVariationRenewalResponse]
)

object UpdateMongoCacheResponse {

  import utils.MappingUtils.constant
  implicit val writes: OWrites[UpdateMongoCacheResponse] = Json.writes[UpdateMongoCacheResponse]

  def readLegacyField[T](key: String, oldKey: String)(implicit r: Reads[T]): Reads[Option[T]] = {
    (__ \ key).read[T] orElse (__ \ oldKey).read[T]
  }.map(Option(_)) orElse constant[Option[T]](None)

  implicit val reads: Reads[UpdateMongoCacheResponse] =
    (
      (__ \ DataImport.key).readNullable[DataImport] ~
        readLegacyField[ViewResponse](ViewResponse.key, "view") ~
        readLegacyField[BusinessMatching](BusinessMatching.key, "businessMatching") ~
        readLegacyField[Eab](Eab.key, "estateAgencyBusiness") ~
        readLegacyField[Seq[TradingPremises]](TradingPremises.key, "tradingPremises") ~
        readLegacyField[BusinessDetails](BusinessDetails.key, "aboutTheBusiness") ~
        readLegacyField[Seq[BankDetails]](BankDetails.key, "bankDetails") ~
        readLegacyField[AddPerson](AddPerson.key, "addPerson") ~
        readLegacyField[BusinessActivities](BusinessActivities.key, "businessActivities") ~
        readLegacyField[Seq[ResponsiblePerson]](ResponsiblePerson.key, "responsiblePeople") ~
        (__ \ Tcsp.key).readNullable[Tcsp] ~
        (__ \ Asp.key).readNullable[Asp] ~
        (__ \ MoneyServiceBusiness.key).readNullable[MoneyServiceBusiness] ~
        (__ \ Hvd.key).readNullable[Hvd] ~
        (__ \ Amp.key).readNullable[Amp] ~
        (__ \ Supervision.key).readNullable[Supervision] ~
        (__ \ SubscriptionResponse.key).readNullable[SubscriptionResponse] ~
        (__ \ AmendVariationRenewalResponse.key).readNullable[AmendVariationRenewalResponse]
    )(UpdateMongoCacheResponse.apply _)
}
