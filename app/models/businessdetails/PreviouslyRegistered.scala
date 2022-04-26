/*
 * Copyright 2022 HM Revenue & Customs
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

package models.businessdetails

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import cats.data.Validated.{Valid}

sealed trait PreviouslyRegistered

case class PreviouslyRegisteredYes(value: Option[String]) extends PreviouslyRegistered
case object PreviouslyRegisteredNo extends PreviouslyRegistered

object PreviouslyRegistered {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, PreviouslyRegistered] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    (__ \ "previouslyRegistered").read[Boolean].withMessage("error.required.atb.previously.registered") flatMap {
      case true => Rule.fromMapping { _ => Valid(PreviouslyRegisteredYes(None)) }
      case false => Rule.fromMapping { _ => Valid(PreviouslyRegisteredNo) }
    }
  }

  implicit val formWrites: Write[PreviouslyRegistered, UrlFormEncoded] = Write {
    case PreviouslyRegisteredYes(Some(value)) =>
      Map("previouslyRegistered" -> Seq("true"),
        "prevMLRRegNo" -> Seq(value)
      )
    case PreviouslyRegisteredYes(None) =>
      Map("previouslyRegistered" -> Seq("true"))
    case PreviouslyRegisteredNo =>
      Map("previouslyRegistered" -> Seq("false"))
  }

  implicit val jsonReads: Reads[PreviouslyRegistered] =
    (__ \ "previouslyRegistered").read[Boolean] flatMap {
      case true => (__ \ "prevMLRRegNo").readNullable[String] map PreviouslyRegisteredYes.apply
      case false => Reads(_ => JsSuccess(PreviouslyRegisteredNo))
    }

  implicit val jsonWrites = Writes[PreviouslyRegistered] {
    case PreviouslyRegisteredYes(Some(value)) =>
      Json.obj(
        "previouslyRegistered" -> true,
        "prevMLRRegNo" -> value
      )
    case PreviouslyRegisteredYes(None) =>
      Json.obj(
        "previouslyRegistered" -> true
      )
    case PreviouslyRegisteredNo => Json.obj("previouslyRegistered" -> false)
  }
}
