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

package generators.bankdetails

import generators.BaseGenerator
import models.bankdetails.BankAccountType._
import models.bankdetails._
import org.scalacheck.Gen

// scalastyle:off magic.number
trait BankDetailsGenerator extends BaseGenerator {

  val accountGen: Gen[BankAccount] = for {
    accountNumber <- numSequence(10)
    sortCode      <- numSequence(6)
    iban          <- numSequence(15)
    account       <- Gen.oneOf(
                       Seq(
                         BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount(accountNumber, sortCode))),
                         BankAccount(
                           Some(BankAccountIsUk(false)),
                           Some(BankAccountHasIban(false)),
                           Some(NonUKAccountNumber(accountNumber))
                         ),
                         BankAccount(
                           Some(BankAccountIsUk(false)),
                           Some(BankAccountHasIban(true)),
                           Some(NonUKIBANNumber(iban))
                         )
                       )
                     )
  } yield account

  val accountTypeGenerator: Gen[BankAccountType] =
    Gen.oneOf(Seq(PersonalAccount, BelongsToBusiness, BelongsToOtherBusiness, NoBankAccountUsed))

  val bankDetailsGen: Gen[BankDetails] = for {
    accountType <- accountTypeGenerator
    name        <- stringOfLengthGen(10)
    account     <- accountGen
  } yield BankDetails(Some(accountType), Some(name), Some(account), hasAccepted = true)
}
