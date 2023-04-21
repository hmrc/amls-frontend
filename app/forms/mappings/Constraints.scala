/*
 * Copyright 2023 HM Revenue & Customs
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

package forms.mappings

import org.joda.time.{LocalDate => JodaLocalDate}
import play.api.data.validation.{Constraint, Invalid, Valid}

import java.time.LocalDate

trait Constraints {

  protected val basicPunctuationRegex = "^[a-zA-Z0-9\u00C0-\u00FF !#$%&'‘’\"“”«»()*+,./:;=?@\\[\\]|~£€¥\\u005C\u2014\u2013\u2010\u005F\u005E\u0060\u000A\u000D\u002d]+$"

  protected def firstError[A](constraints: Constraint[A]*): Constraint[A] =
    Constraint {
      input =>
        constraints
          .map(_.apply(input))
          .find(_ != Valid)
          .getOrElse(Valid)
    }

  protected def minimumValue[A](minimum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>

        import ev._

        if (input >= minimum) {
          Valid
        } else {
          Invalid(errorKey, minimum)
        }
    }

  protected def maximumValue[A](maximum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>

        import ev._

        if (input <= maximum) {
          Valid
        } else {
          Invalid(errorKey, maximum)
        }
    }

  protected def inRange[A](minimum: A, maximum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint {
      input =>

        import ev._

        if (input >= minimum && input <= maximum) {
          Valid
        } else {
          Invalid(errorKey, minimum, maximum)
        }
    }

  protected def regexp(regex: String, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.matches(regex) =>
        Valid
      case _ =>
        Invalid(errorKey, regex)
    }

  protected def minLength(minimum: Int, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.length >= minimum =>
        Valid
      case _ =>
        Invalid(errorKey, minimum)
    }

  protected def maxLength(maximum: Int, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.length <= maximum =>
        Valid
      case _ =>
        Invalid(errorKey, maximum)
    }

  protected def correctLength(correctLength: Int, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.length == correctLength =>
        Valid
      case _ =>
        Invalid(errorKey, correctLength)
    }

  protected def maxLengthInt(maximum: Int, errorKey: String): Constraint[Int] =
    Constraint {
      case str if str <= maximum =>
        Valid
      case _ =>
        Invalid(errorKey, maximum)
    }

  protected def maxDate(maximum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isAfter(maximum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }

  protected def minDate(minimum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isBefore(minimum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }

  protected def jodaMaxDate(maximum: JodaLocalDate, errorKey: String, args: Any*): Constraint[JodaLocalDate] =
    Constraint {
      case date if date.isAfter(maximum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }

  protected def jodaMinDate(minimum: JodaLocalDate, errorKey: String, args: Any*): Constraint[JodaLocalDate] =
    Constraint {
      case date if date.isBefore(minimum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }

  protected def nonEmptySeq(errorKey: String): Constraint[Seq[_]] =
    Constraint {
      case seq if seq.nonEmpty =>
        Valid
      case _ =>
        Invalid(errorKey)
    }
}
