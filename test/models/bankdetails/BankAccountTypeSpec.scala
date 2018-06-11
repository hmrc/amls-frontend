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

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play. PlaySpec
import jto.validation.{Path, Invalid, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class BankAccountTypeSpec extends PlaySpec with MockitoSugar {

  "Bank account types form" must {
    "fail to validate" when {
      "an invalid selection is made for the type of bank account the business uses" in {
        val urlFormEncoded = Map(
          "bankAccountType" -> Seq("")
        )

        BankAccountType.formReads.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "bankAccountType") -> Seq(ValidationError("error.invalid"))
          )))
      }

      "no selection is made for the type of bank account the business uses" in {
        val urlFormEncoded = Map(
          "bankAccountType" -> Seq()
        )

        BankAccountType.formReads.validate(urlFormEncoded) must
          be(Invalid(Seq(
            (Path \ "bankAccountType") -> Seq(ValidationError("error.bankdetails.accounttype"))
          )))
      }

    }
  }

  "BankAccountType" must {

    "return the correct type id" in {
      PersonalAccount.getBankAccountTypeID must be("01")
      BelongsToBusiness.getBankAccountTypeID must be("02")
      BelongsToOtherBusiness.getBankAccountTypeID must be("03")
      NoBankAccountUsed.getBankAccountTypeID must be("04")
    }

    "successfully validate form Read" in {
      BankAccountType.formReads.validate(Map("bankAccountType" -> Seq("01"))) must be (Valid(Some(PersonalAccount)))
      BankAccountType.formReads.validate(Map("bankAccountType" -> Seq("02"))) must be (Valid(Some(BelongsToBusiness)))
      BankAccountType.formReads.validate(Map("bankAccountType" -> Seq("03"))) must be (Valid(Some(BelongsToOtherBusiness)))
      BankAccountType.formReads.validate(Map("bankAccountType" -> Seq("04"))) must be (Valid(Some(NoBankAccountUsed)))

    }

    "fail on invalid selection" in {
        BankAccountType.formReads.validate(Map("bankAccountType" -> Seq("05"))) must be(Invalid(Seq(
            (Path \ "bankAccountType") -> Seq(ValidationError("error.invalid"))
          )))
    }

    "successfully write form data" in {
      BankAccountType.formWrites.writes(Some(PersonalAccount)) must be (Map("bankAccountType" -> Seq("01")))
      BankAccountType.formWrites.writes(Some(BelongsToBusiness)) must be (Map("bankAccountType" -> Seq("02")))
      BankAccountType.formWrites.writes(Some(BelongsToOtherBusiness)) must be (Map("bankAccountType" -> Seq("03")))
      BankAccountType.formWrites.writes(Some(NoBankAccountUsed)) must be (Map("bankAccountType" -> Seq("04")))
    }

    "validate Json read" in {
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" -> "01")) must
        be (JsSuccess(PersonalAccount, JsPath))
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" -> "02")) must
        be (JsSuccess(BelongsToBusiness, JsPath))
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" -> "03")) must
        be (JsSuccess(BelongsToOtherBusiness, JsPath))
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" -> "04")) must
        be (JsSuccess(NoBankAccountUsed, JsPath))
    }

    "fail Json read on invalid data" in  {
      Json.fromJson[BankAccountType](Json.obj("bankAccountType" ->"10")) must
        be (JsError(JsPath, play.api.data.validation.ValidationError("error.invalid")))
    }

    "write correct Json value" in  {
      Json.toJson(PersonalAccount) must be (Json.obj("bankAccountType" -> "01"))
      Json.toJson(BelongsToBusiness) must be (Json.obj("bankAccountType" -> "02"))
      Json.toJson(BelongsToOtherBusiness) must be (Json.obj("bankAccountType" -> "03"))
      Json.toJson(NoBankAccountUsed) must be (Json.obj("bankAccountType" -> "04"))
    }
  }

}
