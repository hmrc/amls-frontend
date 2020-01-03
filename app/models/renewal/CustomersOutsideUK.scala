/*
 * Copyright 2020 HM Revenue & Customs
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

package models.renewal

import models.{Country, businessactivities}
import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import jto.validation._
import play.api.libs.json.{Json, Reads, Writes}
import utils.{JsonMapping, TraversableValidators}

case class CustomersOutsideUK(countries: Option[Seq[Country]])

sealed trait CustomersOutsideUK0 {

  val minLength = 1
  val maxLength = 10

  import JsonMapping._


  private implicit def rule[A]
  (implicit
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

      val countrySeqR = {
        (seqToOptionSeq[String] andThen flattenR[String] andThen cR)
          .andThen(minLengthR[Seq[Country]](minLength) withMessage "error.required.renewal.customer.country.name")
          .andThen(maxLengthR[Seq[Country]](maxLength))
      }

          (__ \ "countries").read(countrySeqR) map Some.apply
      } map CustomersOutsideUK.apply

  private  def write: Write[CustomersOutsideUK, UrlFormEncoded] = Write {
    case CustomersOutsideUK(Some(countries)) => countries.zipWithIndex.map(i => s"countries[${i._2}]" -> Seq(i._1.code)).toMap
    case _ => throw new IllegalArgumentException("No countries added")
  }

  val formR: Rule[UrlFormEncoded, CustomersOutsideUK] = {
    import jto.validation.forms.Rules._
    implicitly
  }

  val jsonR: Reads[CustomersOutsideUK] = {
    import jto.validation.playjson.Rules.{JsValue => _, pickInJson => _, _}
    implicitly
  }

  implicit val formW: Write[CustomersOutsideUK, UrlFormEncoded] = write


  val jsonW = Writes[CustomersOutsideUK] { customer =>
    val countries = customer.countries.fold[Seq[String]](Seq.empty)(customer => customer.map(country => country.code))
    Json.obj(
        "countries" -> countries
      )
    }
}

object CustomersOutsideUK {

  private object Cache extends CustomersOutsideUK0

  val minLength = Cache.minLength
  val maxLength = Cache.maxLength

  implicit val formR: Rule[UrlFormEncoded, CustomersOutsideUK] = Cache.formR
  implicit val jsonR: Reads[CustomersOutsideUK] = Cache.jsonR
  implicit val formW: Write[CustomersOutsideUK, UrlFormEncoded] = Cache.formW
  implicit val jsonW: Writes[CustomersOutsideUK] = Cache.jsonW

  implicit def convert(model: CustomersOutsideUK): businessactivities.CustomersOutsideUK =
    models.businessactivities.CustomersOutsideUK(model.countries)
}
