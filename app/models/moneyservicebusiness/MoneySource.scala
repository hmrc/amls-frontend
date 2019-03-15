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

import cats.data.Validated.Valid
import jto.validation.GenericRules.{maxLength, minLength}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import models.FormTypes.{basicPunctuationPattern, notEmptyStrip}
import play.api.libs.json._
import utils.MappingUtils.Implicits._

trait MoneySource {

  def nameType(fieldName: String) = {
    notEmptyStrip andThen
      minLength(1).withMessage(s"error.invalid.msb.wc.$fieldName") andThen
      maxLength(140).withMessage("error.invalid.maxlength.140") andThen
      basicPunctuationPattern()
  }
}

case class BankMoneySource(bankNames : String)

object BankMoneySource extends MoneySource {

  implicit def formRule: Rule[UrlFormEncoded, Option[BankMoneySource]] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    (__ \ "bankMoneySource").read[Option[String]] flatMap {
      case Some("Yes") => (__ \ "bankNames")
        .read(nameType("bankNames"))
        .map(names => Some(BankMoneySource(names)))
      case _ => Rule[UrlFormEncoded, Option[BankMoneySource]](_ => Valid(None))
    }
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
}

case class WholesalerMoneySource(wholesalerNames : String)

object WholesalerMoneySource extends MoneySource {

  implicit def formRule: Rule[UrlFormEncoded, Option[WholesalerMoneySource]] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    (__ \ "wholesalerMoneySource").read[Option[String]] flatMap {
      case Some("Yes") => (__ \ "wholesalerNames")
        .read(nameType("wholesalerNames"))
        .map(names => Some(WholesalerMoneySource(names)))
      case _ => Rule[UrlFormEncoded, Option[WholesalerMoneySource]](_ => Valid(None))
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
}

sealed trait CustomerMoneySource

object CustomerMoneySource {

  import utils.MappingUtils.Implicits._
  import play.api.libs.json._

  implicit val formRule: Rule[UrlFormEncoded, CustomerMoneySource] =
    From[UrlFormEncoded] { __ =>
      (__ \ "customerMoneySource").readNullable[Boolean].withMessage("error.required.msb.wc.foreignCurrencies")
    }

//  implicit def formWrites = Write[CustomerMoneySource, UrlFormEncoded] {
//    case UsesForeignCurrenciesYes => Map("customerMoneySource" -> "true")
//    case CustomerMoneySourceNo => Map("customerMoneySource" -> "false")
//  }

//  implicit val jsonReads: Reads[CustomerMoneySource] = {
//
//    (__ \ "customerMoneySource").read[Boolean] flatMap {
//      case true => Reads(_ => JsSuccess(CustomerMoneySourceYes))
//      case false => Reads(_ => JsSuccess(CustomerMoneySourceNo))
//    }
//  }
//
//
//
//  implicit val jsonWrites = Writes[CustomerMoneySource] {
//    case CustomerMoneySourceYes => Json.obj("customerMoneySource" -> true)
//    case CustomerMoneySourceNo => Json.obj("customerMoneySource" -> false)
//
//  }

  implicit val cmsReader: Reads[Boolean] = {
    __.read[String] map {
      case "Yes" => true
      case _ => false
    }
  }

  implicit val cmsWriter = new Writes[Boolean] {
    override def writes(o: Boolean): JsValue = o match {
      case true => JsString("Yes")
      case _ => JsNull
    }
  }
}


