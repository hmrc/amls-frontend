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

package models.tradingpremises

import cats.data.Validated.Invalid
import models.DateOfChange
import org.joda.time.LocalDate
import org.scalatest.{MustMatchers, WordSpec}
import jto.validation.forms.UrlFormEncoded
import jto.validation._
import play.api.libs.json._
import views.html.include.forms2.date

class YourTradingPremisesSpec extends WordSpec with MustMatchers {

  val data = Map(
    "tradingName" -> Seq("foo"),
    "addressLine1" -> Seq("1"),
    "addressLine2" -> Seq("2"),
    "postcode" -> Seq("AA11 1AA"),
    "isResidential" -> Seq("true"),
    "startDate.day" -> Seq("24"),
    "startDate.month" -> Seq("2"),
    "startDate.year" -> Seq("1990")
  )

  val model = YourTradingPremises(
    "foo",
    Address(
      "1",
      "2",
      None,
      None,
      "AA11 1AA"
    ),
    Some(true),
    Some(new LocalDate(1990, 2, 24))
  )

  "YourTradingPremises" must {

    "return valid response when isResidential and startDate are empty" in {
      YourTradingPremises.formR.validate(Map("tradingName" -> Seq("foo"),
        "addressLine1" -> Seq("1"),
        "addressLine2" -> Seq("2"),
        "postcode" -> Seq("AA11 1AA")
      )) must be (Valid(YourTradingPremises("foo",Address("1","2",None,None,"AA11 1AA",None),None,None,None)))

    }

    "fail validation when trading name is exceeds maxlength" in {
      YourTradingPremises.formR.validate(Map("tradingName" -> Seq("foooo"*50),
        "addressLine1" -> Seq("1"),
        "addressLine2" -> Seq("2"),
        "postcode" -> Seq("AA11 1AA"),
        "isResidential" -> Seq("true"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("02"),
        "startDate.year" -> Seq("1990"))) must be (Invalid(Seq(Path \ "tradingName"  -> Seq(ValidationError("error.invalid.tp.trading.name"))
      )))
    }

    "fail validation" when {
      "given a future date" in {

        val data = YourTradingPremises.formW.writes(model.copy( startDate = Some(LocalDate.now().plusDays(1))))
        YourTradingPremises.formR.validate(data) must be(Valid(YourTradingPremises("foo",Address("1","2",None,None,"AA11 1AA",None),Some(true),Some(LocalDate.now().plusDays(1)),None)))
0

      }
    }

    "fail validation when trading name is empty" in {
      YourTradingPremises.formR.validate(Map("tradingName" -> Seq(""),
        "addressLine1" -> Seq("1"),
        "addressLine2" -> Seq("2"),
        "postcode" -> Seq("AA11 1AA"),
        "isResidential" -> Seq("true"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("02"),
        "startDate.year" -> Seq("1990"))) must be (Invalid(Seq(Path \ "tradingName"  -> Seq(ValidationError("error.required.tp.trading.name"))
      )))
    }

    "fail validation when trading name contains only spaces" in {
      YourTradingPremises.formR.validate(Map("tradingName" -> Seq("   "),
        "addressLine1" -> Seq("1"),
        "addressLine2" -> Seq("2"),
        "postcode" -> Seq("AA11 1AA"),
        "isResidential" -> Seq("true"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("02"),
        "startDate.year" -> Seq("1990"))) must be (Invalid(Seq(Path \ "tradingName"  -> Seq(ValidationError("error.required.tp.trading.name"))
      )))
    }

    "fail validation when trading name contains invalid characters" in {
      YourTradingPremises.formR.validate(Map("tradingName" -> Seq("{}{}}"),
        "addressLine1" -> Seq("1"),
        "addressLine2" -> Seq("2"),
        "postcode" -> Seq("AA11 1AA"),
        "isResidential" -> Seq("true"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("02"),
        "startDate.year" -> Seq("1990"))) must be (Invalid(Seq(Path \ "tradingName"  -> Seq(ValidationError("err.text.validation"))
      )))
    }

    "Correctly serialise from form data" in {


      implicitly[Rule[UrlFormEncoded, YourTradingPremises]].validate(data) must
        be(Valid(model.copy(tradingNameChangeDate = None)))
    }

    "Correctly write from model to form" in {

      implicitly[Write[YourTradingPremises, UrlFormEncoded]].writes(model.copy(tradingNameChangeDate = None)) must
        be(data)
    }

  }

  "The Json serializer" must {

    val json = Json.obj(
      "tradingName" -> "foo",
      "addressLine1" -> "1",
      "addressLine2" -> "2",
      "addressDateOfChange" -> new LocalDate(1997, 7, 1),
      "postcode" -> "AA11 1AA",
      "isResidential" -> true,
      "startDate" -> new LocalDate(1990, 2, 24),
      "tradingNameChangeDate" -> new LocalDate(2016,1,12)
    )

    val jsonModel = model.copy(
      tradingNameChangeDate = Some(DateOfChange(new LocalDate(2016, 1, 12))),
      tradingPremisesAddress = model.tradingPremisesAddress.copy(dateOfChange = Some(DateOfChange(new LocalDate(1997, 7, 1))))
    )

    "Correctly serialise from json" in {
      implicitly[Reads[YourTradingPremises]].reads(json) must
        be(JsSuccess(jsonModel))
    }

    "Correctly write form model to json" in {

      implicitly[Writes[YourTradingPremises]].writes(jsonModel) must
        be(json)
    }
  }
}
