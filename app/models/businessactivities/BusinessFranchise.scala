package models.businessactivities

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

sealed trait BusinessFranchise

case class BusinessFranchiseYes(value: String) extends BusinessFranchise

case object BusinessFranchiseNo extends BusinessFranchise

object BusinessFranchise {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._

  val franchiseNameType =  notEmptyStrip compose customNotEmpty("error.required.ba.franchise.name") compose
    customMaxLength(maxFranchiseName, "error.max.length.ba.franchise.name")

  implicit val formRule: Rule[UrlFormEncoded, BusinessFranchise] = From[UrlFormEncoded] { __ =>
  import play.api.data.mapping.forms.Rules._
    (__ \ "businessFranchise").read[Option[Boolean]] flatMap {
      case Some(true) =>
        (__ \ "franchiseName").read(franchiseNameType) fmap BusinessFranchiseYes.apply
      case Some(false) => Rule.fromMapping { _ => Success(BusinessFranchiseNo) }
      case _=> (Path \ "businessFranchise") -> Seq(ValidationError("error.required.ba.is.your.franchise"))
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
      case true => (__ \ "franchiseName").read[String] map BusinessFranchiseYes.apply
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


