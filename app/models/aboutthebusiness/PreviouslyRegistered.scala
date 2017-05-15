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

package models.aboutthebusiness

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json._
import cats.data.Validated.{Invalid, Valid}

sealed trait PreviouslyRegistered

case class PreviouslyRegisteredYes(value: String) extends PreviouslyRegistered

case object PreviouslyRegisteredNo extends PreviouslyRegistered

object PreviouslyRegistered {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, PreviouslyRegistered] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    val mlrRegNoRegex = "^([0-9]{8})$".r

    (__ \ "previouslyRegistered").read[Boolean].withMessage("error.required.atb.previously.registered") flatMap {
      case true =>
        (__ \ "prevMLRRegNo").read(pattern(mlrRegNoRegex).withMessage("error.invalid.mlr.number")) map PreviouslyRegisteredYes.apply
      case false => Rule.fromMapping { _ => Valid(PreviouslyRegisteredNo) }
    }
  }

  implicit val formWrites: Write[PreviouslyRegistered, UrlFormEncoded] = Write {
    case PreviouslyRegisteredYes(value) =>
      Map("previouslyRegistered" -> Seq("true"),
        "prevMLRRegNo" -> Seq(value)
      )
    case PreviouslyRegisteredNo => Map("previouslyRegistered" -> Seq("false"))
  }

  implicit val jsonReads: Reads[PreviouslyRegistered] =
    (__ \ "previouslyRegistered").read[Boolean] flatMap {
      case true => (__ \ "prevMLRRegNo").read[String] map PreviouslyRegisteredYes.apply
      case false => Reads(_ => JsSuccess(PreviouslyRegisteredNo))
    }

  implicit val jsonWrites = Writes[PreviouslyRegistered] {
    case PreviouslyRegisteredYes(value) => Json.obj(
      "previouslyRegistered" -> true,
      "prevMLRRegNo" -> value
    )
    case PreviouslyRegisteredNo => Json.obj("previouslyRegistered" -> false)
  }
}
