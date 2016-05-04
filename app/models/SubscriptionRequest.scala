package models

import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.{BusinessMatching, BusinessType}
import models.declaration.AddPerson
import models.estateagentbusiness.EstateAgentBusiness
import models.responsiblepeople.ResponsiblePeople
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
                                responsiblePeopleSection: Option[Seq[ResponsiblePeople]]
                              )

object SubscriptionRequest {
  implicit val format = Json.format[SubscriptionRequest]
}
