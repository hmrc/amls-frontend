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

  implicit val formRule: Rule[UrlFormEncoded, TransactionRecord] =
    From[UrlFormEncoded] { __ =>
      (__ \ "isRecorded").read[Boolean].flatMap {
        case true =>
          (__ \ "transactions").read(minLength[Set[String]](1)) flatMap { z =>
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

        case false => Rule.fromMapping { _ => Success(TransactionRecordNo) }
      }
    }

  def getTupleFromModel(value: Set[TransactionType]) : (Seq[String], String) = {
    val data = value.toSeq.map {
      case Paper => ("01", "")
      case DigitalSpreadsheet => ("02", "")
      case DigitalSoftware(name) => ("03", name)

    }.foldLeft[(Seq[String], String)](
      (Seq.empty, "")
    ) (
      (result, txValue) =>
        (result._1 :+ txValue._1, result._2.concat(txValue._2))
    )
    data
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
            case "01" => Reads(_ => JsSuccess(Paper))
            case "02" => Reads(_ => JsSuccess(DigitalSpreadsheet))
            case "03" =>
              (JsPath \ "name").read[String].map (DigitalSoftware.apply  _)
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
    case TransactionRecordYes(name) =>
      val data = getTupleFromModel(name)
        data._2 match {
          case "" =>  Json.obj("isRecorded" -> true,
            "transactions" -> data._1)

          case _ =>  Json.obj("isRecorded" -> true,
            "transactions" -> data._1,
            "name" -> data._2)
        }
  }
}

