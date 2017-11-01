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

package models.responsiblepeople

import cats.data.Validated.Valid
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import play.api.libs.json.{Json, Writes => _}
import utils.MappingUtils.Implicits._

case class LegalName(previousName: Option[PreviousName])

object LegalName {

  implicit val formats = Json.format[LegalName]

  implicit val formRule: Rule[UrlFormEncoded, LegalName] =
    From[UrlFormEncoded] { __ =>
          (__ \ "hasPreviousName").read[Boolean].withMessage("error.required.rp.hasPreviousName").flatMap[Option[PreviousName]] {
            case true =>
              (__ \ "previous").read[PreviousName] map Some.apply
            case false =>
              Rule(_ => Valid(None))
          } map LegalName.apply
    }

  implicit val formWrite = Write[LegalName, UrlFormEncoded] {
    model =>
      model.previousName match {
        case Some(previous) =>
          Map(
            "hasPreviousName" -> Seq("true"),
            "previous.firstName" -> Seq(previous.firstName getOrElse ""),
            "previous.middleName" -> Seq(previous.middleName getOrElse ""),
            "previous.lastName" -> Seq(previous.lastName getOrElse "")
          )
        case None =>
          Map("hasPreviousName" -> Seq("false"))
      }
  }
}