/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import models.bankdetails.Account._

class BankAccountSpec extends PlaySpec with MockitoSugar {


  val ukAccount = BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("12341234", "000000")))
  val nonUkIban = BankAccount(Some(BankAccountIsUk(false)), Some(BankAccountHasIban(true)), Some( NonUKIBANNumber("ABCDEFGHIJKLMNOPQRSTUVWXYZABCD")))
  val nonUkAccount = BankAccount(Some(BankAccountIsUk(false)), Some(BankAccountHasIban(false)), Some( NonUKAccountNumber("ABCDEFGHIJKLMNOPQRSTUVWXYZABCD")))

  val ukAccountJson:String = """{
    "isUK" : true,
    "accountNumber" : "12341234",
    "sortCode" : "000000"
  }"""

  val nonUkIbanJson:String = """{
    "isUK" : false,
    "isIBAN" : true,
    "IBANNumber" : "ABCDEFGHIJKLMNOPQRSTUVWXYZABCD"
  }"""

  val nonUkAccountJson:String = """{
    "isUK" : false,
    "isIBAN" : false,
    "nonUKAccountNumber" : "ABCDEFGHIJKLMNOPQRSTUVWXYZABCD"
  }"""

  "BankAccount" must {
    "Serialise correctly" in {
      println(Json.prettyPrint(Json.toJson(ukAccount)))
      println(Json.prettyPrint(Json.toJson(nonUkAccount)))
      println(Json.prettyPrint(Json.toJson(nonUkIban)))
    }

    "Deserialise account correctly" in {
      import play.api.libs.json.Json
      println(Json.parse(ukAccountJson).as[Account])
      println(Json.parse(nonUkAccountJson).as[Account])
      println(Json.parse(nonUkIbanJson).as[Account])
    }

    "Deserialise Bank Account correctly" in {
      import play.api.libs.json.Json
//      println(Json.parse(ukAccountJson).as[BankAccount])
      println(Json.parse(nonUkAccountJson).as[BankAccount])
      println(Json.parse(nonUkIbanJson).as[BankAccount])
    }

    "Deserialise hasIBAN correctly" in {
      println(Json.parse(nonUkAccountJson).as[BankAccountHasIban])
      println(Json.parse(nonUkIbanJson).as[BankAccountHasIban])
    }

    "Deserialise isUk correctly" in {
      println(Json.parse(ukAccountJson).as[BankAccountIsUk])
      println(Json.parse(nonUkAccountJson).as[BankAccountIsUk])
      println(Json.parse(nonUkIbanJson).as[BankAccountIsUk])
    }
  }
}
