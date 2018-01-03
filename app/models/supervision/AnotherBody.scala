/*
 * Copyright 2018 HM Revenue & Customs
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

import models.FormTypes._
import org.joda.time.LocalDate
import jto.validation.forms.Rules._
import jto.validation._
import jto.validation.forms._
import play.api.libs.json.{Json, Reads, Writes}
import cats.data.Validated.{Invalid, Valid}
import models.ValidationRule



sealed trait AnotherBody

case class AnotherBodyYes(supervisorName: String,
                          startDate: LocalDate,
                          endDate: LocalDate,
                          endingReason: String) extends AnotherBody

case object AnotherBodyNo extends AnotherBody


object AnotherBody {

  import utils.MappingUtils.Implicits._

  private val supervisorMaxLength = 140
  private val reasonMaxLength = 255

  private val supervisorRule = notEmptyStrip andThen
    notEmpty.withMessage("error.required.supervision.supervisor") andThen
    maxLength(supervisorMaxLength).withMessage("error.invalid.supervision.supervisor") andThen
    basicPunctuationPattern()

  private val reasonRule = notEmptyStrip andThen notEmpty.withMessage("error.required.supervision.reason") andThen
    maxLength(reasonMaxLength).withMessage("error.invalid.maxlength.255") andThen
    basicPunctuationPattern()

  type ValidationRuleType = (String, LocalDate, LocalDate, String)

  val validationRule: ValidationRule[ValidationRuleType] = Rule[ValidationRuleType, ValidationRuleType] {
    case x@(_, d1, d2, _) if !d1.isAfter(d2) => Valid(x)
    case _ => Invalid(Seq(
      (Path \ "startDate") -> Seq(ValidationError("error.expected.supervision.startdate.before.enddate")),
      (Path \ "endDate") -> Seq(ValidationError("error.expected.supervision.enddate.after.startdate"))
    ))
  }

  implicit val formRule: Rule[UrlFormEncoded, AnotherBody] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    (__ \ "anotherBody").read[Boolean].withMessage("error.required.supervision.anotherbody") flatMap {
      case true =>

        val r = (__ \ "supervisorName").read(supervisorRule) ~
          (__ \ "startDate").read(localDateFutureRule) ~
          (__ \ "endDate").read(localDateFutureRule) ~
          (__ \ "endingReason").read(reasonRule)

        r.tupled andThen validationRule andThen Rule.fromMapping[ValidationRuleType, AnotherBody]( x => {
          Valid(AnotherBodyYes(x._1, x._2, x._3, x._4))
        })

      case false => Rule.fromMapping { _ => Valid(AnotherBodyNo) }

    }
  }

  implicit val formWrites: Write[AnotherBody, UrlFormEncoded] = Write {
    case a: AnotherBodyYes =>
      Map(
        "anotherBody" -> Seq("true"),
        "supervisorName" -> Seq(a.supervisorName),
        "endingReason" -> Seq(a.endingReason)
      ) ++ (
        localDateWrite.writes(a.startDate) map {
          case (key, value) =>
            s"startDate.$key" -> value
        })  ++ (
        localDateWrite.writes(a.endDate) map {
          case (key, value) =>
            s"endDate.$key" -> value
        })
    case AnotherBodyNo => Map("anotherBody" -> Seq("false"))
  }

  implicit val jsonReads: Reads[AnotherBody] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._

    (__ \ "anotherBody").read[Boolean] flatMap {
      case true =>
        (
          (__ \ "supervisorName").read[String] ~
            (__ \ "startDate").read[LocalDate] ~
            (__ \ "endDate").read[LocalDate] ~
            (__ \ "endingReason").read[String]) (AnotherBodyYes.apply _) map identity[AnotherBody]

      case false => AnotherBodyNo
    }
  }

  implicit val jsonWrites = Writes[AnotherBody] {
    case a : AnotherBodyYes => Json.obj(
      "anotherBody" -> true,
      "supervisorName" -> a.supervisorName,
      "startDate" -> a.startDate,
      "endDate" -> a.endDate,
      "endingReason" -> a.endingReason
    )
    case AnotherBodyNo => Json.obj("anotherBody" -> false)
  }

}
