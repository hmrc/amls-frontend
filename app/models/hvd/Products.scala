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

package models.hvd

import models.FormTypes._
import jto.validation.forms.UrlFormEncoded
import jto.validation._
import jto.validation.ValidationError
import play.api.libs.json._
import play.api.libs.json.Reads.StringReads
import jto.validation.forms.Rules.{minLength => _, _}
import utils.TraversableValidators.minLengthR
import cats.data.Validated.{Invalid, Valid}

case class Products(items: Set[ItemType]) {
  def sorted = {
    items.toSeq.sortBy( it => it.value)
  }
}

sealed trait ItemType {
  val value: String =
    this match {
      case Alcohol => "01"
      case Tobacco => "02"
      case Antiques => "03"
      case Cars => "04"
      case OtherMotorVehicles => "05"
      case Caravans => "06"
      case Jewellery => "07"
      case Gold => "08"
      case ScrapMetals => "09"
      case MobilePhones => "10"
      case Clothing => "11"
      case Other(_) => "12"
    }
}

case object Alcohol extends ItemType

case object Tobacco extends ItemType

case object Antiques extends ItemType

case object Cars extends ItemType

case object OtherMotorVehicles extends ItemType

case object Caravans extends ItemType

case object Jewellery extends ItemType

case object Gold extends ItemType

case object ScrapMetals extends ItemType

case object MobilePhones extends ItemType

case object Clothing extends ItemType

case class Other(details: String) extends ItemType

object Products{

  import utils.MappingUtils.Implicits._

  val maxDetailsLength = 255
  val otherDetailsType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.hvd.business.sell.other.details") andThen
    maxLength(maxDetailsLength).withMessage("error.invalid.hvd.business.sell.other.details") andThen
    basicPunctuationPattern()

  implicit val formRule: Rule[UrlFormEncoded, Products] =
    From[UrlFormEncoded] { __ =>
      (__ \ "products").read(minLengthR[Set[String]](1).withMessage("error.required.hvd.business.sell.atleast")) flatMap { z =>
        z.map {
          case "01" => Rule[UrlFormEncoded, ItemType](_ => Valid(Alcohol))
          case "02" => Rule[UrlFormEncoded, ItemType](_ => Valid(Tobacco))
          case "03" => Rule[UrlFormEncoded, ItemType](_ => Valid(Antiques))
          case "04" => Rule[UrlFormEncoded, ItemType](_ => Valid(Cars))
          case "05" => Rule[UrlFormEncoded, ItemType](_ => Valid(OtherMotorVehicles))
          case "06" => Rule[UrlFormEncoded, ItemType](_ => Valid(Caravans))
          case "07" => Rule[UrlFormEncoded, ItemType](_ => Valid(Jewellery))
          case "08" => Rule[UrlFormEncoded, ItemType](_ => Valid(Gold))
          case "09" => Rule[UrlFormEncoded, ItemType](_ => Valid(ScrapMetals))
          case "10" => Rule[UrlFormEncoded, ItemType](_ => Valid(MobilePhones))
          case "11" => Rule[UrlFormEncoded, ItemType](_ => Valid(Clothing))
          case "12" =>
            (__ \ "otherDetails").read(otherDetailsType) map Other.apply
          case _ =>
            Rule[UrlFormEncoded, ItemType] { _ =>
              Invalid(Seq((Path \ "products") -> Seq(ValidationError("error.invalid"))))
            }
        }.foldLeft[Rule[UrlFormEncoded, Set[ItemType]]](
          Rule[UrlFormEncoded, Set[ItemType]](_ => Valid(Set.empty))
        ) {
          case (m, n) =>
            n flatMap { x =>
              m map {
                _ + x
              }
            }
        } map Products.apply
      }
    }

  implicit def formWrites = Write[Products, UrlFormEncoded] {
    case Products(transactions) =>
      Map(
        "products[]" -> (transactions map {
          _.value
        }).toSeq
      ) ++ transactions.foldLeft[UrlFormEncoded](Map.empty) {
        case (m, Other(name)) =>
          m ++ Map("otherDetails" -> Seq(name))
        case (m, _) =>
          m
      }
  }

  implicit val jsonReads: Reads[Products] =
    (__ \ "products").read[Set[String]].flatMap { x: Set[String] =>
      x.map {
        case "01" => Reads(_ => JsSuccess(Alcohol)) map identity[ItemType]
        case "02" => Reads(_ => JsSuccess(Tobacco)) map identity[ItemType]
        case "03" => Reads(_ => JsSuccess(Antiques)) map identity[ItemType]
        case "04" => Reads(_ => JsSuccess(Cars)) map identity[ItemType]
        case "05" => Reads(_ => JsSuccess(OtherMotorVehicles)) map identity[ItemType]
        case "06" => Reads(_ => JsSuccess(Caravans)) map identity[ItemType]
        case "07" => Reads(_ => JsSuccess(Jewellery)) map identity[ItemType]
        case "08" => Reads(_ => JsSuccess(Gold)) map identity[ItemType]
        case "09" => Reads(_ => JsSuccess(ScrapMetals)) map identity[ItemType]
        case "10" => Reads(_ => JsSuccess(MobilePhones)) map identity[ItemType]
        case "11" => Reads(_ => JsSuccess(Clothing)) map identity[ItemType]
        case "12" =>
          (JsPath \ "otherDetails").read[String].map(Other.apply _) map identity[ItemType]
        case _ =>
          Reads(_ => JsError((JsPath \ "products") -> play.api.data.validation.ValidationError("error.invalid")))
      }.foldLeft[Reads[Set[ItemType]]](
        Reads[Set[ItemType]](_ => JsSuccess(Set.empty))
      ) {
        (result, data) =>
          data flatMap { m =>
            result.map { n =>
              n + m
            }
          }
      }
    } map Products.apply

  implicit val jsonWrite = Writes[Products] {
    case Products(transactions) =>
      Json.obj(
        "products" -> (transactions map {
          _.value
        }).toSeq
      ) ++ transactions.foldLeft[JsObject](Json.obj()) {
        case (m, Other(name)) =>
          m ++ Json.obj("otherDetails" -> name)
        case (m, _) =>
          m
      }
  }
}

