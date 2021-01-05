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

package models.supervision

import jto.validation.forms.Rules.{maxLength, notEmpty}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import models.FormTypes.{basicPunctuationPattern, notEmptyStrip}
import play.api.libs.json.{Json, Reads, Writes}

case class SupervisionEndReasons(endingReason: String)

object SupervisionEndReasons {

  import utils.MappingUtils.Implicits._

  private val reasonMaxLength = 255

  private val reasonRule = notEmptyStrip andThen notEmpty.withMessage("error.required.supervision.reason") andThen
    maxLength(reasonMaxLength).withMessage("error.supervision.end.reason.invalid.maxlength.255") andThen
    basicPunctuationPattern().withMessage("error.supervision.end.reason.invalid")

  implicit val formRule: Rule[UrlFormEncoded, SupervisionEndReasons] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    (__ \ "endingReason").read(reasonRule) map SupervisionEndReasons.apply
  }

  implicit val formWrites: Write[SupervisionEndReasons, UrlFormEncoded] = Write {
    case a: SupervisionEndReasons => {
      Map(
        "anotherBody" -> Seq("true"),
        "endingReason" -> Seq(a.endingReason)
      )
    }
  }


  implicit val jsonReads: Reads[SupervisionEndReasons] = {

    import play.api.libs.json.Reads._
    import play.api.libs.json._

    (__ \ "supervisionEndingReason").read[String].map(SupervisionEndReasons.apply) map identity[SupervisionEndReasons]
  }

  implicit val jsonWrites = Writes[SupervisionEndReasons] {
    case a: SupervisionEndReasons =>
      Json.obj(
        "supervisionEndingReason" -> a.endingReason
      )
  }
}
