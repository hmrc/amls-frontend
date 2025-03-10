/*
 * Copyright 2024 HM Revenue & Customs
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

import models.bankdetails.BankAccountType.NoBankAccountUsed
import models.registrationprogress._
import play.api.i18n.Messages
import typeclasses.MongoKey
import services.cache.Cache
import utils.StatusConstants

case class BankDetails(
  bankAccountType: Option[BankAccountType] = None,
  accountName: Option[String] = None,
  bankAccount: Option[BankAccount] = None,
  hasChanged: Boolean = false,
  refreshedFromServer: Boolean = false,
  status: Option[String] = None,
  hasAccepted: Boolean = false
) {

  def bankAccountType(v: Option[BankAccountType]): BankDetails =
    v match {
      case None =>
        this.copy(
          bankAccountType = None,
          hasChanged = hasChanged || this.bankAccountType.isDefined,
          hasAccepted = hasAccepted && this.bankAccountType.isEmpty
        )
      case _    =>
        this.copy(
          bankAccountType = v,
          hasChanged = hasChanged || !this.bankAccountType.equals(v),
          hasAccepted = hasAccepted && this.bankAccountType.equals(v)
        )
    }

  def bankAccount(value: Option[BankAccount]): BankDetails =
    this.copy(
      bankAccount = value,
      hasChanged = hasChanged || (this.bankAccount != value),
      hasAccepted = hasAccepted && this.bankAccount == value
    )

  def isBankAccountComplete(ba: BankAccount) =
    ba.isComplete

  def isComplete: Boolean = this match {
    case details if details.status.contains(StatusConstants.Deleted)                           => true
    case BankDetails(None, None, None, false, false, None, false)                              => true
    case BankDetails(Some(NoBankAccountUsed), _, None, _, _, _, accepted)                      => accepted
    case BankDetails(Some(_), Some(_), Some(a), _, _, _, accepted) if isBankAccountComplete(a) => accepted
    case BankDetails(None, _, None, _, _, _, accepted)                                         => accepted
    case _                                                                                     => false
  }
}

object BankDetails {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._
  import utils.MappingUtils._

  def anyChanged(newModel: Seq[BankDetails]): Boolean = newModel exists { x => x.hasChanged }

  def getBankAccountDescription(bankDetails: BankDetails)(implicit messages: Messages): String =
    (bankDetails.bankAccountType, bankDetails.bankAccount) match {
      case (Some(baType), Some(BankAccount(Some(BankAccountIsUk(true)), _, _)))  =>
        messages("bankdetails.accounttype.uk.lbl." + baType.getBankAccountTypeID)
      case (Some(baType), Some(BankAccount(Some(BankAccountIsUk(false)), _, _))) =>
        messages("bankdetails.accounttype.nonuk.lbl." + baType.getBankAccountTypeID)
      case (Some(baType), _)                                                     =>
        messages("bankdetails.accounttype.lbl." + baType.getBankAccountTypeID)
      case _                                                                     => ""
    }

  def taskRow(implicit cache: Cache, messages: Messages) = {

    val messageKey = "bankdetails"
    val notStarted = TaskRow(
      messageKey,
      controllers.bankdetails.routes.WhatYouNeedController.get.url,
      hasChanged = false,
      NotStarted,
      TaskRow.notStartedTag
    )

    cache.getEntry[Seq[BankDetails]](key).fold(notStarted) {
      case model if model.isEmpty                                         =>
        TaskRow(
          messageKey,
          controllers.bankdetails.routes.YourBankAccountsController.get().url,
          hasChanged = false,
          Completed,
          TaskRow.completedTag
        )
      case bds @ model if model.forall(_.isComplete) && anyChanged(model) =>
        TaskRow(
          messageKey,
          controllers.bankdetails.routes.YourBankAccountsController.get().url,
          true,
          Updated,
          TaskRow.updatedTag
        )
      case bds @ model if model forall {
            _.isComplete
          } =>
        TaskRow(
          messageKey,
          controllers.bankdetails.routes.YourBankAccountsController.get().url,
          anyChanged(bds),
          Completed,
          TaskRow.completedTag
        )
      case bds @ _                                                        =>
        TaskRow(
          messageKey,
          controllers.bankdetails.routes.YourBankAccountsController.get().url,
          anyChanged(bds),
          Started,
          TaskRow.incompleteTag
        )
    }
  }

  val key = "bank-details"

  implicit val mongoKey: MongoKey[BankDetails] = new MongoKey[BankDetails] {
    override def apply(): String = "bank-details"
  }

  implicit val reads: Reads[BankDetails] = {

    def accountNameReader: Reads[Option[String]] =
      (__ \ "accountName").readNullable[String] flatMap {
        case x @ Some(_) => constant(x)
        case _           => (__ \ "bankAccount" \ "accountName").readNullable[String] orElse constant(None)
      }

    (
      ((__ \ "bankAccountType").readNullable[BankAccountType] orElse __.read(Reads.optionNoError[BankAccountType])) ~
        accountNameReader ~
        ((__ \ "bankAccount")
          .read[BankAccount]
          .map[Option[BankAccount]](Some(_)) orElse __.read(Reads.optionNoError[BankAccount]).map {
          case Some(BankAccount(None, None, None)) => None
          case x                                   => x
        }) ~
        (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) ~
        (__ \ "refreshedFromServer").readNullable[Boolean].map(_.getOrElse(false)) ~
        (__ \ "status").readNullable[String] ~
        (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
    )(BankDetails.apply _)
  }

  implicit val writes: Writes[BankDetails] = Json.writes[BankDetails]

  implicit def default(details: Option[BankDetails]): BankDetails =
    details.getOrElse(BankDetails())

  object Filters {
    val deletedFilter       = (bd: BankDetails) => bd.status.contains(StatusConstants.Deleted)
    val completeFilter      = (bd: BankDetails) => bd.isComplete
    val noBankAccountFilter = (bd: BankDetails) => bd.bankAccountType.contains(NoBankAccountUsed)

    val emptyModelFilter: BankDetails => Boolean = {
      case BankDetails(None, None, None, _, _, _, _) => true
      case _                                         => false
    }

    val visibleAccountsFilter = (bd: BankDetails) =>
      !deletedFilter(bd) && !noBankAccountFilter(bd) && !emptyModelFilter(bd)

    implicit class ZippedSyntax(zipped: Seq[(BankDetails, Int)]) {
      def visibleAccounts    = zipped filter { case (bd, _) => visibleAccountsFilter(bd) }
      def completeAccounts   = zipped filter { case (bd, _) => completeFilter(bd) }
      def incompleteAccounts = zipped filterNot { case (bd, _) => completeFilter(bd) }
    }
  }
}
