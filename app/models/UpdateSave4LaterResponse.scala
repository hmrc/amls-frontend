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
import models.DataImport
import play.api.libs.json._
import scala.collection.Seq
import play.api.libs.functional.syntax._

case class UpdateSave4LaterResponse(dataImport: Option[DataImport],
                                    view: Option[ViewResponse],
                                    businessMatching: Option[BusinessMatching],
                                    estateAgencyBusiness: Option[EstateAgentBusiness],
                                    tradingPremises: Option[Seq[TradingPremises]],
                                    aboutTheBusiness: Option[AboutTheBusiness],
                                    bankDetails: Option[Seq[BankDetails]],
                                    addPerson: Option[AddPerson],
                                    businessActivities: Option[BusinessActivities],
                                    responsiblePeople: Option[Seq[ResponsiblePerson]],
                                    tcsp: Option[Tcsp],
                                    asp: Option[Asp],
                                    msb: Option[MoneyServiceBusiness],
                                    hvd: Option[Hvd],
                                    supervision: Option[Supervision],
                                    Subscription: Option[SubscriptionResponse],
                                    amendVariationResponse: Option[AmendVariationRenewalResponse]
                                   )

object UpdateSave4LaterResponse {

  import utils.MappingUtils.constant
  implicit val writes = Json.writes[UpdateSave4LaterResponse]

  def readLegacyField[T](key: String, oldKey: String)(implicit r: Reads[T]): Reads[Option[T]] =
      {
        (__ \ key).read[T] orElse (__ \ oldKey).read[T]
      }.map(Option(_)) orElse constant[Option[T]](None)

  implicit val reads: Reads[UpdateSave4LaterResponse] = {
    (
      (__ \ DataImport.key).readNullable[DataImport] ~
        readLegacyField[ViewResponse](ViewResponse.key, "view") ~
        readLegacyField[BusinessMatching](BusinessMatching.key, "businessMatching") ~
        readLegacyField[EstateAgentBusiness](EstateAgentBusiness.key, "estateAgencyBusiness") ~
        readLegacyField[Seq[TradingPremises]](TradingPremises.key, "tradingPremises") ~
        readLegacyField[AboutTheBusiness](AboutTheBusiness.key, "aboutTheBusiness") ~
        readLegacyField[Seq[BankDetails]](BankDetails.key, "bankDetails") ~
        readLegacyField[AddPerson](AddPerson.key, "addPerson") ~
        readLegacyField[BusinessActivities](BusinessActivities.key, "businessActivities") ~
        readLegacyField[Seq[ResponsiblePerson]](ResponsiblePerson.key, "responsiblePeople") ~
        (__ \ Tcsp.key).readNullable[Tcsp] ~
        (__ \ Asp.key).readNullable[Asp] ~
        (__ \ MoneyServiceBusiness.key).readNullable[MoneyServiceBusiness] ~
        (__ \ Hvd.key).readNullable[Hvd] ~
        (__ \ Supervision.key).readNullable[Supervision] ~
        (__ \ SubscriptionResponse.key).readNullable[SubscriptionResponse] ~
        (__ \ AmendVariationRenewalResponse.key).readNullable[AmendVariationRenewalResponse]
      ) (UpdateSave4LaterResponse.apply _)
  }
}


