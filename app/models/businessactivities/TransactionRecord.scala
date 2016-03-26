package models.businessactivities

import models.FormTypes._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.json.Reads.StringReads
import play.api.data.mapping.forms.Rules.{minLength => _, _}
import utils.TraversableValidators.minLength

sealed trait TransactionRecord

case class TransactionRecordYes(transactionType: Set[TransactionType]) extends TransactionRecord

case object TransactionRecordNo extends TransactionRecord


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

object TransactionRecord {

  import utils.MappingUtils.Implicits._

  val maxSoftwareNameLength = 40
  val softwareNameType =  notEmptyStrip compose
                          customNotEmpty("error.required.ba.software.package.name") compose
                          customMaxLength(maxSoftwareNameLength, "error.max.length.ba.software.package.name")

  implicit val formRule: Rule[UrlFormEncoded, TransactionRecord] =
    From[UrlFormEncoded] { __ =>
      (__ \ "isRecorded").read[Option[Boolean]].flatMap {
        case Some(true) =>
          (__ \ "transactions").read(minLength[Set[String]]("error.required.ba.atleast.one.transaction.record")) flatMap { z =>
            z.map {
              case "01" => Rule[UrlFormEncoded, TransactionType](_ => Success(Paper))
              case "02" => Rule[UrlFormEncoded, TransactionType](_ => Success(DigitalSpreadsheet))
              case "03" =>
                (__ \ "name").read(softwareNameType) fmap DigitalSoftware.apply
              case _ =>
                Rule[UrlFormEncoded, TransactionType] { _ =>
                  Failure(Seq((Path \ "transactions") -> Seq(ValidationError("error.invalid"))))
                }
            }.foldLeft[Rule[UrlFormEncoded, Set[TransactionType]]](
              Rule[UrlFormEncoded, Set[TransactionType]](_ => Success(Set.empty))
            ) {
              case (m, n) =>
                  n flatMap { x =>
                    m fmap {
                      _ + x
                    }
                  }
            } fmap TransactionRecordYes.apply
          }

        case Some(false) => Rule.fromMapping { _ => Success(TransactionRecordNo) }
        case _ => Path \ "isRecorded" -> Seq(ValidationError("error.required.ba.select.transaction.record"))
      }
    }

  implicit def formWrites = Write[TransactionRecord, UrlFormEncoded] {
    case TransactionRecordNo => Map("isRecorded" -> "false")
    case TransactionRecordYes(transactions) =>
      Map(
        "isRecorded" -> Seq("true"),
        "transactions" -> (transactions map { _.value }).toSeq
      ) ++ transactions.foldLeft[UrlFormEncoded](Map.empty) {
        case (m, DigitalSoftware(name)) =>
          m ++ Map("name" -> Seq(name))
        case (m, _) =>
          m
      }
  }

  implicit val jsonReads: Reads[TransactionRecord] =
    (__ \ "isRecorded").read[Boolean] flatMap {
      case true => (__ \ "transactions").read[Set[String]].flatMap {x:Set[String] =>
        x.map {
            case "01" => Reads(_ => JsSuccess(Paper)) map identity[TransactionType]
            case "02" => Reads(_ => JsSuccess(DigitalSpreadsheet)) map identity[TransactionType]
            case "03" =>
              (JsPath \ "digitalSoftwareName").read[String].map (DigitalSoftware.apply  _) map identity[TransactionType]
            case _ =>
              Reads(_ => JsError((JsPath \ "transactions") -> ValidationError("error.invalid")))
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

  implicit val jsonWrite = Writes[TransactionRecord] {
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

