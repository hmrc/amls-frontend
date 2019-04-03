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

package models.estateagentbusiness

import cats.data.Validated.Valid
import jto.validation.{ValidationError, _}
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._

sealed trait RedressScheme

case object ThePropertyOmbudsman extends RedressScheme
case object OmbudsmanServices extends RedressScheme
case object PropertyRedressScheme extends RedressScheme
case class Other(v: String) extends RedressScheme

case object RedressSchemedNo extends RedressScheme

object RedressScheme {
  import utils.MappingUtils.Implicits._

  val maxRedressOtherTypeLength = 255
  val redressOtherType = notEmpty.withMessage("error.required.eab.redress.scheme.name") andThen
    maxLength(maxRedressOtherTypeLength).withMessage("error.invalid.eab.redress.scheme.name")

  implicit val formRedressRule: Rule[UrlFormEncoded, RedressScheme] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
        ( __ \ "propertyRedressScheme").read[String].withMessage("error.required.eab.which.redress.scheme") flatMap {
          case "05" => Rule.fromMapping { _ => Valid(RedressSchemedNo) }
          case "01" => ThePropertyOmbudsman
          case "02" => OmbudsmanServices
          case "03" => PropertyRedressScheme
          case "04" =>
            (__ \ "other").read(redressOtherType) map Other.apply
          case _ =>
            (Path \ "propertyRedressScheme") -> Seq(ValidationError("error.invalid"))
        }
      }


  implicit val formRedressWrites: Write[RedressScheme, UrlFormEncoded] = Write {
    case ThePropertyOmbudsman => Map("propertyRedressScheme" -> "01")
    case OmbudsmanServices => Map("propertyRedressScheme" -> "02")
    case PropertyRedressScheme => Map("propertyRedressScheme" -> "03")
    case Other(value) =>
      Map(
        "propertyRedressScheme" -> "04",
        "other" -> value
      )
    case RedressSchemedNo => Map("propertyRedressScheme" -> "05")
  }

  implicit val jsonRedressReads : Reads[RedressScheme] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "isRedress").read[Boolean] flatMap {
      case true =>
      {
        (__ \ "propertyRedressScheme").read[String].flatMap[RedressScheme] {
          case "01" => ThePropertyOmbudsman
          case "02" => OmbudsmanServices
          case "03" => PropertyRedressScheme
          case "04" =>
            (JsPath \ "propertyRedressSchemeOther").read[String] map {
              Other(_)
            }
          case _ =>
            play.api.data.validation.ValidationError("error.invalid")
        }
      }
      case false => Reads(_ => JsSuccess(RedressSchemedNo))
    }
  }

  implicit val jsonRedressWrites = Writes[RedressScheme] {
      case ThePropertyOmbudsman => Json.obj("isRedress" -> true,"propertyRedressScheme" -> "01")
      case OmbudsmanServices => Json.obj("isRedress" -> true,"propertyRedressScheme" -> "02")
      case PropertyRedressScheme => Json.obj("isRedress" -> true,"propertyRedressScheme" -> "03")
      case Other(value) =>
        Json.obj(
          "isRedress" -> true,
          "propertyRedressScheme" -> "04",
          "propertyRedressSchemeOther" -> value
        )
    case RedressSchemedNo => Json.obj("isRedress" -> false)
  }
}


