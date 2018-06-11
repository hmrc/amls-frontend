/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.bankdetails

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import play.api.Logger
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.StatusConstants

case class BankDetails(
                        bankAccountType: Option[BankAccountType] = None,
                        accountName: Option[String] = None,
                        bankAccount: Option[Account] = None,
                        hasChanged: Boolean = false,
                        refreshedFromServer: Boolean = false,
                        status: Option[String] = None,
                        hasAccepted: Boolean = false
                      ) {

  def bankAccountType(v: Option[BankAccountType]): BankDetails = {
    v match {
      case None => this.copy(bankAccountType = None, hasChanged = hasChanged || this.bankAccountType.isDefined,
        hasAccepted = hasAccepted && this.bankAccountType.isEmpty)
      case _ =>
        this.copy(bankAccountType = v, hasChanged = hasChanged || !this.bankAccountType.equals(v),
          hasAccepted = hasAccepted && this.bankAccountType.equals(v))
    }
  }

  def bankAccount(value: Option[Account]): BankDetails = {
    this.copy(bankAccount = value, hasChanged = hasChanged || (this.bankAccount != value),
      hasAccepted = hasAccepted && this.bankAccount == value)
  }

  def isComplete: Boolean = this match {
    case BankDetails(Some(NoBankAccountUsed), _, None, _, _, _, accepted) => accepted
    case BankDetails(Some(_), Some(_), Some(_), _, _, _, accepted) => accepted
    case BankDetails(None, _, None, _, _, _, accepted) => accepted
    case _ => false
  }
}

object BankDetails {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._
  import utils.MappingUtils._

  implicit def maybeBankAccount(account: Account): Option[Account] = Some(account)

  def anyChanged(newModel: Seq[BankDetails]): Boolean = newModel exists { x => x.hasChanged }

  def section(implicit cache: CacheMap): Section = {
    Logger.debug(s"[BankDetails][section] $cache")

    val msgKey = "bankdetails"
    val defaultSection = Section(msgKey, NotStarted, false, controllers.bankdetails.routes.WhatYouNeedController.get(-1))

    cache.getEntry[Seq[BankDetails]](key).fold(defaultSection) { bds =>
      bds match {
        case model if model.isEmpty => Section(msgKey, Completed, false, controllers.bankdetails.routes.YourBankAccountsController.get())
        case model if model forall {
          _.isComplete
        } => Section(msgKey, Completed, anyChanged(bds), controllers.bankdetails.routes.YourBankAccountsController.get())
        case model => {
          Section(msgKey, Started, anyChanged(bds), controllers.bankdetails.routes.YourBankAccountsController.get())
        }
      }

    }

  }

  val key = "bank-details"

  implicit val mongoKey = new MongoKey[BankDetails] {
    override def apply(): String = "bank-details"
  }

  implicit val reads: Reads[BankDetails] = {

    def accountNameReader: Reads[Option[String]] = {
      (__ \ "accountName").readNullable[String] flatMap {
        case x@Some(_) => constant(x)
        case _ => (__ \ "bankAccount" \ "accountName").readNullable[String] orElse constant(None)
      }
    }

    (
      ((__ \ "bankAccountType").readNullable[BankAccountType] orElse __.read(Reads.optionNoError[BankAccountType])) ~
        accountNameReader ~
        ((__ \ "bankAccount").read[Account].map[Option[Account]](Some(_)) orElse __.read(Reads.optionNoError[Account])) ~
        (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) ~
        (__ \ "refreshedFromServer").readNullable[Boolean].map(_.getOrElse(false)) ~
        (__ \ "status").readNullable[String] ~
        (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
      ) (BankDetails.apply _)
  }

  implicit val writes: Writes[BankDetails] = Json.writes[BankDetails]

  implicit def default(details: Option[BankDetails]): BankDetails =
    details.getOrElse(BankDetails())
}