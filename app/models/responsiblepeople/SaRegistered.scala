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

package models.responsiblepeople

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import jto.validation.forms.Rules._
import utils.MappingUtils.Implicits._
import cats.data.Validated.{Invalid, Valid}

sealed trait SaRegistered

case class SaRegisteredYes(value: String) extends SaRegistered

case object SaRegisteredNo extends SaRegistered

object SaRegistered {

  val utrTypeRegex = "^[0-9]{10}$".r
  val utrType = notEmpty.withMessage("error.required.utr.number") andThen pattern(utrTypeRegex).withMessage("error.invalid.length.utr.number")

  implicit val formRule: Rule[UrlFormEncoded, SaRegistered] = From[UrlFormEncoded] { __ =>
  import jto.validation.forms.Rules._
    (__ \ "saRegistered").read[Boolean].withMessage("error.required.sa.registration") flatMap {
      case true =>
        (__ \ "utrNumber").read(utrType) map (SaRegisteredYes.apply)
      case false => Rule.fromMapping { _ => Valid(SaRegisteredNo) }
    }
  }

  implicit val formWrites: Write[SaRegistered, UrlFormEncoded] = Write {
    case SaRegisteredYes(value) =>
      Map("saRegistered" -> Seq("true"),
          "utrNumber" -> Seq(value)
      )
    case SaRegisteredNo => Map("saRegistered" -> Seq("false"))
  }

  implicit val jsonReads: Reads[SaRegistered] =
    (__ \ "saRegistered").read[Boolean] flatMap {
      case true => (__ \ "utrNumber").read[String] map (SaRegisteredYes.apply _)
      case false => Reads(_ => JsSuccess(SaRegisteredNo))
    }

  implicit val jsonWrites = Writes[SaRegistered] {
    case SaRegisteredYes(value) => Json.obj(
      "saRegistered" -> true,
      "utrNumber" -> value
    )
    case SaRegisteredNo => Json.obj("saRegistered" -> false)
  }

}


