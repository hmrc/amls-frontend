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

import models.Country
import models.FormTypes._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Success, Write}
import jto.validation._
import play.api.libs.json.{Json, Reads, Writes}

case class SendTheLargestAmountsOfMoney (
                       country_1: Country,
                       country_2: Option[Country] = None,
                       country_3: Option[Country] = None
                     ) {

  def countryList = {
    this.productIterator.collect {
      case Some(Country(name, code)) => Country(name, code)
      case x: Country => x
    }
  }

}

object SendTheLargestAmountsOfMoney {

  implicit val format = Json.format[SendTheLargestAmountsOfMoney]

  implicit val formRule: Rule[UrlFormEncoded, SendTheLargestAmountsOfMoney] = From[UrlFormEncoded] { __ =>
    import utils.MappingUtils.Implicits._
    import jto.validation.forms.Rules._
        ((__ \ "country_1").read[Country].withMessage("error.required.country.name") ~
          (__ \ "country_2").read[Option[Country]] ~
          (__ \ "country_3").read[Option[Country]]
          )(SendTheLargestAmountsOfMoney.apply _)
    }

  implicit val formWrites: Write[SendTheLargestAmountsOfMoney, UrlFormEncoded] = Write {countries =>
      Map(
        "country_1" -> Seq(countries.country_1.code),
        "country_2" -> (countries.country_2.toSeq map { _.code }),
        "country_3" -> (countries.country_3.toSeq map { _.code })
      )
    }
}
