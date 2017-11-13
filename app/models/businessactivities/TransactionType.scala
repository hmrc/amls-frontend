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
import jto.validation.forms.Rules.{maxLength, notEmpty}
import jto.validation.{From, Path, Rule, ValidationError}
import jto.validation.forms.UrlFormEncoded
import models.FormTypes.{basicPunctuationPattern, notEmptyStrip}
import models.businessactivities.KeepTransactionRecords.softwareNameType
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

object TransactionType {

  import utils.MappingUtils.Implicits._
  import jto.validation.forms.Rules._

  private val maxSoftwareNameLength = 40

  private val softwareNameType =  notEmptyStrip andThen
    notEmpty.withMessage("error.required.ba.software.package.name") andThen
    maxLength(maxSoftwareNameLength).withMessage("error.invalid.maxlength.40") andThen
    basicPunctuationPattern()

  implicit val formRule: Rule[UrlFormEncoded, Set[TransactionType]] = From[UrlFormEncoded] { __ =>
    (__ \ "transactions").read(minLengthR[Set[String]](1).withMessage("error.required.ba.atleast.one.transaction.record")) flatMap { r => r.map {
        case "01" => toSuccessRule(Paper)
        case "02" => toSuccessRule(DigitalSpreadsheet)
        case "03" =>
          (__ \ "name").read(softwareNameType) map DigitalSoftware.apply
        case _ =>
          Rule[UrlFormEncoded, TransactionType] { _ =>
            Invalid(Seq((Path \ "transactions") -> Seq(ValidationError("error.invalid"))))
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
    }
  }
}