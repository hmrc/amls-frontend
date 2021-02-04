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

package models.moneyservicebusiness

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import models.{currencies => currenciesSeq, renewal => r}
import play.api.libs.json._
import utils.MappingUtils.Implicits._
import utils.MappingUtils.constant
import utils.{GenericValidators, TraversableValidators}

case class WhichCurrencies(currencies: Seq[String],
                           usesForeignCurrencies: Option[UsesForeignCurrencies] = None,
                           moneySources: Option[MoneySources] = None) {

  def currencies(p: Seq[String]): WhichCurrencies =
    this.copy(currencies = p)

  def usesForeignCurrencies(p: UsesForeignCurrencies): WhichCurrencies =
    this.copy(usesForeignCurrencies = Some(p))

  def moneySources(p: MoneySources): WhichCurrencies =
    this.copy(moneySources = Some(p))
}

object WhichCurrencies {

  def convert(wc: WhichCurrencies): r.WhichCurrencies = {
    r.WhichCurrencies(wc.currencies, wc.usesForeignCurrencies match {
      case Some(UsesForeignCurrenciesYes) => Some(r.UsesForeignCurrenciesYes)
      case Some(UsesForeignCurrenciesNo) => Some(r.UsesForeignCurrenciesNo)
    }, wc.moneySources match {
      case Some(ms) => {
        val bms = ms.bankMoneySource.fold[Option[r.BankMoneySource]](None) {
          b => Some(r.BankMoneySource(b.bankNames))
        }

        val wms = ms.wholesalerMoneySource.fold[Option[r.WholesalerMoneySource]](None) {
          b => Some(r.WholesalerMoneySource(b.wholesalerNames))
        }

        val cms = ms.customerMoneySource.fold[Option[Boolean]](None) {
          b => Some(b)
        }

        Some(r.MoneySources(bms, wms, cms))
      }
      case _ => None
    })
  }

  val emptyToNone: String => Option[String] = { x =>
    x.trim() match {
      case "" => None
      case s => Some(s)
    }
  }

  private val currencyListType = TraversableValidators.seqToOptionSeq(emptyToNone) andThen
    TraversableValidators.flattenR[String] andThen
    TraversableValidators.minLengthR[Seq[String]](1).withMessage("error.invalid.msb.wc.currencies") andThen
    GenericRules.traversableR(GenericValidators.inList(currenciesSeq)).withMessage("error.invalid.msb.wc.currencies.format")


  implicit def formRule: Rule[UrlFormEncoded, WhichCurrencies] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    (__ \ "currencies").read(currencyListType).flatMap(r => WhichCurrencies(r.toSeq))
  }

  implicit val formWrite: Write[WhichCurrencies, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    (__ \ "currencies").write[Seq[String]] contramap {x =>x.currencies}
  }

  def oldUsesForeignCurrenciesReader: Reads[Option[UsesForeignCurrencies]] =
    (__ \ "usesForeignCurrencies").readNullable[Boolean] map { ed =>
      ed.fold[Option[UsesForeignCurrencies]](None) { e => e match {
        case true => Some(UsesForeignCurrenciesYes)
        case false => Some(UsesForeignCurrenciesNo)
      }}
    }

  def oldMoneySourcesReader: Reads[Option[MoneySources]] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    val bankMoney = ((__ \ "bankMoneySource").readNullable[String] and
      (__ \ "bankNames").readNullable[String])((a, b) => (a, b) match {
      case (Some("Yes"), Some(names)) => Some(BankMoneySource(names))
      case _ => None
    })

    val wholeSalerMoney = ((__ \ "wholesalerMoneySource").readNullable[String] and
      (__ \ "wholesalerNames").readNullable[String])((a, b) => (a, b) match {
      case (Some("Yes"), Some(names)) => Some(WholesalerMoneySource(names))
      case _ => None
    })

    val customerMoney = (__ \ "customerMoneySource").readNullable[String] map {
      case Some("Yes") => true
      case _ => false
    }

    (bankMoney and wholeSalerMoney and customerMoney) ((a, b, c) => Some(MoneySources(a, b, Some(c))))
  }

  implicit val jsonReads: Reads[WhichCurrencies] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._

    ((__ \ "currencies").read[Seq[String]] and
      ((__ \ "usesForeignCurrencies").read(Reads.optionNoError[UsesForeignCurrencies]) flatMap {
        case None => oldUsesForeignCurrenciesReader
        case x => constant(x)
    }) and
      ((__ \ "moneySources").readNullable[MoneySources]
        flatMap {
      case None => oldMoneySourcesReader
      case x => constant(x)
    }
        )) (WhichCurrencies.apply _)
  }

  implicit val jsonWrites: Writes[WhichCurrencies] = Writes {
    case wc: WhichCurrencies => {
      Json.obj("currencies" -> wc.currencies,
      "usesForeignCurrencies" -> wc.usesForeignCurrencies) ++
        Json.obj("moneySources" -> wc.moneySources)
    }
  }

}
