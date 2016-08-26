package models.bankdetails

import models.registrationprogress.{Completed, NotStarted, Section, Started}
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
    val notStarted = Section(messageKey, NotStarted, controllers.bankdetails.routes.BankAccountAddController.get(true))
    val complete = Section(messageKey, Completed, controllers.bankdetails.routes.SummaryController.get(true))
    cache.getEntry[Seq[BankDetails]](key).fold(notStarted) {
      case model if model.isEmpty => complete
      case model if model forall { _.isComplete } => complete
      case model =>
        val index = model.indexWhere {
          case model if !model.isComplete => true
          case _ => false
        }
        Section(messageKey, Started, controllers.bankdetails.routes.WhatYouNeedController.get(index + 1))
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


