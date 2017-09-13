/*
 * Copyright 2017 HM Revenue & Customs
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

import config.ApplicationConfig
import models.asp.Asp
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import play.api.Logger
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.StatusConstants

case class BankDetails(
                        bankAccountType: Option[BankAccountType] = None,
                        bankAccount: Option[BankAccount] = None,
                        hasChanged: Boolean = false,
                        refreshedFromServer: Boolean = false,
                        status: Option[String] = None,
                        hasAccepted: Boolean = false
                      ) {

  def bankAccountType(v: Option[BankAccountType]): BankDetails = {
    v match {
      case None => this.copy(bankAccountType = None, hasChanged = hasChanged || this.bankAccountType.isEmpty)
      case _ => this.copy(bankAccountType = v, hasChanged = hasChanged || !this.bankAccountType.equals(v))
    }
  }

  def bankAccount(value: Option[BankAccount]): BankDetails = {
    this.copy(bankAccount = value, hasChanged = hasChanged || (this.bankAccount != value))
  }

  def hasAccepted(value: Boolean): BankDetails = {
    this.copy(hasAccepted = value, hasChanged = hasChanged || (this.hasAccepted != value))
  }


  def isComplete: Boolean =
    this match {
      case BankDetails(Some(NoBankAccountUsed), None, _, _, _, true) if ApplicationConfig.hasAcceptedToggle => true
      case BankDetails(Some(NoBankAccountUsed), None, _, _, _, false) if ApplicationConfig.hasAcceptedToggle => false
      case BankDetails(Some(NoBankAccountUsed), None, _, _, _, _) => true
      case BankDetails(Some(_), Some(_), _, _, status, _) => true
      case BankDetails(None, None, _, _, _, _) => true //This code part of fix for the issue AMLS-1549 back button issue
      case _ => false
    }
}

object BankDetails {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit def maybeBankAccount(account: BankAccount): Option[BankAccount] = Some(account)

  def anyChanged(newModel: Seq[BankDetails]): Boolean = newModel exists { x => x.hasChanged || x.status.contains(StatusConstants.Deleted) }

  def section(implicit cache: CacheMap): Section = {
    Logger.debug(s"[BankDetails][section] $cache")

    def filter(bds: Seq[BankDetails]) = bds.filterNot(_.status.contains(StatusConstants.Deleted)).filterNot(_.equals(BankDetails()))

    val msgKey = "bankdetails"
    val defaultSection = Section(msgKey, NotStarted, false, controllers.bankdetails.routes.BankAccountAddController.get())

    cache.getEntry[Seq[BankDetails]](key).fold(defaultSection) { bds =>
      if (filter(bds).equals(Nil)) {
        Section(msgKey, NotStarted, anyChanged(bds), controllers.bankdetails.routes.BankAccountAddController.get())
      } else {
        bds match {
          case model if model.isEmpty => Section(msgKey, Completed, anyChanged(bds), controllers.bankdetails.routes.SummaryController.get(true))
          case model if model forall {
            _.isComplete
          } => Section(msgKey, Completed, anyChanged(bds), controllers.bankdetails.routes.SummaryController.get(true))
          case model => {
            val index = model.indexWhere {
              case bdModel if !bdModel.isComplete => true
              case _ => false
            }
            Section(msgKey, Started, anyChanged(bds), controllers.bankdetails.routes.WhatYouNeedController.get(index + 1))
          }
        }
      }
    }

  }

  val key = "bank-details"

  implicit val mongoKey = new MongoKey[BankDetails] {
    override def apply(): String = "bank-details"
  }

  implicit val reads: Reads[BankDetails] = (
    ((__ \ "bankAccountType").readNullable[BankAccountType] orElse __.read(Reads.optionNoError[BankAccountType])) ~
      ((__ \ "bankAccount").read[BankAccount].map[Option[BankAccount]](Some(_)) orElse __.read(Reads.optionNoError[BankAccount])) ~
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) ~
      (__ \ "refreshedFromServer").readNullable[Boolean].map(_.getOrElse(false)) ~
      (__ \ "status").readNullable[String] ~
      (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
    ) (BankDetails.apply _)


  implicit val writes: Writes[BankDetails] = Json.writes[BankDetails]

  implicit def default(details: Option[BankDetails]): BankDetails =
    details.getOrElse(BankDetails())
}


