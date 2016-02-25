package models.businessactivities

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._

sealed trait BusinessFranchise

case class BusinessFranchiseYes(value: String) extends BusinessFranchise

case object BusinessFranchiseNo extends BusinessFranchise

object BusinessFranchise {

  import models.FormTypes._

  implicit val formRule: Rule[UrlFormEncoded, BusinessFranchise] = From[UrlFormEncoded] { __ =>
  import play.api.data.mapping.forms.Rules._
    (__ \ "businessFranchise").read[Boolean] flatMap {
      case true =>
        (__ \ "franchiseName").read(descriptionType) fmap (BusinessFranchiseYes.apply)
      case false => Rule.fromMapping { _ => Success(BusinessFranchiseNo) }
    }
  }

  implicit val formWrites: Write[BusinessFranchise, UrlFormEncoded] = Write {
    case BusinessFranchiseYes(value) =>
      Map("businessFranchise" -> Seq("true"),
        "franchiseName" -> Seq(value)
      )
    case BusinessFranchiseNo => Map("businessFranchise" -> Seq("false"))
  }

  implicit val jsonReads: Reads[BusinessFranchise] =
    (__ \ "businessFranchise").read[Boolean] flatMap {
      case true => (__ \ "franchiseName").read[String] map (BusinessFranchiseYes.apply _)
      case false => Reads(_ => JsSuccess(BusinessFranchiseNo))
    }

  implicit val jsonWrites = Writes[BusinessFranchise] {
    case BusinessFranchiseYes(value) => Json.obj(
      "businessFranchise" -> true,
      "franchiseName" -> value
    )
    case BusinessFranchiseNo => Json.obj("businessFranchise" -> false)
  }

}


