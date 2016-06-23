package models

import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.{BusinessMatching}
import models.declaration.AddPerson
import models.estateagentbusiness.EstateAgentBusiness
import models.asp.Asp
import models.moneyservicebusiness.MoneyServiceBusiness
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import play.api.libs.json.Json

case class SubscriptionRequest(
                                businessMatchingSection: Option[BusinessMatching],
                                eabSection: Option[EstateAgentBusiness],
                                tradingPremisesSection: Option[Seq[TradingPremises]],
                                aboutTheBusinessSection: Option[AboutTheBusiness],
                                bankDetailsSection: Option[Seq[BankDetails]],
                                aboutYouSection: Option[AddPerson],
                                businessActivitiesSection: Option[BusinessActivities],
                                responsiblePeopleSection: Option[Seq[ResponsiblePeople]],
                                tcspSection: Option[Tcsp],
                                aspSection: Option[Asp],
                                msbSection: Option[MoneyServiceBusiness],
                                supervisionSection:Option[Supervision]
                              )

object SubscriptionRequest {
  implicit val format = Json.format[SubscriptionRequest]
}
