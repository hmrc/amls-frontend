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

import cats.data.Validated.{Invalid, Valid}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{ValidationError, _}
import play.api.libs.json.{Json, Writes => _}
import utils.MappingUtils.Implicits._
import jto.validation.forms.Writes._
import play.api.libs.functional.syntax._

case class PreviousName(
                         firstName: Option[String],
                         middleName: Option[String],
                         lastName: Option[String]
                       ) {

  val fullName = Seq(firstName, middleName, lastName).flatten[String].mkString(" ")

  def isDefined(pn: PreviousName): Boolean = pn match {
    case PreviousName(None, None, None) => false
    case _ => true
  }

}

object PreviousName {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._


  implicit val formR: Rule[UrlFormEncoded, PreviousName] = From[UrlFormEncoded] { __ =>

    import jto.validation.forms.Rules._

    type I = (Option[String], Option[String], Option[String])

    val iR = Rule[I, I] {
      case names@(first, middle, last) if names.productIterator.collectFirst {
        case Some(_) => true
      }.isDefined =>
        Valid(names)
      case _ =>
        Invalid(Seq(Path -> Seq(ValidationError("error.rp.previous.invalid"))))
    }

    (__ \ "hasPreviousName").read[Boolean].withMessage("error.required.rp.hasPreviousName") flatMap {
      case true => (
        (
          (__ \ "firstName").read(optionR(genericNameRule("error.required.rp.first_name"))) ~
            (__ \ "middleName").read(optionR(genericNameRule())) ~
            (__ \ "lastName").read(optionR(genericNameRule("error.required.rp.last_name")))
          ).tupled andThen iR).map(t => PreviousName(t._1, t._2, t._3))
      case false => Rule.fromMapping { _ => Valid(PreviousName(None, None, None)) }
    }
  }

  implicit val formW: Write[PreviousName, UrlFormEncoded] = Write {

      case PreviousName(None, None, None) =>
        Map("hasPreviousName" -> Seq("false"))
      case PreviousName(a, b, c) =>
        Map(
          "hasPreviousName" -> Seq("true"),
          "firstName" -> Seq(a.toString),
          "middleName" -> Seq(b.toString),
          "lastName" -> Seq(c.toString)
        )

    }

  implicit val format = Json.format[PreviousName]
}
