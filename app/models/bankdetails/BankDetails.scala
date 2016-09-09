package models.bankdetails

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class BankDetails (
                         bankAccountType: Option[BankAccountType] = None,
                         bankAccount: Option[BankAccount] = None,
                         hasChanged: Boolean = false
                        ){

  def bankAccountType(v: Option[BankAccountType]): BankDetails = {
    println(s"******* bankAccountType $v - $hasChanged")
    v match {
      case None => this.copy(bankAccountType = None, hasChanged = hasChanged || this.bankAccountType.isEmpty)
      case _ => this.copy(bankAccountType = v, hasChanged = hasChanged || !this.bankAccountType.equals(v))
    }
  }

  def bankAccount(v: BankAccount): BankDetails = {
    println(s"******* bankAccount $v - $hasChanged")
    val thing = this.copy(bankAccount = Some(v), hasChanged = hasChanged || !this.bankAccount.contains(v))
    println(s"********* $thing")
    thing
  }
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

  def anyChanged2(oldModel: Seq[BankDetails], newModel: Seq[BankDetails]): Boolean = {
    !(oldModel equals newModel)
  }

  def anyChanged(newModel: Seq[BankDetails]): Boolean = {
    newModel exists { _.hasChanged }
  }

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "bankdetails"
    val notStarted = Section(messageKey, NotStarted, false, controllers.bankdetails.routes.BankAccountAddController.get(true))

    val originalSeq = cache.getEntry[Seq[BankDetails]](key)
//    val hasOriginalSeqChanged = anyChanged2(originalSeq, )

      originalSeq.fold(notStarted) {
      case model: Seq[BankDetails] if model.isEmpty => {
        println(s"******* ${anyChanged(model)} 1 *********")
        Section(messageKey, Completed, anyChanged(model), controllers.bankdetails.routes.SummaryController.get(true))
      }
      case model if model forall {
        _.isComplete
      } => {
        println(s"******* ${anyChanged(model)} 2 *********")
        Section(messageKey, Completed, anyChanged(model), controllers.bankdetails.routes.SummaryController.get(true))
      }
      case model => {
        println(s"******* ${anyChanged(model)} 3 *********")
        val index = model.indexWhere {
          case m if !m.isComplete => true
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
      __.read[Option[BankAccountType]] and
      __.read[Option[BankAccount]] and
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false))
    ) (BankDetails.apply _)

  implicit val writes: Writes[BankDetails] = Writes[BankDetails] {
    model =>
      Seq(
        Json.toJson(model.bankAccountType).asOpt[JsObject],
        Json.toJson(model.bankAccount).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      } + ("hasChanged" -> JsBoolean(model.hasChanged))
  }

  implicit def default(details: Option[BankDetails]): BankDetails =
    details.getOrElse(BankDetails())
}


