package models.bankdetails

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class BankDetails (
                         bankAccountType: Option[BankAccountType] = None,
                         bankAccount: Option[BankAccount] = None,
                         hasChanged: Boolean = false
                        ){

  def bankAccountType(v: BankAccountType): BankDetails =
    this.copy(bankAccountType = Some(v), hasChanged = hasChanged || !this.bankAccountType.contains(v))

  def bankAccount(v: BankAccount): BankDetails =
    this.copy(bankAccount = Some(v), hasChanged = hasChanged || !this.bankAccount.contains(v))

  def isComplete: Boolean =
    this match {
      case BankDetails(Some(_), Some(_), _) => true
      case BankDetails(None, None, _) => true //This code part of fix for the issue AMLS-1549 back button issue
      case _ => false
    }
}

object BankDetails {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def anyChanged(model: Seq[BankDetails]): Boolean = model exists { _.hasChanged }

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "bankdetails"
    val notStarted = Section(messageKey, NotStarted, false, controllers.bankdetails.routes.BankAccountAddController.get(true))

    cache.getEntry[Seq[BankDetails]](key).fold(notStarted) {
      case model: Seq[BankDetails] if model.isEmpty =>
        Section(messageKey, Completed, anyChanged(model), controllers.bankdetails.routes.SummaryController.get(true))
      case model if model forall { _.isComplete } =>
        Section(messageKey, Completed, anyChanged(model), controllers.bankdetails.routes.SummaryController.get(true))
      case model =>
        val index = model.indexWhere {
          case m if !m.isComplete => true
          case _ => false
        }
        Section(messageKey, Started, anyChanged(model), controllers.bankdetails.routes.WhatYouNeedController.get(index + 1))
    }
  }

  val key = "bank-details"

  implicit val mongoKey = new MongoKey[BankDetails] {
    override def apply(): String = "bank-details"
  }

  implicit val reads: Reads[BankDetails] = (
      __.read[Option[BankAccountType]] and
      __.read[Option[BankAccount]] and
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false))
    ) (BankDetails.apply _)

  implicit val writes: Writes[BankDetails] = Writes[BankDetails] {
    model =>
      Seq(
        Json.toJson(model.bankAccountType).asOpt[JsObject],
        Json.toJson(model.bankAccount).asOpt[JsObject],
        Json.toJson(model.hasChanged).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(details: Option[BankDetails]): BankDetails =
    details.getOrElse(BankDetails())
}


