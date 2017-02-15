package models.businessactivities

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json._

sealed trait BusinessFranchise

case class BusinessFranchiseYes(value: String) extends BusinessFranchise

case object BusinessFranchiseNo extends BusinessFranchise

object BusinessFranchise {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._

  private val maxFranchiseName = 140
  private val regexPattern = "^[0-9a-zA-Z_]+$".r
  private val franchiseNameType =  notEmptyStrip andThen notEmpty.withMessage("error.required.ba.franchise.name") andThen
    pattern(regexPattern).withMessage("err.text.validation") andThen
    maxLength(maxFranchiseName).withMessage("error.max.length.ba.franchise.name")

  implicit val formRule: Rule[UrlFormEncoded, BusinessFranchise] = From[UrlFormEncoded] { __ =>
  import jto.validation.forms.Rules._
    (__ \ "businessFranchise").read[Boolean].withMessage("error.required.ba.is.your.franchise") flatMap {
      case true =>
        (__ \ "franchiseName").read(franchiseNameType) map BusinessFranchiseYes.apply
      case false => Rule.fromMapping { _ => Valid(BusinessFranchiseNo) }
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

