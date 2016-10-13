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
