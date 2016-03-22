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

}

object BankDetails {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "bankdetails"
    val incomplete = Section(messageKey, false, controllers.bankdetails.routes.WhatYouNeedController.get())
    cache.getEntry[IsComplete](key).fold(incomplete) {
      isComplete =>
        if (isComplete.isComplete) {
          Section(messageKey, true, controllers.bankdetails.routes.SummaryController.get())
        } else {
          incomplete
        }
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


