/*
 * Copyright 2024 HM Revenue & Customs
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

import models.businessmatching.BusinessMatchingMsbService.{ChequeCashingNotScrapMetal => BMChequeCashingNotScrapMetal, ChequeCashingScrapMetal => BMChequeCashingScrapMetal, CurrencyExchange => BMCurrencyExchange, ForeignExchange => BMForeignExchange, TransmittingMoney => BMTransmittingMoney}
import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.Aliases.CheckboxItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

sealed trait TradingPremisesMsbService {

  val value: String

  val message                                         = "msb.services.list.lbl."
  def getMessage(implicit messages: Messages): String = {

    import TradingPremisesMsbService._

    this match {
      case TransmittingMoney          => messages(s"${message}01")
      case CurrencyExchange           => messages(s"${message}02")
      case ChequeCashingNotScrapMetal => messages(s"${message}03")
      case ChequeCashingScrapMetal    => messages(s"${message}04")
      case ForeignExchange            => messages(s"${message}05")
    }
  }

  def index = value.substring(1)
}

case class TradingPremisesMsbServices(services: Set[TradingPremisesMsbService])

object TradingPremisesMsbService extends Enumerable.Implicits {

  case object TransmittingMoney extends WithName("transmittingMoney") with TradingPremisesMsbService {
    override val value: String = "01"
  }

  case object CurrencyExchange extends WithName("currencyExchange") with TradingPremisesMsbService {
    override val value: String = "02"
  }

  case object ChequeCashingNotScrapMetal extends WithName("chequeCashingNotScrapMetal") with TradingPremisesMsbService {
    override val value: String = "03"
  }

  case object ChequeCashingScrapMetal extends WithName("chequeCashingScrapMetal") with TradingPremisesMsbService {
    override val value: String = "04"
  }

  case object ForeignExchange extends WithName("foreignExchange") with TradingPremisesMsbService {
    override val value: String = "05"
  }

  def formValues(
    filterValues: Option[Seq[TradingPremisesMsbService]]
  )(implicit messages: Messages): Seq[CheckboxItem] = {

    val filteredValues = filterValues.fold(all)(all diff _)

    filteredValues
      .map { msbService =>
        CheckboxItem(
          content = Text(messages(s"msb.services.list.lbl.${msbService.value}")),
          value = msbService.toString,
          id = Some(s"value_${msbService.index}"),
          name = Some(s"value[${msbService.index}]")
        )
      }
      .sortBy(_.value)

  }

  implicit val reads: Reads[TradingPremisesMsbService] = Reads {
    case JsString("01") => JsSuccess(TransmittingMoney)
    case JsString("02") => JsSuccess(CurrencyExchange)
    case JsString("03") => JsSuccess(ChequeCashingNotScrapMetal)
    case JsString("04") => JsSuccess(ChequeCashingScrapMetal)
    case JsString("05") => JsSuccess(ForeignExchange)
    case _              => JsError(play.api.libs.json.JsonValidationError("error.invalid"))
  }

  implicit val writes: Writes[TradingPremisesMsbService] = Writes[TradingPremisesMsbService] {
    case TransmittingMoney          => JsString(TransmittingMoney.value)
    case CurrencyExchange           => JsString(CurrencyExchange.value)
    case ChequeCashingNotScrapMetal => JsString(ChequeCashingNotScrapMetal.value)
    case ChequeCashingScrapMetal    => JsString(ChequeCashingScrapMetal.value)
    case ForeignExchange            => JsString(ForeignExchange.value)
  }

  val all: Seq[TradingPremisesMsbService] = Seq(
    TransmittingMoney,
    CurrencyExchange,
    ChequeCashingNotScrapMetal,
    ChequeCashingScrapMetal,
    ForeignExchange
  )

  implicit val enumerable: Enumerable[TradingPremisesMsbService] = Enumerable(all.map(v => v.toString -> v): _*)
}

object TradingPremisesMsbServices {

  // TODO - come back to this
  implicit val jsonWrites: Writes[TradingPremisesMsbServices] = new Writes[TradingPremisesMsbServices] {
    def writes(s: TradingPremisesMsbServices): JsValue = {
      val values = s.services map Json.toJson[TradingPremisesMsbService]

      Json.obj(
        "msbServices" -> values
      )
    }
  }

  implicit val jReads: Reads[TradingPremisesMsbServices] =
    (__ \ "msbServices").read[Set[TradingPremisesMsbService]].map(TradingPremisesMsbServices.apply)

  implicit def convertServices(
    msbService: Set[models.businessmatching.BusinessMatchingMsbService]
  ): Set[TradingPremisesMsbService] =
    msbService map { s => convertSingleService(s) }

  implicit def convertSingleService(
    msbService: models.businessmatching.BusinessMatchingMsbService
  ): models.tradingpremises.TradingPremisesMsbService = {

    import TradingPremisesMsbService._

    msbService match {
      case BMTransmittingMoney          => TransmittingMoney
      case BMCurrencyExchange           => CurrencyExchange
      case BMChequeCashingNotScrapMetal => ChequeCashingNotScrapMetal
      case BMChequeCashingScrapMetal    => ChequeCashingScrapMetal
      case BMForeignExchange            => ForeignExchange
    }
  }
}
