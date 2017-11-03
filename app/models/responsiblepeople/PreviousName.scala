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

import org.joda.time.LocalDate
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json.Json
import utils.DateHelper
import cats.data.Validated.{Invalid, Valid}

case class PreviousName(
                       firstName: Option[String],
                       middleName: Option[String],
                       lastName: Option[String],
                       date: Option[LocalDate] = None
                       ) {

  val formattedDate = date.map(v => DateHelper.formatDate(v))

  def formattedPreviousName(that: PersonName): String = that match {
      case PersonName(first, middle, last, _, _) =>
        Seq(
          firstName orElse Some(first),
          middleName orElse middle,
          lastName orElse Some(last)
        ).flatten[String].mkString(" ")
  }

}

object PreviousName {

  import models.FormTypes._

  implicit val formR: Rule[UrlFormEncoded, PreviousName] = From[UrlFormEncoded] { __ =>

      import jto.validation.forms.Rules._

      type I = (Option[String], Option[String], Option[String])

      val iR = Rule[I, I] {
        case names @ (first, middle, last) if names.productIterator.collectFirst {
          case Some(_) => true
        }.isDefined =>
          Valid(names)
        case _ =>
          Invalid(Seq(Path -> Seq(ValidationError("error.rp.previous.invalid"))))
      }

      // Defining this here because it helps out the compiler with typechecking
      val builder: (I, LocalDate) => PreviousName = {
        case ((first, middle, last), date) =>
          PreviousName(first, middle, last, Some(date))
      }

      (((
        (__ \ "firstName").read(optionR(genericNameRule("error.required.rp.first_name"))) ~
        (__ \ "middleName").read(optionR(genericNameRule())) ~
        (__ \ "lastName").read(optionR(genericNameRule("error.required.rp.last_name")))
      ).tupled andThen iR) ~ (__ \ "date").read(localDateFutureRule))(builder)
    }

  implicit val formW: Write[PreviousName, UrlFormEncoded] = To[UrlFormEncoded] { __ =>

      import jto.validation.forms.Writes._
      import play.api.libs.functional.syntax._

      (
        (__ \ "firstName").write[Option[String]] ~
        (__ \ "middleName").write[Option[String]] ~
        (__ \ "lastName").write[Option[String]] ~
        (__ \ "date").write(optionW(localDateWrite))
      )(unlift(PreviousName.unapply))
    }

  implicit val format = Json.format[PreviousName]
}
