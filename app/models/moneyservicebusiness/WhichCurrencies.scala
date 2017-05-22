/*
 * Copyright 2017 HM Revenue & Customs
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

import cats.data.Validated.{Invalid, Valid}
import config.ApplicationConfig
import jto.validation.GenericRules._
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import models.FormTypes._
import models._
import play.api.libs.json._
import models.renewal.{WhichCurrencies => RWhichCurrencies}
import utils.MappingUtils.Implicits._
import utils.{GenericValidators, TraversableValidators}

case class WhichCurrencies(currencies: Seq[String],
                           usesForeignCurrencies: Option[Boolean],
                           bankMoneySource: Option[BankMoneySource],
                           wholesalerMoneySource: Option[WholesalerMoneySource],
                           customerMoneySource: Option[Boolean])


object WhichCurrencies {
  def convert(wc: WhichCurrencies): RWhichCurrencies = {
    RWhichCurrencies(wc.currencies,  wc.usesForeignCurrencies, wc.bankMoneySource, wc.wholesalerMoneySource, wc.customerMoneySource)
  }

  type MoneySourceValidation = (Option[BankMoneySource], Option[WholesalerMoneySource], Option[Boolean])
  type WhichCurrenciesValidation = (Option[Boolean], Option[BankMoneySource], Option[WholesalerMoneySource], Option[Boolean])

  val emptyToNone: String => Option[String] = { x =>
    x.trim() match {
      case "" => None
      case s => Some(s)
    }
  }

  private def nameType(fieldName: String) = {
    notEmptyStrip andThen
      minLength(1).withMessage(s"error.invalid.msb.wc.$fieldName") andThen
      maxLength(140).withMessage("error.invalid.maxlength.140") andThen
      basicPunctuationPattern()
  }

  private val currencyListType = TraversableValidators.seqToOptionSeq(emptyToNone) andThen
    TraversableValidators.flattenR[String] andThen
    TraversableValidators.minLengthR[Seq[String]](1) andThen
    GenericRules.traversableR(GenericValidators.inList(currencies))

  private val validateMoneySources: ValidationRule[MoneySourceValidation] = Rule[MoneySourceValidation, MoneySourceValidation] {
      case x@(Some(_), _, _) => Valid(x)
      case x@(_, Some(_), _) => Valid(x)
      case x@(_, _, Some(true)) => Valid(x)
      case _ => Invalid(Seq((Path \ "WhoWillSupply") -> Seq(ValidationError("error.invalid.msb.wc.moneySources"))))
    }


  private val validateWhichCurrencies: ValidationRule[WhichCurrenciesValidation] = Rule[WhichCurrenciesValidation, WhichCurrenciesValidation] {
    case x@(Some(true), Some(b), _, _) => Valid(x)
    case x@(Some(true), _, Some(c), _) => Valid(x)
    case x@(Some(true), _, _, Some(d)) => Valid(x)
    case x@(Some(false), _, _, _) => Valid((Some(false), None, None, None))
    case _ => Invalid(Seq((Path \ "WhoWillSupply") -> Seq(ValidationError("error.invalid.msb.wc.moneySources"))))
  }

  implicit def formR: Rule[UrlFormEncoded, WhichCurrencies] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    val currencies = (__ \ "currencies").read(currencyListType).withMessage("error.invalid.msb.wc.currencies")

    val usesForeignCurrencies = ApplicationConfig.release7 match {
      case true =>
        (__ \ "usesForeignCurrencies").read[String] withMessage "error.required.msb.wc.foreignCurrencies" map {
          case "Yes" => Option(true)
          case _ => Option(false)
        }
      case _ => Rule[UrlFormEncoded, Option[Boolean]](_ => Valid(None))
    }

    val bankMoneySource: Rule[UrlFormEncoded, Option[BankMoneySource]] =
      (__ \ "bankMoneySource").read[Option[String]] flatMap {
        case Some("Yes") => (__ \ "bankNames")
          .read(nameType("bankNames"))
          .map(names => Some(BankMoneySource(names)))
        case _ => Rule[UrlFormEncoded, Option[BankMoneySource]](_ => Valid(None))
      }

    val wholesalerMoneySource: Rule[UrlFormEncoded, Option[WholesalerMoneySource]] =
      (__ \ "wholesalerMoneySource").read[Option[String]] flatMap {
        case Some("Yes") => (__ \ "wholesalerNames")
          .read(nameType("wholesalerNames"))
          .map(names => Some(WholesalerMoneySource(names)))
        case _ => Rule[UrlFormEncoded, Option[WholesalerMoneySource]](_ => Valid(None))
      }

    val customerMoneySource = (__ \ "customerMoneySource").read[Option[String]] map {
      case Some("Yes") => Some(true)
      case _ => None
    }

    ApplicationConfig.release7 match {
      case true => (currencies ~ ((usesForeignCurrencies ~ bankMoneySource ~ wholesalerMoneySource ~ customerMoneySource).tupled andThen validateWhichCurrencies)).apply {
        (a, b) => WhichCurrencies(a.toSeq, b._1, b._2, b._3, b._4)
      }

      case _ => (currencies ~ ((bankMoneySource ~ wholesalerMoneySource ~ customerMoneySource).tupled andThen validateMoneySources))
        .apply { (a: Traversable[String], b: MoneySourceValidation) =>
          (a, b) match {
            case (c, (bms, wms, cms)) => WhichCurrencies(c.toSeq, None, bms, wms, cms)
          }
        }
    }

  }

  implicit val formW: Write[WhichCurrencies, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._

    val bToS: (Boolean) => Option[String] = {
      case true => Some("Yes")
      case _ => Some("No")
    }

    val defaultFlagValue: (WhichCurrencies) => Option[String] = {
      case x if ApplicationConfig.release7 && (x.customerMoneySource.contains(true) || x.bankMoneySource.isDefined || x.wholesalerMoneySource.isDefined) =>
        Some("Yes")
      case _ if ApplicationConfig.release7 =>
        Some("No")
      case _ => None
    }

    (
      (__ \ "currencies").write[Seq[String]] ~
        (__ \ "bankMoneySource").write[Option[String]] ~
        (__ \ "bankNames").write[Option[String]] ~
        (__ \ "wholesalerMoneySource").write[Option[String]] ~
        (__ \ "wholesalerNames").write[Option[String]] ~
        (__ \ "customerMoneySource").write[Option[String]] ~
        (__ \ "usesForeignCurrencies").write[Option[String]]
      ).apply(wc => (wc.currencies,
      wc.bankMoneySource.map(_ => "Yes"),
      wc.bankMoneySource.map(bms => bms.bankNames),
      wc.wholesalerMoneySource.map(_ => "Yes"),
      wc.wholesalerMoneySource.map(bms => bms.wholesalerNames),
      wc.customerMoneySource.map(_ => "Yes"),
      wc.usesForeignCurrencies.fold[Option[String]](defaultFlagValue(wc))(bToS)
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

      val flag = (ApplicationConfig.release7, usesForeignCurrencies) match {
        case (true, None) => Some(bms.isDefined || wms.isDefined || cms.contains(true))
        case (_, x) => x
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
