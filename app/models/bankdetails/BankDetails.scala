package models.bankdetails

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.StatusConstants

case class BankDetails (
                         bankAccountType: Option[BankAccountType] = None,
                         bankAccount: Option[BankAccount] = None,
                         hasChanged: Boolean = false,
                         status:Option[String] = None
                        ){

  def bankAccountType(v: Option[BankAccountType]): BankDetails = {
    v match {
      case None => this.copy(bankAccountType = None, hasChanged = hasChanged || this.bankAccountType.isEmpty)
      case _ => this.copy(bankAccountType = v, hasChanged = hasChanged || !this.bankAccountType.equals(v))
    }
  }
  def bankAccount(v: BankAccount): BankDetails = {
    this.copy(bankAccount = Some(v), hasChanged = hasChanged || !this.bankAccount.contains(v))
  }
  def isComplete: Boolean =
    this match {
      case BankDetails(Some(_), Some(_), _,_) => true
      case BankDetails(None, None, _,_) => true //This code part of fix for the issue AMLS-1549 back button issue
      case _ => false
    }
}

object BankDetails {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def anyChanged(newModel: Seq[BankDetails]): Boolean = {
    newModel exists { _.hasChanged }
  }

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "bankdetails"
    val notStarted = Section(messageKey, NotStarted, false, controllers.bankdetails.routes.BankAccountAddController.get(true))


    cache.getEntry[Seq[BankDetails]](key).fold(notStarted) {
      _.filterNot(_.status.contains(StatusConstants.Deleted)).filterNot(_ == BankDetails()) match {
        case Nil => notStarted
        case model if model.isEmpty => Section(messageKey, Completed, anyChanged(model), controllers.bankdetails.routes.SummaryController.get(true))
        case model if model forall { _.isComplete } => Section(messageKey, Completed, anyChanged(model),
          controllers.bankdetails.routes.SummaryController.get(true))
        case model =>
          val index = model.indexWhere {
            case model if !model.isComplete => true
            case _ => false
          }
          Section(messageKey, Started, anyChanged(model), controllers.bankdetails.routes.WhatYouNeedController.get(index + 1))
      }
    }
  }

  val key = "bank-details"

  implicit val mongoKey = new MongoKey[BankDetails] {
    override def apply(): String = "bank-details"
  }

  implicit val reads: Reads[BankDetails] = (
    (__ \ "bankAccountType").readNullable[BankAccountType] and
      (__ \ "bankAccount").readNullable[BankAccount] and
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
      (__ \ "status").readNullable[String]
    ) (BankDetails.apply _)

  implicit val writes: Writes[BankDetails] = Json.writes[BankDetails]

  implicit def default(details: Option[BankDetails]): BankDetails =
    details.getOrElse(BankDetails())
}


