package models.tradingpremises

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.StatusConstants

import scala.collection.Seq

case class TradingPremises(
                            registeringAgentPremises: Option[RegisteringAgentPremises] = None,
                            yourTradingPremises: Option[YourTradingPremises] = None,
                            businessStructure: Option[BusinessStructure] = None,
                            agentName: Option[AgentName] = None,
                            agentCompanyDetails: Option[AgentCompanyDetails] = None,
                            agentPartnership: Option[AgentPartnership] = None,
                            whatDoesYourBusinessDoAtThisAddress: Option[WhatDoesYourBusinessDo] = None,
                            msbServices: Option[MsbServices] = None,
                            hasChanged: Boolean = false,
                            lineId: Option[Int] = None,
                            status: Option[String] = None,
                            endDate: Option[ActivityEndDate] = None,
                            removalReason: Option[String] = None,
                            removalReasonOther: Option[String] = None
                          ) {

  def businessStructure(p: BusinessStructure): TradingPremises =
    this.copy(businessStructure = Some(p), hasChanged = hasChanged || !this.businessStructure.contains(p))

  def agentName(p: AgentName): TradingPremises =
    this.copy(agentName = Some(p), hasChanged = hasChanged || !this.agentName.contains(p))

  def agentCompanyDetails(p: AgentCompanyDetails): TradingPremises =
    this.copy(agentCompanyDetails = Some(p), hasChanged = hasChanged || !this.agentCompanyDetails.contains(p))

  def agentPartnership(p: AgentPartnership): TradingPremises =
    this.copy(agentPartnership = Some(p), hasChanged = hasChanged || !this.agentPartnership.contains(p))

  def yourTradingPremises(p: YourTradingPremises): TradingPremises =
    this.copy(yourTradingPremises = Some(p), hasChanged = hasChanged || !this.yourTradingPremises.contains(p))

  def whatDoesYourBusinessDoAtThisAddress(p: WhatDoesYourBusinessDo): TradingPremises =
    this.copy(whatDoesYourBusinessDoAtThisAddress = Some(p), hasChanged = hasChanged || !this.whatDoesYourBusinessDoAtThisAddress.contains(p))

  def msbServices(p: MsbServices): TradingPremises =
    this.copy(msbServices = Some(p), hasChanged = hasChanged || !this.msbServices.contains(p))

  def registeringAgentPremises(p: RegisteringAgentPremises): TradingPremises =
    this.copy(registeringAgentPremises = Some(p), hasChanged = hasChanged || !this.registeringAgentPremises.contains(p))

  def status(p: String): TradingPremises =
    this.copy(status = Some(p), hasChanged = hasChanged || !this.registeringAgentPremises.contains(p))

  def isComplete: Boolean =
    this match {
      case TradingPremises(_, Some(x), _, _, _, _, Some(_), _, _, _, _, _, _, _) => true
      case TradingPremises(_, _, Some(_), Some(_), Some(_), Some(_), Some(_), _, _, _, _, _, _, _) => true
      case TradingPremises(None, None, None, None, None, None, None, None, _, _, _, _, _, _) => true //This code part of fix for the issue AMLS-1549 back button issue
      case _ => false
    }
}

object TradingPremises {

  import play.api.libs.json._

  val key = "trading-premises"

  implicit val formatOption = Reads.optionWithNull[Seq[TradingPremises]]

  def anyChanged(newModel: Seq[TradingPremises]): Boolean = newModel exists { _.hasChanged }

  def section(implicit cache: CacheMap): Section = {

    val messageKey = "tradingpremises"
    val notStarted = Section(messageKey, NotStarted, false, controllers.tradingpremises.routes.TradingPremisesAddController.get())

    def filter(tp: Seq[TradingPremises]) = tp.filterNot(_.status.contains(StatusConstants.Deleted)).filterNot(_ == TradingPremises())

    cache.getEntry[Seq[TradingPremises]](key).fold(notStarted) { tp =>

      if (filter(tp).equals(Nil)) {
        Section(messageKey, NotStarted, anyChanged(tp), controllers.tradingpremises.routes.TradingPremisesAddController.get())
      } else {
        tp match {
          case premises if premises.nonEmpty && premises.forall {
            _.isComplete
          } => Section(messageKey, Completed, anyChanged(tp), controllers.tradingpremises.routes.SummaryController.answers())
          case _ => {
            val index = tp.indexWhere {
              case model if !model.isComplete => true
              case _ => false
            }
            Section(messageKey, Started, anyChanged(tp), controllers.tradingpremises.routes.WhatYouNeedController.get(index + 1))
          }
        }
      }

    }

  }

  implicit val mongoKey = new MongoKey[TradingPremises] {
    override def apply(): String = "trading-premises"
  }

  implicit val reads: Reads[TradingPremises] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    def backCompatibleReads[T](fieldName: String)(implicit rds: Reads[T]) = {
      (__ \ fieldName).read[T].map[Option[T]] {
        Some(_)
      } orElse __.read(Reads.optionNoError[T])
    }

    def readAgentCompanyDetails = {
      (__ \ "agentCompanyDetails").read[AgentCompanyDetails].map[Option[AgentCompanyDetails]] {
        Some(_)
      } orElse
        (__ \ "agentCompanyName").readNullable[AgentCompanyName].map[Option[AgentCompanyDetails]] {
          case Some(agc) => Some(AgentCompanyName(agc.agentCompanyName))
          case _ => None
        }
    }

    (
      backCompatibleReads[RegisteringAgentPremises]("registeringAgentPremises") and
        backCompatibleReads[YourTradingPremises]("yourTradingPremises") and
        backCompatibleReads[BusinessStructure]("businessStructure") and
        backCompatibleReads[AgentName]("agentName") and
        readAgentCompanyDetails and
        backCompatibleReads[AgentPartnership]("agentPartnership") and
        backCompatibleReads[WhatDoesYourBusinessDo]("whatDoesYourBusinessDoAtThisAddress") and
        backCompatibleReads[MsbServices]("msbServices") and
        (__ \ "hasChanged").readNullable[Boolean].map {
          _.getOrElse(false)
        } and
        (__ \ "lineId").readNullable[Int] and
        (__ \ "status").readNullable[String] and
        (__ \ "endDate").readNullable[ActivityEndDate] and
        (__ \ "removalReason").readNullable[String] and
        (__ \ "removalReasonOther").readNullable[String]
      ) apply TradingPremises.apply _
  }

  implicit val writes: Writes[TradingPremises] = Json.writes[TradingPremises]

  implicit def default(tradingPremises: Option[TradingPremises]): TradingPremises =
    tradingPremises.getOrElse(TradingPremises())
}
