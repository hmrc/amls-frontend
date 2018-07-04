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

package models.businessmatching

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, ValidationError, _}
import models.tradingpremises.{ChequeCashingNotScrapMetal => TPChequeCashingNotScrapMetal, ChequeCashingScrapMetal => TPChequeCashingScrapMetal, CurrencyExchange => TPCurrencyExchange, ForeignExchange => TPForeignExchange, TransmittingMoney => TPTransmittingMoney}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.i18n.{Lang, Messages}
import play.api.libs.json.{Reads, Writes, _}
import utils.TraversableValidators._


case class BusinessMatchingMsbServices(msbServices : Set[BusinessMatchingMsbService])

sealed trait BusinessMatchingMsbService {

  def getMessage(implicit lang: Lang): String = {
    val message = "businessmatching.services.list.lbl."
    this match {
      case TransmittingMoney => Messages(s"${message}01")
      case CurrencyExchange => Messages(s"${message}02")
      case ChequeCashingNotScrapMetal => Messages(s"${message}03")
      case ChequeCashingScrapMetal => Messages(s"${message}04")
      case ForeignExchange => Messages(s"${message}05")
    }
  }
}

case object TransmittingMoney extends BusinessMatchingMsbService
case object CurrencyExchange extends BusinessMatchingMsbService
case object ChequeCashingNotScrapMetal extends BusinessMatchingMsbService
case object ChequeCashingScrapMetal extends BusinessMatchingMsbService
case object ForeignExchange extends BusinessMatchingMsbService

object BusinessMatchingMsbService {

  implicit val serviceR = Rule[String, BusinessMatchingMsbService] {
    case "01" => Valid(TransmittingMoney)
    case "02" => Valid(CurrencyExchange)
    case "03" => Valid(ChequeCashingNotScrapMetal)
    case "04" => Valid(ChequeCashingScrapMetal)
    case "05" => Valid(ForeignExchange)
    case _ => Invalid(Seq(Path -> Seq(ValidationError("error.invalid"))))
  }

  implicit val serviceW = Write[BusinessMatchingMsbService, String] {
    case TransmittingMoney => "01"
    case CurrencyExchange => "02"
    case ChequeCashingNotScrapMetal => "03"
    case ChequeCashingScrapMetal => "04"
    case ForeignExchange => "05"
  }

  implicit val jsonR:Reads[BusinessMatchingMsbService] =  Reads {
    case JsString("01") => JsSuccess(TransmittingMoney)
    case JsString("02") => JsSuccess(CurrencyExchange)
    case JsString("03") => JsSuccess(ChequeCashingNotScrapMetal)
    case JsString("04") => JsSuccess(ChequeCashingScrapMetal)
    case JsString("05") => JsSuccess(ForeignExchange)
    case _ => JsError((JsPath \ "services") -> play.api.data.validation.ValidationError("error.invalid"))
  }

  implicit val jsonW = Writes[BusinessMatchingMsbService] {
    case TransmittingMoney => JsString("01")
    case CurrencyExchange => JsString("02")
    case ChequeCashingNotScrapMetal => JsString("03")
    case ChequeCashingScrapMetal => JsString("04")
    case ForeignExchange => JsString("05")
  }
}

object BusinessMatchingMsbServices {

  val all: Set[BusinessMatchingMsbService] = Set(
    TransmittingMoney,
    CurrencyExchange,
    ChequeCashingNotScrapMetal,
    ChequeCashingScrapMetal,
    ForeignExchange
  )

  import utils.MappingUtils.Implicits._

  implicit def formReads
  (implicit
   p: Path => RuleLike[UrlFormEncoded, Set[BusinessMatchingMsbService]]
  ): Rule[UrlFormEncoded, BusinessMatchingMsbServices] =
    From[UrlFormEncoded] { __ =>
      (__ \ "msbServices").read(minLengthR[Set[BusinessMatchingMsbService]](1).withMessage("error.required.msb.services")).flatMap(BusinessMatchingMsbServices.apply)
    }

  implicit def formWrites
  (implicit
   w: Write[BusinessMatchingMsbService, String]
  ) = Write[BusinessMatchingMsbServices, UrlFormEncoded] { data =>
    Map("msbServices[]" -> data.msbServices.toSeq.map(w.writes))
  }

  implicit val formats = Json.format[BusinessMatchingMsbServices]

  def getValue(ba:BusinessMatchingMsbService): String =
    ba match {
      case TransmittingMoney => "01"
      case CurrencyExchange => "02"
      case ChequeCashingNotScrapMetal => "03"
      case ChequeCashingScrapMetal => "04"
      case ForeignExchange => "05"
    }

  implicit def convertServices(msbServices: Set[models.tradingpremises.TradingPremisesMsbService]): Set[models.businessmatching.BusinessMatchingMsbService] =
    msbServices map {s => convertSingleService(s)}


  implicit def convertSingleService(msbService: models.tradingpremises.TradingPremisesMsbService) : models.businessmatching.BusinessMatchingMsbService = {
    msbService match {
      case TPTransmittingMoney => TransmittingMoney
      case TPCurrencyExchange => CurrencyExchange
      case TPChequeCashingNotScrapMetal => ChequeCashingNotScrapMetal
      case TPChequeCashingScrapMetal => ChequeCashingScrapMetal
      case TPForeignExchange => ForeignExchange
    }
  }
}

