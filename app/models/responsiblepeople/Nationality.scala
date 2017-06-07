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

package models.responsiblepeople

import models.Country
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json._

sealed trait Nationality

case object British extends Nationality

case class OtherCountry(name: Country) extends Nationality

object Nationality {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, Nationality] =
    From[UrlFormEncoded] { readerURLFormEncoded =>
      import jto.validation.forms.Rules._
      (readerURLFormEncoded \ "nationality").read[String].withMessage("error.required.nationality") flatMap {
        case "01" => British
        case "02" =>
          (readerURLFormEncoded \ "otherCountry").read[Country] map OtherCountry.apply
        case _ =>
          (Path \ "nationality") -> Seq(ValidationError("error.invalid"))
      }
    }

  implicit val formWrite: Write[Nationality, UrlFormEncoded] = Write {
    case British => "nationality" -> "01"
    case OtherCountry(value) => Map("nationality" -> "02",
      "otherCountry" -> value.code)
  }

  implicit val jsonReads: Reads[Nationality] = {
    import play.api.libs.json._

    (__ \ "nationality").read[String].flatMap[Nationality] {
      case "01" => British
      case "02" => (JsPath \ "otherCountry").read[Country] map OtherCountry.apply
      case _ => play.api.data.validation.ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[Nationality] {
    case British => Json.obj("nationality" -> "01")
    case OtherCountry(value) => Json.obj(
      "nationality" -> "02",
      "otherCountry" -> value
    )
  }

  implicit def getNationality(country: Option[Country]): Option[Nationality] = {
    country match {
      case Some(countryType)=> Some(countryType)
      case _ => None
    }
  }

  implicit def getNationality(country: Country): Nationality = {
    country match {
      case Country("United Kingdom", "GB") => British
      case someCountry => OtherCountry(someCountry)
    }
  }

  implicit def getCountry(nationality: Nationality): Country = {
    nationality match {
      case British =>Country("United Kingdom", "GB")
      case OtherCountry(someCountry) => someCountry
    }
  }
}
