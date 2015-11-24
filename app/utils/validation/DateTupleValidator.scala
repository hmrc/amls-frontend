package utils.validation

/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.text.{DateFormatSymbols => JDateFormatSymbols}

import org.joda.time.LocalDate
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import uk.gov.hmrc.play.mappers.DateFields.{day, month, year}


object DateTupleValidator extends FormValidator {


  val dateTuple: Mapping[Option[LocalDate]] = dateTuple(validate = true)

  /**
   * Checks that month is 12 or less and that day is 31 or less.
   * @param messagesKey
   * @param validate
   * @return
   */
  def dateWithinRange(messagesKey:String, validate:Boolean) =
    Constraint[(Option[String],Option[String],Option[String])]("NumberTooHigh")( data =>
      (data._1, data._2, data._3) match {
        case (None, None, None) => Valid
        case (yearOption, monthOption, dayOption) if validate=> {
          import utils.StringHelper
          val mthTrimmed = monthOption.getOrElse("").trim
          val dayTrimmed = dayOption.getOrElse("").trim
          val yearTrimmed = yearOption.getOrElse("").trim
          if (mthTrimmed != "" && dayTrimmed != "" && yearTrimmed != "" && StringHelper.isAllDigits(mthTrimmed) &&
            StringHelper.isAllDigits(dayTrimmed) && StringHelper.isAllDigits(yearTrimmed)) {
            val mth = mthTrimmed.toInt
            val day = dayTrimmed.toInt
            val year = yearTrimmed.toInt
            if (mth <= 12 && day <= 31 && year >= 1000) Valid else Invalid(messagesKey)
          } else {
            Valid
          }
        }
        case (yearOption, monthOption, dayOption) => Valid
      }
    )

  /**
   * Checks that no invalid characters in date input.
   * @param messagesKey
   * @param validate
   * @return
   */
  def invalidCharacterConstraint(messagesKey:String, validate:Boolean) =
    Constraint[(Option[String],Option[String], Option[String])]("InvalidCharacter")( data => {
      (data._1, data._2, data._3) match {
        case (None, None, None) => Valid
        case (yearOption, monthOption, dayOption) => {
          try {
            val y = yearOption.getOrElse(throw new Exception("Year missing")).trim
            if (y.length != 4) {
              throw new Exception("Year must be 4 digits")
            }
            new LocalDate(y.toInt, monthOption.getOrElse(throw new Exception("Month missing")).trim.toInt,
              dayOption.getOrElse(throw new Exception("Day missing")).trim.toInt)
            Valid
          } catch {
            case _: Throwable => if (validate) {
              Invalid(messagesKey)
            } else {
              Valid
            }
          }
        }
      }
    }
    )

  def fieldEmptyConstraint(messagesKey:String, validate:Boolean) =
    Constraint[(Option[String],Option[String],Option[String])]("FieldEmpty")( data =>
      (data._1, data._2, data._3) match {
        case (None, None, None) => Valid
        case (yearOption, monthOption, dayOption) if validate=> {
          val mthTrimmed = monthOption.getOrElse("").trim
          val dayTrimmed = dayOption.getOrElse("").trim
          val yearTrimmed = yearOption.getOrElse("").trim

          if (mthTrimmed == "" || dayTrimmed == "" || yearTrimmed == "") {
            Invalid(messagesKey)
          } else {
            Valid
          }

        }
        case (yearOption, monthOption, dayOption) => Valid
      }
    )

  def mandatoryDateTuple(blankValueMessageKey: String = "error.invalid.date.format",
                         invalidValueMessageKey: String = "error.invalid.date.format",
                         invalidNumberTooHighMessageKey: String = "error.invalid.date.format"): Mapping[LocalDate]
  = dateTuple(true,blankValueMessageKey, invalidValueMessageKey, invalidNumberTooHighMessageKey)
    .verifying(blankValueMessageKey,
      data => data.isDefined).transform(o =>
        o.fold(LocalDate.now()) {x=>x},
        v => Option(v))

  def dateTuple(validate: Boolean = true,
                invalidEmptyMessageKey: String = "error.invalid.date.format",
                invalidValueMessageKey: String = "error.invalid.date.format",
                invalidNumberTooHighMessageKey: String = "error.invalid.date.format") = tuple(
    year -> optional(text),
    month -> optional(text),
    day -> optional(text)
  ).verifying(stopOnFirstFail(
    dateWithinRange(invalidNumberTooHighMessageKey, validate),
    fieldEmptyConstraint(invalidEmptyMessageKey, validate),
    invalidCharacterConstraint(invalidValueMessageKey, validate)
  )).transform(
  {
    case (Some(y), Some(m), Some(d)) =>
      try {
        Some(new LocalDate(y.trim.toInt, m.trim.toInt, d.trim.toInt))
      } catch {
        case e: Exception =>
          if (validate) {
            throw e
          } else {
            None
          }
      }
    case (a, b, c) => None
  },
  (date: Option[LocalDate]) => date match {
    case Some(d) => (Some(d.getYear.toString), Some(d.getMonthOfYear.toString), Some(d.getDayOfMonth.toString))
    case _ => (None, None, None)
  }
  )
}
