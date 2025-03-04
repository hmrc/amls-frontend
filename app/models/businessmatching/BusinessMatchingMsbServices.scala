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

package models.businessmatching

import models.tradingpremises.TradingPremisesMsbService.{ChequeCashingNotScrapMetal => TPChequeCashingNotScrapMetal, ChequeCashingScrapMetal => TPChequeCashingScrapMetal, CurrencyExchange => TPCurrencyExchange, ForeignExchange => TPForeignExchange, TransmittingMoney => TPTransmittingMoney}
import models.{CheckYourAnswersField, Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem

case class BusinessMatchingMsbServices(msbServices: Set[BusinessMatchingMsbService])

sealed trait BusinessMatchingMsbService extends CheckYourAnswersField {

  import BusinessMatchingMsbService._

  val value: String

  def getMessage(implicit messages: Messages): String = {
    val message = "businessmatching.services.list.lbl."
    this match {
      case TransmittingMoney          => messages(s"${message}01")
      case CurrencyExchange           => messages(s"${message}02")
      case ChequeCashingNotScrapMetal => messages(s"${message}03")
      case ChequeCashingScrapMetal    => messages(s"${message}04")
      case ForeignExchange            => Messages(s"${message}05")
    }
  }

  def index: String = value.substring(1)
}

object BusinessMatchingMsbService extends Enumerable.Implicits {

  case object TransmittingMoney extends WithName("transmittingMoney") with BusinessMatchingMsbService {
    override val value: String = "01"
  }
  case object CurrencyExchange extends WithName("currencyExchange") with BusinessMatchingMsbService {
    override val value: String = "02"
  }
  case object ChequeCashingNotScrapMetal
      extends WithName("chequeCashingNotScrapMetal")
      with BusinessMatchingMsbService {
    override val value: String = "03"
  }
  case object ChequeCashingScrapMetal extends WithName("chequeCashingScrapMetal") with BusinessMatchingMsbService {
    override val value: String = "04"
  }
  case object ForeignExchange extends WithName("foreignExchange") with BusinessMatchingMsbService {
    override val value: String = "05"
  }

  implicit val jsonR: Reads[BusinessMatchingMsbService] = Reads {
    case JsString("01") => JsSuccess(TransmittingMoney)
    case JsString("02") => JsSuccess(CurrencyExchange)
    case JsString("03") => JsSuccess(ChequeCashingNotScrapMetal)
    case JsString("04") => JsSuccess(ChequeCashingScrapMetal)
    case JsString("05") => JsSuccess(ForeignExchange)
    case _              => JsError((JsPath \ "services") -> play.api.libs.json.JsonValidationError("error.invalid"))
  }

  implicit val jsonW: Writes[BusinessMatchingMsbService] = Writes[BusinessMatchingMsbService] {
    case TransmittingMoney          => JsString("01")
    case CurrencyExchange           => JsString("02")
    case ChequeCashingNotScrapMetal => JsString("03")
    case ChequeCashingScrapMetal    => JsString("04")
    case ForeignExchange            => JsString("05")
  }

  implicit val enumerable: Enumerable[BusinessMatchingMsbService] =
    Enumerable(BusinessMatchingMsbServices.all.map(v => v.toString -> v): _*)
}

object BusinessMatchingMsbServices {

  import BusinessMatchingMsbService._

  val all: Seq[BusinessMatchingMsbService] = Seq(
    TransmittingMoney,
    CurrencyExchange,
    ChequeCashingNotScrapMetal,
    ChequeCashingScrapMetal,
    ForeignExchange
  )

  def formValues(foreignExchangeEnabled: Boolean)(implicit messages: Messages): Seq[CheckboxItem] = {

    val filteredServices = if (foreignExchangeEnabled) all else all.filterNot(_ == ForeignExchange)

    filteredServices
      .map { service =>
        CheckboxItem(
          content = Text(messages(s"msb.services.list.lbl.${service.value}")),
          value = service.toString,
          id = Some(s"value_${service.index}"),
          name = Some(s"value[${service.index}]")
        )
      }
      .sortBy(_.value)
  }

  implicit val formats: OFormat[BusinessMatchingMsbServices] = Json.format[BusinessMatchingMsbServices]

  def getValue(ba: BusinessMatchingMsbService): String =
    ba match {
      case TransmittingMoney          => "01"
      case CurrencyExchange           => "02"
      case ChequeCashingNotScrapMetal => "03"
      case ChequeCashingScrapMetal    => "04"
      case ForeignExchange            => "05"
    }

  implicit def convertServices(
    msbServices: Set[models.tradingpremises.TradingPremisesMsbService]
  ): Set[models.businessmatching.BusinessMatchingMsbService] =
    msbServices map { s => convertSingleService(s) }

  implicit def convertSingleService(
    msbService: models.tradingpremises.TradingPremisesMsbService
  ): models.businessmatching.BusinessMatchingMsbService =
    msbService match {
      case TPTransmittingMoney          => TransmittingMoney
      case TPCurrencyExchange           => CurrencyExchange
      case TPChequeCashingNotScrapMetal => ChequeCashingNotScrapMetal
      case TPChequeCashingScrapMetal    => ChequeCashingScrapMetal
      case TPForeignExchange            => ForeignExchange
    }
}
