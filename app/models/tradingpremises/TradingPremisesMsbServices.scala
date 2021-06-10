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

package models.tradingpremises

import cats.data.Validated.{Invalid, Valid}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{Rule, ValidationError, _}
import models.DateOfChange
import models.businessmatching.{ChequeCashingNotScrapMetal => BMChequeCashingNotScrapMetal, ChequeCashingScrapMetal => BMChequeCashingScrapMetal, CurrencyExchange => BMCurrencyExchange, ForeignExchange => BMForeignExchange, TransmittingMoney => BMTransmittingMoney}
import play.api.i18n.Messages
import play.api.libs.json.{Reads, Writes, _}
import utils.TraversableValidators

sealed trait TradingPremisesMsbService{
  val message = "msb.services.list.lbl."
  def getMessage(implicit messages: Messages): String =
    this match {
      case TransmittingMoney => messages(s"${message}01")
      case CurrencyExchange => messages(s"${message}02")
      case ChequeCashingNotScrapMetal => messages(s"${message}03")
      case ChequeCashingScrapMetal => messages(s"${message}04")
      case ForeignExchange => messages(s"${message}05")
    }
}

case object TransmittingMoney extends TradingPremisesMsbService
case object CurrencyExchange extends TradingPremisesMsbService
case object ChequeCashingNotScrapMetal extends TradingPremisesMsbService
case object ChequeCashingScrapMetal extends TradingPremisesMsbService
case object ForeignExchange extends TradingPremisesMsbService

case class TradingPremisesMsbServices(services : Set[TradingPremisesMsbService])

object TradingPremisesMsbService {

  implicit val serviceR = Rule[String, TradingPremisesMsbService] {
    case "01" => Valid(TransmittingMoney)
    case "02" => Valid(CurrencyExchange)
    case "03" => Valid(ChequeCashingNotScrapMetal)
    case "04" => Valid(ChequeCashingScrapMetal)
    case "05" => Valid(ForeignExchange)
    case _ => Invalid(Seq(Path -> Seq(ValidationError("error.invalid"))))
  }

  implicit val serviceW = Write[TradingPremisesMsbService, String] {
    case TransmittingMoney => "01"
    case CurrencyExchange => "02"
    case ChequeCashingNotScrapMetal => "03"
    case ChequeCashingScrapMetal => "04"
    case ForeignExchange => "05"
  }

  implicit val jsonR: Rule[JsValue, TradingPremisesMsbService] = {
    import jto.validation.playjson.Rules._
    stringR andThen serviceR
  }

  implicit val jsonW: Write[TradingPremisesMsbService, JsValue] = {
    import jto.validation.playjson.Writes._
    serviceW andThen string
  }

  def applyWithoutDateOfChange(services: Set[TradingPremisesMsbService]) = TradingPremisesMsbServices(services)

  def unapplyWithoutDateOfChange(s: TradingPremisesMsbServices) = Some(s.services)

}

sealed trait MsbServices0 {

  private implicit def rule[A]
  (implicit
   p: Path => RuleLike[A, Set[TradingPremisesMsbService]]
  ): Rule[A, TradingPremisesMsbServices] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits.RichRule

      val required =
        TraversableValidators.minLengthR[Set[TradingPremisesMsbService]](1) withMessage "error.required.tp.services"

      (__ \ "msbServices").read(required) map TradingPremisesMsbService.applyWithoutDateOfChange
    }

  private implicit def write[A]
  (implicit
   p: Path => WriteLike[Set[TradingPremisesMsbService], A]
  ): Write[TradingPremisesMsbServices, A] =
    To[A] { __ =>

      import play.api.libs.functional.syntax.unlift

      (__ \ "msbServices").write[Set[TradingPremisesMsbService]] contramap unlift(TradingPremisesMsbService.unapplyWithoutDateOfChange)
    }

  val formR: Rule[UrlFormEncoded, TradingPremisesMsbServices] = {
    import jto.validation.forms.Rules._
    implicitly[Rule[UrlFormEncoded, TradingPremisesMsbServices]]
  }

  val formW: Write[TradingPremisesMsbServices, UrlFormEncoded] = {
    import jto.validation.forms.Writes._
    import utils.MappingUtils.writeM
    implicitly[Write[TradingPremisesMsbServices, UrlFormEncoded]]
  }
}

object TradingPremisesMsbServices {

  private object Cache extends MsbServices0

  def addDateOfChange(doc: Option[DateOfChange], obj: JsObject) =
    doc.fold(obj) { dateOfChange => obj + ("dateOfChange" -> DateOfChange.writes.writes(dateOfChange))}

  implicit val jsonWrites = new Writes[TradingPremisesMsbServices] {
    def writes(s: TradingPremisesMsbServices): JsValue = {
      val values = s.services map { x => JsString(TradingPremisesMsbService.serviceW.writes(x)) }

      Json.obj(
        "msbServices" -> values
      )
    }
  }

  implicit val msbServiceReader: Reads[Set[TradingPremisesMsbService]] = {
    __.read[JsArray].map(a => a.value.map(TradingPremisesMsbService.jsonR.validate(_).toOption.get).toSet)
  }

  implicit val jReads: Reads[TradingPremisesMsbServices] = {
    (__ \ "msbServices").read[Set[TradingPremisesMsbService]].map(TradingPremisesMsbServices.apply _)
  }

  implicit val formR: Rule[UrlFormEncoded, TradingPremisesMsbServices] = Cache.formR
  implicit val formW: Write[TradingPremisesMsbServices, UrlFormEncoded] = Cache.formW

  implicit def convertServices(msbService: Set[models.businessmatching.BusinessMatchingMsbService]): Set[TradingPremisesMsbService] =
    msbService map {s => convertSingleService(s)}

  implicit def convertSingleService(msbService: models.businessmatching.BusinessMatchingMsbService): models.tradingpremises.TradingPremisesMsbService = {
    msbService match {
      case BMTransmittingMoney => TransmittingMoney
      case BMCurrencyExchange => CurrencyExchange
      case BMChequeCashingNotScrapMetal => ChequeCashingNotScrapMetal
      case BMChequeCashingScrapMetal => ChequeCashingScrapMetal
      case BMForeignExchange => ForeignExchange
    }
  }
}

