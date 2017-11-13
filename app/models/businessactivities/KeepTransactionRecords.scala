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

import models.FormTypes._
import jto.validation.forms.UrlFormEncoded
import jto.validation._
import jto.validation.ValidationError
import play.api.libs.json._
import play.api.libs.json.Reads.StringReads
import jto.validation.forms.Rules.{minLength => _, _}
import utils.TraversableValidators.minLengthR
import cats.data.Validated.{Invalid, Valid}

sealed trait KeepTransactionRecords

case class TransactionRecordYes(transactionType: Set[TransactionType]) extends KeepTransactionRecords

case object TransactionRecordNo extends KeepTransactionRecords


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

object KeepTransactionRecords {

  import utils.MappingUtils.Implicits._

  val maxSoftwareNameLength = 40
  val softwareNameType =  notEmptyStrip andThen
                          notEmpty.withMessage("error.required.ba.software.package.name") andThen
                          maxLength(maxSoftwareNameLength).withMessage("error.invalid.maxlength.40") andThen
                          basicPunctuationPattern()

  implicit val formRule: Rule[UrlFormEncoded, KeepTransactionRecords] =
    From[UrlFormEncoded] { __ =>
      (__ \ "isRecorded").read[Boolean].withMessage("error.required.ba.select.transaction.record") flatMap {
        case true =>
          (__ \ "transactions").read(minLengthR[Set[String]](1).withMessage("error.required.ba.atleast.one.transaction.record")) flatMap { z =>
            z.map {
              case "01" => Rule[UrlFormEncoded, TransactionType](_ => Valid(Paper))
              case "02" => Rule[UrlFormEncoded, TransactionType](_ => Valid(DigitalSpreadsheet))
              case "03" =>
                (__ \ "name").read(softwareNameType) map DigitalSoftware.apply
              case _ =>
                Rule[UrlFormEncoded, TransactionType] { _ =>
                  Invalid(Seq((Path \ "transactions") -> Seq(ValidationError("error.invalid"))))
                }
            }.foldLeft[Rule[UrlFormEncoded, Set[TransactionType]]](
              Rule[UrlFormEncoded, Set[TransactionType]](_ => Valid(Set.empty))
            ) {
              case (m, n) =>
                  n flatMap { x =>
                    m map {
                      _ + x
                    }
                  }
            } map TransactionRecordYes.apply
          }

        case false => Rule.fromMapping { _ => Valid(TransactionRecordNo) }
      }
    }

  implicit def formWrites = Write[KeepTransactionRecords, UrlFormEncoded] {
    case TransactionRecordNo => Map("isRecorded" -> "false")
    case TransactionRecordYes(transactions) =>
      Map(
        "isRecorded" -> Seq("true"),
        "transactions[]" -> (transactions map { _.value }).toSeq
      ) ++ transactions.foldLeft[UrlFormEncoded](Map.empty) {
        case (m, DigitalSoftware(name)) =>
          m ++ Map("name" -> Seq(name))
        case (m, _) =>
          m
      }
  }

  implicit val jsonReads: Reads[KeepTransactionRecords] =
    (__ \ "isRecorded").read[Boolean] flatMap {
      case true => (__ \ "transactions").read[Set[String]].flatMap {x:Set[String] =>
        x.map {
            case "01" => Reads(_ => JsSuccess(Paper)) map identity[TransactionType]
            case "02" => Reads(_ => JsSuccess(DigitalSpreadsheet)) map identity[TransactionType]
            case "03" =>
              (JsPath \ "digitalSoftwareName").read[String].map (DigitalSoftware.apply  _) map identity[TransactionType]
            case _ =>
              Reads(_ => JsError((JsPath \ "transactions") -> play.api.data.validation.ValidationError("error.invalid")))
          }.foldLeft[Reads[Set[TransactionType]]](
            Reads[Set[TransactionType]](_ => JsSuccess(Set.empty))
         ){
          (result, data) =>
            data flatMap {m =>
             result.map {n =>
               n + m
             }
           }
        }
      } map TransactionRecordYes.apply
      case false => Reads(_ => JsSuccess(TransactionRecordNo))
    }

  implicit val jsonWrite = Writes[KeepTransactionRecords] {
    case TransactionRecordNo => Json.obj("isRecorded" -> false)
    case TransactionRecordYes(transactions) =>
      Json.obj(
        "isRecorded" -> true,
        "transactions" -> (transactions map {
          _.value
        }).toSeq
      ) ++ transactions.foldLeft[JsObject](Json.obj()) {
        case (m, DigitalSoftware(name)) =>
          m ++ Json.obj("digitalSoftwareName" -> name)
        case (m, _) =>
          m
      }
  }
}

