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

package models.businessactivities

import cats.data.Validated.{Invalid, Valid}
import play.api.data.validation.{ValidationError => VE}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Path, Rule, To, ValidationError, Write}
import models.FormTypes.{basicPunctuationPattern, notEmptyStrip}
import play.api.libs.json._
import utils.TraversableValidators.minLengthR

sealed trait TransactionType {
  val value: String =
    this match {
      case Paper => "01"
      case DigitalSpreadsheet => "02"
      case DigitalSoftware(_) => "03"
    }
}

case object Paper extends TransactionType

case object DigitalSpreadsheet extends TransactionType

case class DigitalSoftware(name: String) extends TransactionType

case class TransactionTypes(types: Set[TransactionType])

object TransactionTypes {

  import jto.validation.forms.Rules._
  import utils.MappingUtils.Implicits._

  implicit val jsonReads = new Reads[TransactionTypes] {
    override def reads(json: JsValue) = {
      val t = (json \ "types").asOpt[Set[String]]
      val n = (json \ "software").asOpt[String]
      val validValues = Set("01", "02", "03")

      (t, n) match {
        case (None, _) => JsError(__ \ "types" -> VE("error.missing"))
        case (Some(types), None) if types.contains("03") => JsError(__ \ "software" -> VE("error.missing"))
        case (Some(types), _) if types.diff(validValues).nonEmpty => JsError(__ \ "types" -> VE("error.invalid"))
        case (Some(types), maybeName) => JsSuccess(TransactionTypes(types map {
          case "01" => Paper
          case "02" => DigitalSpreadsheet
          case "03" => DigitalSoftware(maybeName.getOrElse(""))
        }))
      }
    }
  }

  implicit val jsonWrites = Writes[TransactionTypes] { t =>
    Json.obj(
      "types" -> t.types.map(_.value)
    ) ++ (t.types.collectFirst {
      case DigitalSoftware(name) => Json.obj("name" -> name)
    } getOrElse Json.obj())
  }

  private val maxSoftwareNameLength = 40

  private val softwareNameType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.ba.software.package.name") andThen
    maxLength(maxSoftwareNameLength).withMessage("error.invalid.maxlength.40") andThen
    basicPunctuationPattern()

  implicit val formRule: Rule[UrlFormEncoded, TransactionTypes] = From[UrlFormEncoded] { __ =>
    (__ \ "types").read(minLengthR[Set[String]](1).withMessage("error.required.ba.atleast.one.transaction.record")) flatMap { r =>
      r.map {
        case "01" => toSuccessRule(Paper)
        case "02" => toSuccessRule(DigitalSpreadsheet)
        case "03" =>
          (__ \ "name").read(softwareNameType) map DigitalSoftware.apply
        case _ =>
          Rule[UrlFormEncoded, TransactionType] { _ =>
            Invalid(Seq((Path \ "types") -> Seq(ValidationError("error.invalid"))))
          }
      }.foldLeft[Rule[UrlFormEncoded, Set[TransactionType]]](
        Set.empty[TransactionType]
      ) {
        case (m, n) =>
          n flatMap { x =>
            m map {
              _ + x
            }
          }
      }
    } map TransactionTypes.apply
  }

  implicit val formWriter = Write[TransactionTypes, UrlFormEncoded] { t =>
    Map(
      "types[]" -> t.types.map(_.value).toSeq
    ) ++ t.types.collectFirst {
      case DigitalSoftware(name) => name
    }.fold(Map[String, Seq[String]]())(n => Map("name" -> Seq(n)))
  }

}