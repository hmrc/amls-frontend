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

import models.bankdetails.Account._
import play.api.libs.json._

case class BankAccount(isUk: Option[BankAccountIsUk], hasIban: Option[BankAccountHasIban], account: Option[Account]) {

  def isUk(isUk: BankAccountIsUk): BankAccount = if (changedIsUk(isUk)) {
    this.copy(
      isUk = Option(isUk),
      hasIban = if (isUk.isUk) { None }
      else { hasIban },
      account = None
    )
  } else {
    this.copy(isUk = Option(isUk))
  }

  def hasIban(hasIban: BankAccountHasIban): BankAccount = if (changedHasIban(hasIban)) {
    this.copy(hasIban = Option(hasIban), account = None)
  } else {
    this.copy(hasIban = Option(hasIban))
  }

  def isComplete: Boolean = this match {
    case BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount(_, _)))                                   => true
    case BankAccount(Some(BankAccountIsUk(false)), Some(BankAccountHasIban(false)), Some(NonUKAccountNumber(_))) => true
    case BankAccount(Some(BankAccountIsUk(false)), Some(BankAccountHasIban(true)), Some(NonUKIBANNumber(_)))     => true
    case _                                                                                                       => false
  }

  def account(account: Account): BankAccount = this.copy(account = Option(account))

  private def changedIsUk(newData: BankAccountIsUk): Boolean =
    isUk.map(acc => acc.isUk).exists(isUk => isUk != newData.isUk)

  private def changedHasIban(newData: BankAccountHasIban): Boolean =
    hasIban.map(iban => iban.hasIban).exists(hasIban => hasIban != newData.hasIban)
}

object BankAccount {

  import play.api.libs.functional.syntax._

  implicit val jsonReads: Reads[BankAccount] = (
    __.read(Reads.optionNoError[BankAccountIsUk]) and
      __.read(Reads.optionNoError[BankAccountHasIban]) and
      __.read(Reads.optionNoError[Account])
  )(BankAccount.apply _)

  implicit val jsonWrites: Writes[BankAccount] = Writes { model: BankAccount =>
    Seq(
      Json.toJson(model.isUk).asOpt[JsObject],
      Json.toJson(model.hasIban).asOpt[JsObject],
      Json.toJson(model.account).asOpt[JsObject]
    ).flatten.fold(Json.obj()) {
      _ ++ _
    }
  }
}
