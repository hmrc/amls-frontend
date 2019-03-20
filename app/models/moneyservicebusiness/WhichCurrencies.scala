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
import models.supervision.AnotherBody.oldEndDateReader
import models.supervision.SupervisionEnd
import org.joda.time.LocalDate
import utils.MappingUtils.constant

case class WhichCurrencies(currencies: Seq[String],
                           usesForeignCurrencies: Option[UsesForeignCurrencies] = None,
                           moneySources: Option[MoneySources] = None)


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

  def oldUsesForeignCurrencies: Reads[Option[UsesForeignCurrencies]] =
    (__ \ "usesForeignCurrencies").readNullable[Boolean] map { ed =>
      ed.fold[Option[UsesForeignCurrencies]](None) { e => e match {
        case true => Some(UsesForeignCurrenciesYes)
        case false => Some(UsesForeignCurrenciesNo)
      }}
    }

  implicit val jsonReads: Reads[WhichCurrencies] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._

    ((__ \ "currencies").read[Seq[String]] and
      ((__ \ "usesForeignCurrencies").readNullable[UsesForeignCurrencies] flatMap {
        case None => oldUsesForeignCurrencies
        case x => constant(x)
    }) and
      (__ \ "moneySources").readNullable[MoneySources]) (WhichCurrencies.apply _)
  }

  implicit val jsonWrites = Json.writes[WhichCurrencies]

}
