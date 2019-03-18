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

import cats.data.Validated.{Invalid, Valid}
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import models.FormTypes._
import models.ValidationRule
import play.api.libs.json._

case class BankMoneySource(bankNames : String)

case class WholesalerMoneySource(wholesalerNames : String)

case object CustomerMoneySource

case class MoneySources(bankMoneySource: Option[BankMoneySource],
                         wholesalerMoneySource: Option[WholesalerMoneySource],
                         customerMoneySource: Option[Boolean])

object MoneySources {
  import jto.validation.forms.Rules._
  import utils.MappingUtils.Implicits._

  private def nameType(fieldName: String) = {
    notEmptyStrip andThen
      minLength(1).withMessage(s"error.invalid.msb.wc.$fieldName") andThen
      maxLength(140).withMessage("error.invalid.maxlength.140") andThen
      basicPunctuationPattern()
  }

  type MoneySourceValidation = (Option[BankMoneySource], Option[WholesalerMoneySource], Option[Boolean])

  private val validateMoneySources: ValidationRule[MoneySourceValidation] = Rule[MoneySourceValidation, MoneySourceValidation] {
    case x@(Some(_), _, _) => Valid(x)
    case x@(_, Some(_), _) => Valid(x)
    case x@(_, _, Some(true)) => Valid(x)
    case _ => Invalid(Seq((Path \ "WhoWillSupply") -> Seq(ValidationError("error.invalid.msb.wc.moneySources"))))
  }

  implicit def formRule: Rule[UrlFormEncoded, MoneySources] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms._

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

    val customerMoneySource: Rule[UrlFormEncoded, Option[Boolean]] =
      (__ \ "customerMoneySource").read[Option[String]] map {
      case Some("Yes") => Some(true)
      case _ => None
    }

    val validatedMs = (bankMoneySource ~ wholesalerMoneySource ~ customerMoneySource).tupled andThen validateMoneySources

    validatedMs map { msv: MoneySourceValidation =>
      (msv._1, msv._2, msv._3) match{
        case (b, w, c) => MoneySources(b,w,c)
      }
    }
  }

  implicit val formWrite: Write[MoneySources, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._

    ((__ \ "bankMoneySource").write[Option[String]] ~
      (__ \ "bankNames").write[Option[String]] ~
      (__ \ "wholesalerMoneySource").write[Option[String]] ~
      (__ \ "wholesalerNames").write[Option[String]] ~
      (__ \ "customerMoneySource").write[Option[String]]).apply(ms =>
      (ms.bankMoneySource.map(_ => "Yes"),
        ms.bankMoneySource.map(bms => bms.bankNames),
        ms.wholesalerMoneySource.map(_ => "Yes"),
        ms.wholesalerMoneySource.map(bms => bms.wholesalerNames),
        ms.customerMoneySource.map(_ => "Yes")))
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

  implicit val jsonReads: Reads[MoneySources] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      __.read[Option[BankMoneySource]] and
        __.read[Option[WholesalerMoneySource]] and
        (__ \ "customerMoneySource").readNullable(cmsReader))((bms, wms, cms) => MoneySources(bms, wms, cms))
  }

  implicit val jsonWrites: Writes[MoneySources] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (
      __.write[Option[BankMoneySource]] and
        __.write[Option[WholesalerMoneySource]] and
        (__ \ "customerMoneySource").writeNullable(cmsWriter))(x => (x.bankMoneySource, x.wholesalerMoneySource, x.customerMoneySource))
  }
}







