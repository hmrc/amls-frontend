package models.businessactivities

import models.FormTypes._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._

sealed trait TransactionRecord

case class TransactionRecordYes(transactionType: Set[TransactionType]) extends TransactionRecord
case object TransactionRecordNo extends TransactionRecord


sealed trait TransactionType

case object Paper extends TransactionType
case object DigitalSpreadsheet extends TransactionType
case class DigitalSoftware(name : String) extends TransactionType

object TransactionType {

  implicit val formRule: Rule[UrlFormEncoded, Set[TransactionType]] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._

      (__ \ "transactions").read[Set[String]] flatMap { z =>
        z.map {
          case "01" => Rule[UrlFormEncoded, TransactionType](_ => Success(Paper))
          case "02" => Rule[UrlFormEncoded, TransactionType](_ => Success(DigitalSpreadsheet))
          case "03" =>
            (__ \ "name").read(softwareNameType) fmap DigitalSoftware.apply
        }.foldLeft[Rule[UrlFormEncoded, Set[TransactionType]]](
          Rule[UrlFormEncoded, Set[TransactionType]](_ => Success(Set.empty))
        ) {
          case (m, n) =>
            n flatMap { x =>
              m fmap {
                _ + x
              }
            }
        }
      }
    }
}

object TransactionRecord {

  implicit def formReads
  (
    implicit p:Path => RuleLike[UrlFormEncoded, Set[TransactionType]]
  ):Rule[UrlFormEncoded, TransactionRecord] =
    From[UrlFormEncoded] { __ =>

      import play.api.data.mapping.forms.Rules._

      (__ \ "isRecorded").read[Boolean].flatMap {
        case true => {
          __.read[Set[TransactionType]] fmap TransactionRecordYes.apply
        }
        case false => Rule.fromMapping { _ => Success(TransactionRecordNo) }
      }
    }
}