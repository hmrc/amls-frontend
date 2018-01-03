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

package models.moneyservicebusiness

import models.Country
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.{Json, Reads, Writes}
import utils.{JsonMapping, TraversableValidators}
import cats.data.Validated.{Invalid, Valid}

case class BranchesOrAgents(branches: Option[Seq[Country]])

sealed trait BranchesOrAgents0 {

  val minLength = 1
  val maxLength = 10

  import JsonMapping._
  import utils.MappingUtils.MonoidImplicits._

  private implicit def rule[A]
  (implicit
   bR: Path => Rule[A, Boolean],
   sR: Path => Rule[A, Seq[String]],
   cR: Rule[Seq[String], Seq[Country]]
  ): Rule[A, BranchesOrAgents] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits.RichRule
      import TraversableValidators._

       implicit val emptyToNone: String => Option[String] = {
          case "" => None
          case s => Some(s)
       }

      val boolR =
        bR andThen {
          _ withMessage "error.required.hasCountries.msb.branchesOrAgents"
        }

      val countrySeqR = {
        (seqToOptionSeq[String] andThen flattenR[String] andThen cR)
          .andThen(minLengthR[Seq[Country]](minLength) withMessage "error.invalid.countries.msb.branchesOrAgents")
          .andThen(maxLengthR[Seq[Country]](maxLength))
      }

      (__ \ "hasCountries").read(boolR).flatMap[Option[Seq[Country]]] {
        case true =>
          (__ \ "countries").read(countrySeqR) map Some.apply
        case false =>
          Rule(_ => Valid(None))
      } map BranchesOrAgents.apply
    }


  private implicit def write
  (implicit
   mon: cats.Monoid[UrlFormEncoded],
   a: Path => WriteLike[Boolean, UrlFormEncoded],
   b: Path => WriteLike[Option[Seq[Country]], UrlFormEncoded]
  ): Write[BranchesOrAgents, UrlFormEncoded] =
    To[UrlFormEncoded] { __ =>
      (
        (__ \ "hasCountries").write[Boolean].contramap[Option[Seq[_]]] {
          case Some(x) if x.isEmpty => false
          case Some(_) => true
          case None => false
        } ~
          (__ \ "countries").write[Option[Seq[Country]]]
        )(a => (a.branches, a.branches))
    }

  val formR: Rule[UrlFormEncoded, BranchesOrAgents] = {
    import jto.validation.forms.Rules._
    implicitly
  }

  val jsonR: Reads[BranchesOrAgents] = {
    import jto.validation.playjson.Rules.{JsValue => _, pickInJson => _, _}
    implicitly
  }

  val formW: Write[BranchesOrAgents, UrlFormEncoded] = {
    import cats.implicits._
    import jto.validation.forms.Writes._
    implicitly
  }

  val jsonW = Writes[BranchesOrAgents] {x =>
    val countries = x.branches.fold[Seq[String]](Seq.empty)(x => x.map(m => m.code))
    countries.nonEmpty match {
      case true =>  Json.obj(
        "hasCountries" -> true,
        "countries" -> countries
      )
      case false =>
        Json.obj(
          "hasCountries" -> false
        )
    }
  }
}

object BranchesOrAgents {

  private object Cache extends BranchesOrAgents0

  val minLength = Cache.minLength
  val maxLength = Cache.maxLength

  implicit val formR: Rule[UrlFormEncoded, BranchesOrAgents] = Cache.formR
  implicit val jsonR: Reads[BranchesOrAgents] = Cache.jsonR
  implicit val formW: Write[BranchesOrAgents, UrlFormEncoded] = Cache.formW
  implicit val jsonW: Writes[BranchesOrAgents] = Cache.jsonW
}
