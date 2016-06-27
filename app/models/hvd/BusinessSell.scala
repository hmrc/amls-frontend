package models.hvd

import models.FormTypes._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.json.Reads.StringReads
import play.api.data.mapping.forms.Rules.{minLength => _, _}
import utils.TraversableValidators.minLengthR

sealed trait ItemType

case object Alcohol extends ItemType
case object Tobacco extends ItemType
case object Antiques extends ItemType
case object Cars extends ItemType
case object Other motor vehicles extends ItemType
case object Caravans extends ItemType
case object Jewellery extends ItemType
case object Gold extends ItemType
case object ScrapMetals extends ItemType
case object MobilePhones extends ItemType
case object Clothing extends ItemType
case class Other(details:String) extends ItemType

case class BusinessSell(itemTypes: Set[ItemType])

object BusinessSell {

  import utils.MappingUtils.Implicits._

  val maxSoftwareNameLength = 40
  val softwareNameType =  notEmptyStrip compose
                          notEmpty.withMessage("error.required.ba.software.package.name") compose
                          maxLength(maxSoftwareNameLength).withMessage("error.max.length.ba.software.package.name")

  implicit val formRule: Rule[UrlFormEncoded, BusinessSell] =
    From[UrlFormEncoded] { __ =>
      (__ \ "isRecorded").read[Boolean].withMessage("error.required.ba.select.transaction.record") flatMap {
        case true =>
          (__ \ "transactions").read(minLengthR[Set[String]](1).withMessage("error.required.ba.atleast.one.transaction.record")) flatMap { z =>
            z.map {
              case "01" => Rule[UrlFormEncoded, BusinessSell](_ => Success(Paper))
              case "02" => Rule[UrlFormEncoded, BusinessSell](_ => Success(DigitalSpreadsheet))
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

  implicit def formWrites = Write[TransactionRecord, UrlFormEncoded] {
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



