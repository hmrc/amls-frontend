package models

import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.estateagentbusiness.EstateAgentBusiness
import models.tradingpremises.TradingPremises
import play.api.libs.json.Json

case class SubscriptionRequest(
                                businessType: Option[String],
                                eabSection: Option[EstateAgentBusiness],
                                aboutTheBusinessSection: Option[AboutTheBusiness],
                                tradingPremisesSection: Option[Seq[TradingPremises]],
                                bankDetailsSection : Option[Seq[BankDetails]]
                              )

object SubscriptionRequest {
  implicit val format = Json.format[SubscriptionRequest]
}