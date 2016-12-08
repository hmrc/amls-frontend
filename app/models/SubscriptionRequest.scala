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
import models.responsiblepeople.ResponsiblePeople
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
                                responsiblePeopleSection: Option[Seq[ResponsiblePeople]],
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

  implicit def rpSequenceWrites(implicit responsiblePeopleWrites: Writes[ResponsiblePeople]): Writes[Seq[ResponsiblePeople]] = {
    Writes(x => JsArray(x.filterNot(_ == ResponsiblePeople()).map {
      rp => responsiblePeopleWrites.writes(rp)
    }))
  }

  implicit val format = Json.format[SubscriptionRequest]
}
