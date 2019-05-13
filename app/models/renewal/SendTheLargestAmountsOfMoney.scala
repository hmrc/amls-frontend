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

package models.renewal

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Path, Rule, RuleLike, To, Write, WriteLike}
import models.Country
import play.api.libs.json.{Json, Reads, Writes}
import utils.TraversableValidators
import utils.TraversableValidators.{flattenR, maxLengthR, minLengthR, seqToOptionSeq}

//case class SendTheLargestAmountsOfMoney(
//                       country_1: Country,
//                       country_2: Option[Country] = None,
//                       country_3: Option[Country] = None
//                     ) {
//
//  def countryList = {
//    this.productIterator.collect {
//      case Some(Country(name, code)) => Country(name, code)
//      case x: Country => x
//    }
//  }
//
//}

//object SendTheLargestAmountsOfMoney {
//
//  implicit val format = Json.format[SendTheLargestAmountsOfMoney]
//
//  implicit val formRule: Rule[UrlFormEncoded, SendTheLargestAmountsOfMoney] = From[UrlFormEncoded] { __ =>
//    import jto.validation.forms.Rules._
//    import utils.MappingUtils.Implicits._
//        ((__ \ "country_1").read[Country].withMessage("error.required.renewal.country.name") ~
//          (__ \ "country_2").read[Option[Country]] ~
//          (__ \ "country_3").read[Option[Country]]
//          )(SendTheLargestAmountsOfMoney.apply _)
//    }
//
//  implicit val formWrites: Write[SendTheLargestAmountsOfMoney, UrlFormEncoded] = Write { countries =>
//      Map(
//        "country_1" -> Seq(countries.country_1.code),
//        "country_2" -> (countries.country_2.toSeq map { _.code }),
//        "country_3" -> (countries.country_3.toSeq map { _.code })
//      )
//    }
//
//  implicit def convert(model: SendTheLargestAmountsOfMoney): models.moneyservicebusiness.SendTheLargestAmountsOfMoney = {
//    models.moneyservicebusiness.SendTheLargestAmountsOfMoney(model.countryList.toSeq)
//  }
//}

case class SendTheLargestAmountsOfMoney (countries: Seq[Country])

private sealed trait SendTheLargestAmountsOfMoney0 {

  private implicit def rule[A]
  (implicit
   a: Path => RuleLike[A, Seq[String]],
   cR: Rule[Seq[String], Seq[Country]]
  ): Rule[A, SendTheLargestAmountsOfMoney] =
    From[A] { __ =>

      import TraversableValidators._
      import utils.MappingUtils.Implicits.RichRule

      implicit val emptyToNone: String => Option[String] = {
        case "" => None
        case s => Some(s)
      }

      val seqR = {
        (seqToOptionSeq[String] andThen flattenR[String] andThen cR)
          .andThen(minLengthR[Seq[Country]](1) withMessage "error.invalid.country")
          .andThen(maxLengthR[Seq[Country]](3))
      }

      (__ \ "largestAmountsOfMoney").read(seqR) map SendTheLargestAmountsOfMoney.apply
    }

  private implicit def write[A]
  (implicit
   a: Path => WriteLike[Seq[Country], A]
  ): Write[SendTheLargestAmountsOfMoney, A] =
    To[A] { __ =>
      import play.api.libs.functional.syntax.unlift
      (__ \ "largestAmountsOfMoney").write[Seq[Country]] contramap unlift(SendTheLargestAmountsOfMoney.unapply)
    }

  val formR: Rule[UrlFormEncoded, SendTheLargestAmountsOfMoney] = {
    import jto.validation.forms.Rules._
    implicitly
  }

  val jsonR: Reads[SendTheLargestAmountsOfMoney] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    ((__ \ "country_1").read[Country] and
      (__ \ "country_2").readNullable[Country] and
      (__ \ "country_3").readNullable[Country]).tupled map {
      case (a, Some(b), Some(c)) => SendTheLargestAmountsOfMoney(Seq(a, b, c))
      case (a, Some(b), None) => SendTheLargestAmountsOfMoney(Seq(a, b))
      case (a, None, None) => SendTheLargestAmountsOfMoney(Seq(a))
      case (_, _, _) => SendTheLargestAmountsOfMoney(Seq())
    }
  }

  val formW: Write[SendTheLargestAmountsOfMoney, UrlFormEncoded] = {
    import jto.validation.forms.Writes._
    import utils.MappingUtils.spm
    implicitly
  }

  val jsonW = Writes[SendTheLargestAmountsOfMoney] { lom =>
    lom.countries match {
      case Seq(a, b, c) => Json.obj("country_1" -> a, "country_2" -> b, "country_3" -> c)
      case Seq(a, b) => Json.obj("country_1" -> a, "country_2" -> b)
      case Seq(a) => Json.obj("country_1" -> a)
      case _ => Json.obj()
    }
  }
}

object SendTheLargestAmountsOfMoney {

  private object Cache extends SendTheLargestAmountsOfMoney0

  implicit val formR: Rule[UrlFormEncoded, SendTheLargestAmountsOfMoney] = Cache.formR
  implicit val formW: Write[SendTheLargestAmountsOfMoney, UrlFormEncoded] = Cache.formW
  implicit val jsonR: Reads[SendTheLargestAmountsOfMoney] = Cache.jsonR
  implicit val jsonW: Writes[SendTheLargestAmountsOfMoney] = Cache.jsonW

  implicit def convert(model: SendTheLargestAmountsOfMoney): models.moneyservicebusiness.SendTheLargestAmountsOfMoney = {
    models.moneyservicebusiness.SendTheLargestAmountsOfMoney(model.countries)
  }
}
