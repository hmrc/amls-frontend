/*
 * Copyright 2019 HM Revenue & Customs
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
                          startDate: Option[LocalDate],
                          endDate: Option[LocalDate],
                          endingReason: Option[String]) extends AnotherBody

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

  type ValidationRuleType = (String, Option[LocalDate], Option[LocalDate], Option[String])

  val validationRule: ValidationRule[ValidationRuleType] = Rule[ValidationRuleType, ValidationRuleType] {
    case x@(_, d1, d2, _) if d1.isDefined && d2.isDefined && !d1.get.isAfter(d2.get) => Valid(x)
    case x@(_, d1, d2, _) if d1.isEmpty & d2.isEmpty => Valid(x)
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
          (__ \ "startDate").read(optionR(localDateFutureRule)) ~
          (__ \ "endDate").read(optionR(localDateFutureRule)) ~
          (__ \ "endingReason").read(optionR(reasonRule))

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
        "endingReason" -> Seq(a.endingReason.getOrElse(""))
      ) ++ (
        localDateWrite.writes(a.startDate.getOrElse(LocalDate.now())) map {
          case (key, value) =>
            s"startDate.$key" -> value
        })  ++ (
        localDateWrite.writes(a.endDate.getOrElse(LocalDate.now())) map {
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
            (__ \ "startDate").readNullable[LocalDate] ~
            (__ \ "endDate").readNullable[LocalDate] ~
            (__ \ "endingReason").readNullable[String]) (AnotherBodyYes.apply _) map identity[AnotherBody]

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
