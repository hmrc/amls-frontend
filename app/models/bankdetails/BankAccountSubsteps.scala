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

import play.api.libs.json._

case class BankAccountIsUk(isUk: Boolean)

case class BankAccountHasIban(hasIban: Boolean)

sealed trait Account

sealed trait NonUKAccount extends Account

case class UKAccount(accountNumber: String, sortCode: String) extends Account {

  def displaySortCode: String = {
    // scalastyle:off magic.number
    val pair1 = sortCode.substring(0, 2)
    val pair2 = sortCode.substring(2, 4)
    val pair3 = sortCode.substring(4, 6)
    // scalastyle:on magic.number
    pair1 + "-" + pair2 + "-" + pair3
  }
}

case class NonUKAccountNumber(accountNumber: String) extends NonUKAccount

case class NonUKIBANNumber(IBANNumber: String) extends NonUKAccount

object BankAccountIsUk {

  implicit val isUkJsonReads: Reads[BankAccountIsUk] = (__ \ "isUK").read[Boolean] map BankAccountIsUk.apply

  implicit val isUkJsonWrites: Writes[BankAccountIsUk] = Writes[BankAccountIsUk] { data =>
    Json.obj("isUK" -> data.isUk)
  }
}

object BankAccountHasIban {

  implicit val hasIbanJsonReads: Reads[BankAccountHasIban] = (__ \ "isIBAN").read[Boolean] map BankAccountHasIban.apply

  implicit val hasIbanJsonWrites: Writes[BankAccountHasIban] = Writes[BankAccountHasIban] { data =>
    Json.obj("isIBAN" -> data.hasIban)
  }
}

object Account {

  val ukJsonReads: Reads[Account] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "accountNumber").read[String] ~
        (__ \ "sortCode").read[String]
    )(UKAccount.apply _)
  }

  val ukJsonWrites = Writes[UKAccount] { data =>
    Json.obj("accountNumber" -> data.accountNumber, "sortCode" -> data.sortCode)
  }

  val nonUkAccountJsonWrites = Writes[NonUKAccountNumber] { data =>
    Json.obj("nonUKAccountNumber" -> data.accountNumber)
  }

  val nonUkAccountJsonReads: Reads[Account] = (__ \ "nonUKAccountNumber").read[String] map NonUKAccountNumber.apply

  val nonUkIbanJsonReads: Reads[Account] = (__ \ "IBANNumber").read[String] map NonUKIBANNumber.apply

  val nonUkIbanJsonWrites = Writes[NonUKIBANNumber](data => Json.obj("IBANNumber" -> data.IBANNumber))

  implicit val accountReads: Reads[Account] = ukJsonReads orElse nonUkIbanJsonReads orElse nonUkAccountJsonReads

  implicit val accountWrites: Writes[Account] = Writes[Account] {
    case account @ UKAccount(_, _)       => ukJsonWrites.writes(account)
    case account @ NonUKIBANNumber(_)    => nonUkIbanJsonWrites.writes(account)
    case account @ NonUKAccountNumber(_) => nonUkAccountJsonWrites.writes(account)
  }
}
