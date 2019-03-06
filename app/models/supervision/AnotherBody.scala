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

import cats.data.Validated.{Invalid, Valid}
import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms._
import models.FormTypes._
import models.ValidationRule
import org.joda.time.LocalDate
import play.api.libs.json.{Json, Reads, Writes}



sealed trait AnotherBody

case class AnotherBodyYes(supervisorName: String,
                          startDate: Option[SupervisionStart],
                          endDate: Option[SupervisionEnd],
                          endingReason: Option[SupervisionEndReasons]) extends AnotherBody {

  def supervisorName(p: String): AnotherBodyYes =
    this.copy(supervisorName = p)

  def startDate(p: SupervisionStart): AnotherBodyYes =
    this.copy(startDate = Some(p))

  def endDate(p: SupervisionEnd): AnotherBodyYes =
    this.copy(endDate = Some(p))

  def endingReason(p: SupervisionEndReasons): AnotherBodyYes =
    this.copy(endingReason = Some(p))

  def isComplete(): Boolean = this match {
    case AnotherBodyYes(name, Some(_), Some(_), Some(_)) => true
    case _ => false

  }
}

case object AnotherBodyNo extends AnotherBody

object AnotherBody {

  import utils.MappingUtils.Implicits._

  private val supervisorMaxLength = 140

  private val supervisorRule = notEmptyStrip andThen
    notEmpty.withMessage("error.required.supervision.supervisor") andThen
    maxLength(supervisorMaxLength).withMessage("error.invalid.supervision.supervisor") andThen
    basicPunctuationPattern()

  type ValidationRuleType = (Option[String], Option[LocalDate], Option[LocalDate], Option[String])

  val validationRule: ValidationRule[ValidationRuleType] = Rule[ValidationRuleType, ValidationRuleType] {
    case x@(_, d1, d2, _) if d1.isDefined & d2.isDefined && !d1.get.isAfter(d2.get) => Valid(x)
    case x@(_, d1, d2, _) => Valid(x)
    case _ => Invalid(Seq(
      (Path \ "startDate") -> Seq(ValidationError("error.expected.supervision.startdate.before.enddate")),
      (Path \ "endDate") -> Seq(ValidationError("error.expected.supervision.enddate.after.startdate"))
    ))
  }

  implicit val formRule: Rule[UrlFormEncoded, AnotherBody] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    (__ \ "anotherBody").read[Boolean] flatMap {
      case true =>
        ((__ \ "supervisorName").read(supervisorRule) ~
          (__ \ "startDate").read[Option[SupervisionStart]] ~
          (__ \ "endDate").read[Option[SupervisionEnd]] ~
          (__ \ "endingReason").read[Option[SupervisionEndReasons]]).apply(AnotherBodyYes.apply _)

      case false => Rule.fromMapping { _ => Valid(AnotherBodyNo) }
    }
  }

  implicit val formWrites: Write[AnotherBody, UrlFormEncoded] = Write {
    case a: AnotherBodyYes =>
      Map(
        "anotherBody" -> Seq("true"),
        "supervisorName" -> Seq(a.supervisorName)
      )
    case AnotherBodyNo => Map("anotherBody" -> Seq("false"))
  }

  implicit val jsonReads: Reads[AnotherBody] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._

    (__ \ "anotherBody").read[Boolean] flatMap {
      case true =>
        (
          (__ \ "supervisorName").read[String] and
            (__ \ "startDate").readNullable[SupervisionStart] and
            (__ \ "endDate").read(Reads.optionNoError[SupervisionEnd]) and
            (__ \ "endingReason").read(Reads.optionNoError[SupervisionEndReasons])
          ) (AnotherBodyYes.apply _) map identity[AnotherBody]

      case false => AnotherBodyNo
    }
  }

  implicit val jsonWrites = Writes[AnotherBody] {
    case a : AnotherBodyYes => {
     Json.obj("anotherBody" -> true,
        "supervisorName" -> a.supervisorName,
        "startDate" -> a.startDate,
        "endDate" -> a.endDate,
        "endingReason" -> a.endingReason)
    }
    case AnotherBodyNo => Json.obj("anotherBody" -> false)
  }

}