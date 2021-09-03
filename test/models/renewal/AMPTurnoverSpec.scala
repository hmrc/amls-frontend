/*
 * Copyright 2021 HM Revenue & Customs
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

package models.renewal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import models.amp.Amp
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

import scala.collection.Seq

class AMPTurnoverSpec extends PlaySpec {

  "AMPTurnover" should {
    "Form Validation" must {

      "successfully validate given an enum value" in {

        AMPTurnover.formRule.validate(Map("percentageExpectedTurnover" -> Seq("01"))) must
          be(Valid(AMPTurnover.First))

        AMPTurnover.formRule.validate(Map("percentageExpectedTurnover" -> Seq("02"))) must
          be(Valid(AMPTurnover.Second))

        AMPTurnover.formRule.validate(Map("percentageExpectedTurnover" -> Seq("03"))) must
          be(Valid(AMPTurnover.Third))

        AMPTurnover.formRule.validate(Map("percentageExpectedTurnover" -> Seq("04"))) must
          be(Valid(AMPTurnover.Fourth))

        AMPTurnover.formRule.validate(Map("percentageExpectedTurnover" -> Seq("05"))) must
          be(Valid(AMPTurnover.Fifth))

      }

      "write correct data from enum value" in {

        AMPTurnover.formWrites.writes(AMPTurnover.First) must
          be(Map("percentageExpectedTurnover" -> Seq("01")))

        AMPTurnover.formWrites.writes(AMPTurnover.Second) must
          be(Map("percentageExpectedTurnover" -> Seq("02")))

        AMPTurnover.formWrites.writes(AMPTurnover.Third) must
          be(Map("percentageExpectedTurnover" -> Seq("03")))

        AMPTurnover.formWrites.writes(AMPTurnover.Fourth) must
          be(Map("percentageExpectedTurnover" -> Seq("04")))

        AMPTurnover.formWrites.writes(AMPTurnover.Fifth) must
          be(Map("percentageExpectedTurnover" -> Seq("05")))

      }


      "throw error on invalid data" in {
        AMPTurnover.formRule.validate(Map("percentageExpectedTurnover" -> Seq("20"))) must
          be(Invalid(Seq((Path \ "percentageExpectedTurnover", Seq(ValidationError("error.invalid"))))))
      }

      "throw error on empty data" in {
        AMPTurnover.formRule.validate(Map.empty) must
          be(Invalid(Seq((Path \ "percentageExpectedTurnover", Seq(ValidationError("error.required.renewal.amp.percentage"))))))
      }
    }

    "JSON validation" must {

      "successfully validate given an enum value" in {

        Json.fromJson[AMPTurnover](Json.obj("percentageExpectedTurnover" -> "01")) must
          be(JsSuccess(AMPTurnover.First, JsPath ))

        Json.fromJson[AMPTurnover](Json.obj("percentageExpectedTurnover" -> "02")) must
          be(JsSuccess(AMPTurnover.Second, JsPath ))

        Json.fromJson[AMPTurnover](Json.obj("percentageExpectedTurnover" -> "03")) must
          be(JsSuccess(AMPTurnover.Third, JsPath ))

        Json.fromJson[AMPTurnover](Json.obj("percentageExpectedTurnover" -> "04")) must
          be(JsSuccess(AMPTurnover.Fourth, JsPath ))

        Json.fromJson[AMPTurnover](Json.obj("percentageExpectedTurnover" -> "05")) must
          be(JsSuccess(AMPTurnover.Fifth, JsPath ))

      }

      "write the correct value" in {

        Json.toJson(AMPTurnover.First.asInstanceOf[AMPTurnover]) must
          be(Json.obj("percentageExpectedTurnover" -> "01"))

        Json.toJson(AMPTurnover.Second.asInstanceOf[AMPTurnover]) must
          be(Json.obj("percentageExpectedTurnover" -> "02"))

        Json.toJson(AMPTurnover.Third.asInstanceOf[AMPTurnover]) must
          be(Json.obj("percentageExpectedTurnover" -> "03"))

        Json.toJson(AMPTurnover.Fourth.asInstanceOf[AMPTurnover]) must
          be(Json.obj("percentageExpectedTurnover" -> "04"))

        Json.toJson(AMPTurnover.Fifth.asInstanceOf[AMPTurnover]) must
          be(Json.obj("percentageExpectedTurnover" -> "05"))

      }

      "throw error for invalid data" in {
        Json.fromJson[AMPTurnover](Json.obj("percentageExpectedTurnover" -> "20")) must
          be(JsError(JsPath , play.api.libs.json.JsonValidationError("error.invalid")))
      }
    }

    "Convert amp section data" in {
      val ampData = Amp(Json.obj(
        "typeOfParticipant"      -> Seq("artGalleryOwner"),
        "soldOverThreshold"             -> true,
        "dateTransactionOverThreshold"  -> LocalDate.now.toString,
        "identifyLinkedTransactions"    -> true,
        "percentageExpectedTurnover"    -> "zeroToTwenty"
      ))
      val newAmpData = Json.obj(
        "typeOfParticipant"      -> Seq("artGalleryOwner"),
        "soldOverThreshold"             -> true,
        "dateTransactionOverThreshold"  -> LocalDate.now.toString,
        "identifyLinkedTransactions"    -> true,
        "percentageExpectedTurnover"    -> "twentyOneToForty"
      )

      AMPTurnover.convert(Some(AMPTurnover.Second), ampData) must be(Amp(newAmpData))
    }

  }
}
