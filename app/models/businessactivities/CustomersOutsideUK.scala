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

package models.businessactivities

import models.Country
import jto.validation.forms.{UrlFormEncoded, _}
import jto.validation.{From, Rule, Write}
import jto.validation._
import play.api.libs.json.{Json, Reads, Writes, __}
import utils.{JsonMapping, TraversableValidators}
import cats.data.Validated.Valid

case class CustomersOutsideUK(countries: Option[Seq[Country]])

sealed trait CustomersOutsideUK0 {

  val minLength = 1
  val maxLength = 10

  import JsonMapping._


  private implicit def rule[A]
  (implicit
   bR: Path => Rule[A, Boolean],
   sR: Path => Rule[A, Seq[String]],
   cR: Rule[Seq[String], Seq[Country]]
  ): Rule[A, CustomersOutsideUK] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits.RichRule
      import TraversableValidators._

      implicit val emptyToNone: String => Option[String] = {
        case "" => None
        case s => Some(s)
      }

      val boolR =
        bR andThen {
          _ withMessage "error.required.ba.select.country"
        }

      val countrySeqR = {
        (seqToOptionSeq[String] andThen flattenR[String] andThen cR)
          .andThen(minLengthR[Seq[Country]](minLength) withMessage "error.invalid.ba.select.country")
          .andThen(maxLengthR[Seq[Country]](maxLength))
      }

      (__ \ "isOutside").read(boolR).flatMap[Option[Seq[Country]]] {
        case true =>
          (__ \ "countries").read(countrySeqR) map Some.apply
        case false =>
          Rule(_ => Valid(None))
      } map CustomersOutsideUK.apply
    }

  val formR: Rule[UrlFormEncoded, CustomersOutsideUK] = {
    import jto.validation.forms.Rules._
    implicitly
  }

  val jsonReads: Reads[CustomersOutsideUK] =
    (__ \ "countries").readNullable[Seq[Country]].map {
      case Some(countries) if countries.isEmpty => CustomersOutsideUK(None)
      case c@Some(countries) => CustomersOutsideUK apply c
      case None => CustomersOutsideUK(None)
    }

  val formWrites: Write[CustomersOutsideUK, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    (
      (__ \ "isOutside").write[Boolean].contramap[Option[_]] {
        case Some(_) => true
        case None => false
      } ~
        (__ \ "countries").write[Option[Seq[Country]]]
      ) (a => (a.countries, a.countries))
  }

  val jsonW = Writes[CustomersOutsideUK] { x =>
    val countries = x.countries.fold[Seq[String]](Seq.empty)(x => x.map(m => m.code))
    countries.nonEmpty match {
      case true => Json.obj(
        "isOutside" -> true,
        "countries" -> countries
      )
      case false =>
        Json.obj(
          "isOutside" -> false
        )
    }
  }
}

object CustomersOutsideUK {

  private object Cache extends CustomersOutsideUK0

  val minLength = Cache.minLength
  val maxLength = Cache.maxLength

  implicit val formR: Rule[UrlFormEncoded, CustomersOutsideUK] = Cache.formR
  implicit val jsonR: Reads[CustomersOutsideUK] = Cache.jsonReads
  implicit val formW: Write[CustomersOutsideUK, UrlFormEncoded] = Cache.formWrites
  implicit val jsonW: Writes[CustomersOutsideUK] = Cache.jsonW
}
