package models.bankdetails

import models.registrationprogress.{IsComplete, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class BankDetails (
                         bankAccountType: Option[BankAccountType] = None,
                         bankAccount: Option[BankAccount] = None
                        ){

  def bankAccountType(v: BankAccountType): BankDetails =
    this.copy(bankAccountType = Some(v))

  def bankAccount(v: BankAccount): BankDetails =
    this.copy(bankAccount = Some(v))

  def isComplete: Boolean =
    this match {
      case BankDetails(Some(_), Some(_)) => true
      case _ => false
    }
}

object BankDetails {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "bankdetails"
    val incomplete = Section(messageKey, false, controllers.bankdetails.routes.WhatYouNeedController.get())
    val complete = Section(messageKey, true, controllers.bankdetails.routes.SummaryController.get())
    cache.getEntry[Seq[BankDetails]](key).fold(incomplete) {
      case model if model.isEmpty => complete
      case model if model forall { _.isComplete } => complete
      case _ => incomplete
    }
  }

  val key = "bank-details"

  implicit val mongoKey = new MongoKey[BankDetails] {
    override def apply(): String = "bank-details"
  }

  implicit val reads: Reads[BankDetails] = (
      __.read[Option[BankAccountType]] and
      __.read[Option[BankAccount]]
    ) (BankDetails.apply _)

  implicit val writes: Writes[BankDetails] = Writes[BankDetails] {
    model =>
      Seq(
        Json.toJson(model.bankAccountType).asOpt[JsObject],
        Json.toJson(model.bankAccount).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(details: Option[BankDetails]): BankDetails =
    details.getOrElse(BankDetails())
}


