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
import models.FormTypes._
import play.api.libs.json.{Json, Writes => _}
import utils.MappingUtils.Implicits._

case class LegalName( firstName: String,
                      middleName: Option[String],
                      lastName: String)

object LegalName {

  implicit val formats = Json.format[LegalName]

  implicit val formRule: Rule[UrlFormEncoded, LegalName] =
    From[UrlFormEncoded] { __ =>
          (__ \ "hasPreviousName").read[Boolean].withMessage("error.required.rp.hasPreviousName") flatMap  {
            case true => (
                (__ \ "firstName").read(genericNameRule("error.required.rp.first_name")) ~
                (__ \ "middleName").read(optionR(genericNameRule())) ~
                (__ \ "lastName").read(genericNameRule("error.required.rp.last_name"))
              ) (LegalName.apply _)
            case false => Rule.fromMapping { _ => Valid(LegalName("", None,"")) }
          }
    }

  implicit val formWrite = Write[LegalName, UrlFormEncoded] {
        case a: LegalName =>
          Map(
            "hasPreviousName" -> Seq("true"),
            "firstName" -> Seq(a.firstName),
            "middleName" -> Seq(a.middleName getOrElse ""),
            "lastName" -> Seq(a.lastName)
          )
        case _ =>
          Map("hasPreviousName" -> Seq("false"))
      }
}