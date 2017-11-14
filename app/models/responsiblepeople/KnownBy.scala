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
import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import models.FormTypes._
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._

case class KnownBy(otherNames: Option[String]) {

  val otherName = Seq(otherNames).flatten[String].mkString(" ")

  def isDefined(kb: KnownBy): Boolean = kb match {
    case KnownBy(None) => false
    case _ => true
  }
}

object KnownBy {

  import play.api.libs.json._

  implicit val formats = Json.format[KnownBy]



  val otherNamesLength = 140
  val otherNamesType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.rp.otherNames") andThen
    maxLength(otherNamesLength).withMessage("error.invalid.maxlength.140") andThen
    basicPunctuationPattern()

  implicit val formRule: Rule[UrlFormEncoded, KnownBy] =
    From[UrlFormEncoded] { __ =>
      (__ \ "hasOtherNames").read[Boolean].withMessage("error.required.rp.hasOtherNames") flatMap  {
        case true => (__ \ "otherNames").read(otherNamesType) map { x => KnownBy(Some(x))}
        case false => Rule.fromMapping { _ => Valid(KnownBy(None)) }
      }
    }

  implicit val formWrite = Write[KnownBy, UrlFormEncoded] {
    model =>
      model.isDefined(model) match {
        case true =>
          Map(
            "hasOtherNames" -> Seq("true"),
            "otherNames" -> Seq(model.otherNames getOrElse "")
          )
        case _ =>
          Map("hasOtherNames" -> Seq("false"))
      }
  }
}