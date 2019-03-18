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

package models.moneyservicebusiness

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import models.currencies
import play.api.libs.json._
import utils.MappingUtils.Implicits._
import utils.{GenericValidators, TraversableValidators}
import models.renewal.{WhichCurrencies => RWhichCurrencies}

case class WhichCurrencies(currencies: Seq[String])


object WhichCurrencies {
  def convert(wc: WhichCurrencies, uf: UsesForeignCurrencies, ms: MoneySources): RWhichCurrencies = {
    RWhichCurrencies(wc.currencies,  uf match {
      case UsesForeignCurrenciesYes => Some(true)
      case UsesForeignCurrenciesNo => Some(false)
    }, ms.bankMoneySource, ms.wholesalerMoneySource, ms.customerMoneySource)
  }

  def convert(wc: WhichCurrencies): RWhichCurrencies = {
    RWhichCurrencies(wc.currencies, None, None, None, None)
  }

  val emptyToNone: String => Option[String] = { x =>
    x.trim() match {
      case "" => None
      case s => Some(s)
    }
  }

  private val currencyListType = TraversableValidators.seqToOptionSeq(emptyToNone) andThen
    TraversableValidators.flattenR[String] andThen
    TraversableValidators.minLengthR[Seq[String]](1) andThen
    GenericRules.traversableR(GenericValidators.inList(currencies))


  implicit def formRule: Rule[UrlFormEncoded, WhichCurrencies] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    (__ \ "currencies").read(currencyListType).withMessage("error.invalid.msb.wc.currencies").flatMap(r => WhichCurrencies(r.toSeq))
  }

  implicit val formWrite: Write[WhichCurrencies, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    (__ \ "currencies").write[Seq[String]] contramap {x =>x.currencies}
  }

  implicit val bmsReader: Reads[Option[BankMoneySource]] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    ((__ \ "bankMoneySource").readNullable[String] and
      (__ \ "bankNames").readNullable[String])((a, b) => (a, b) match {
      case (Some("Yes"), Some(names)) => Some(BankMoneySource(names))
      case _ => None
    })

  }

  implicit val bmsWriter = new Writes[Option[BankMoneySource]] {
    override def writes(o: Option[BankMoneySource]): JsValue = o match {
      case Some(x) => Json.obj("bankMoneySource" -> "Yes",
        "bankNames" -> x.bankNames)
      case _ => Json.obj()
    }
  }

  implicit val wsReader: Reads[Option[WholesalerMoneySource]] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    ((__ \ "wholesalerMoneySource").readNullable[String] and
      (__ \ "wholesalerNames").readNullable[String])((a, b) => (a, b) match {
      case (Some("Yes"), Some(names)) => Some(WholesalerMoneySource(names))
      case _ => None
    })
  }

  implicit val wsWriter = new Writes[Option[WholesalerMoneySource]] {
    override def writes(o: Option[WholesalerMoneySource]): JsValue = o match {
      case Some(x) => Json.obj("wholesalerMoneySource" -> "Yes",
        "wholesalerNames" -> x.wholesalerNames)
      case _ => Json.obj()
    }
  }

  val cmsReader: Reads[Boolean] = {
    __.read[String] map {
      case "Yes" => true
      case _ => false
    }
  }

  val cmsWriter = new Writes[Boolean] {
    override def writes(o: Boolean): JsValue = o match {
      case true => JsString("Yes")
      case _ => JsNull
    }
  }

  implicit val jsonR: Reads[WhichCurrencies] = {
    import play.api.libs.json._
    (__ \ "currencies").read[Seq[String]].map(WhichCurrencies.apply)
  }

  implicit val jsonWrites: Writes[WhichCurrencies] = Json.writes[WhichCurrencies]
}
