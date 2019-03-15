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

import jto.validation.GenericRules._
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import models.FormTypes._
import models.currencies
import models.moneyservicebusiness.WhichCurrencies.emptyToNone
import models.renewal.{WhichCurrencies => RWhichCurrencies}
import play.api.libs.json._
import utils.{GenericValidators, TraversableValidators}
import utils.MappingUtils.Implicits._

case class WhichCurrencies(currencies: Seq[String],
                           usesForeignCurrencies: Option[Boolean] = None,
                           bankMoneySource: Option[BankMoneySource] = None,
                           wholesalerMoneySource: Option[WholesalerMoneySource] = None,
                           customerMoneySource: Option[Boolean] = None)


object WhichCurrencies {
  def convert(wc: WhichCurrencies): RWhichCurrencies = {
    RWhichCurrencies(wc.currencies,  wc.usesForeignCurrencies, wc.bankMoneySource, wc.wholesalerMoneySource, wc.customerMoneySource)
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


  implicit def formR: Rule[UrlFormEncoded, WhichCurrencies] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    ((__ \ "currencies").read(currencyListType).withMessage("error.invalid.msb.wc.currencies") ~
    (__ \ "usesForeignCurrencies").read[Option[Boolean]].withMessage("error.required.msb.wc.foreignCurrencies") ~
    (__ \ "bankMoneySource").read[Option[BankMoneySource]] ~
    (__ \ "wholesalerMoneySource").read[Option[WholesalerMoneySource]] ~
    (__ \ "customerMoneySource").read[Option[Boolean]]).tupled map {
      r => WhichCurrencies(r._1.toSeq, r._2,r._3,r._4,r._5)
    }
  }

  implicit val formW: Write[WhichCurrencies, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._

    val bToS: (Boolean) => Option[String] = {
      case true => Some("Yes")
      case _ => Some("No")
    }

    val defaultFlagValue: (WhichCurrencies) => Option[String] = {
      case x if (x.customerMoneySource.contains(true) || x.bankMoneySource.isDefined || x.wholesalerMoneySource.isDefined) => Some("Yes")
      case _ => Some("No")
    }

    (
      (__ \ "currencies").write[Seq[String]] ~
        (__ \ "bankMoneySource").write[Option[String]] ~
        (__ \ "bankNames").write[Option[String]] ~
        (__ \ "wholesalerMoneySource").write[Option[String]] ~
        (__ \ "wholesalerNames").write[Option[String]] ~
        (__ \ "customerMoneySource").write[Option[String]]
      ).apply(wc => (wc.currencies,
      wc.bankMoneySource.map(_ => "Yes"),
      wc.bankMoneySource.map(bms => bms.bankNames),
      wc.wholesalerMoneySource.map(_ => "Yes"),
      wc.wholesalerMoneySource.map(bms => bms.wholesalerNames),
      wc.customerMoneySource.map(_ => "Yes")
      ))
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
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "currencies").read[Seq[String]] and
        (__ \ "usesForeignCurrencies").readNullable[Boolean] and
        __.read[Option[BankMoneySource]] and
        __.read[Option[WholesalerMoneySource]] and
        (__ \ "customerMoneySource").readNullable(cmsReader)
      )((currencies, usesForeignCurrencies, bms, wms, cms) => {

      val flag = usesForeignCurrencies match {
        case None => None//Some(bms.isDefined || wms.isDefined || cms.contains(true))
        case x => x
      }

      WhichCurrencies(currencies, flag, bms, wms, cms)
    })
  }

  implicit val jsonW: Writes[WhichCurrencies] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (
      (__ \ "currencies").write[Seq[String]] and
        (__ \ "usesForeignCurrencies").writeNullable[Boolean] and
        __.write[Option[BankMoneySource]] and
        __.write[Option[WholesalerMoneySource]] and
        (__ \ "customerMoneySource").writeNullable(cmsWriter)

      )(x => (x.currencies, x.usesForeignCurrencies, x.bankMoneySource, x.wholesalerMoneySource, x.customerMoneySource))

  }
}
