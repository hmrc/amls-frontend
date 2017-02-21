package models.businessmatching

import models.FormTypes._
import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import cats.data.Validated.{Invalid, Valid}

sealed trait BusinessAppliedForPSRNumber
case class BusinessAppliedForPSRNumberYes(regNumber: String) extends BusinessAppliedForPSRNumber
case object BusinessAppliedForPSRNumberNo extends BusinessAppliedForPSRNumber

object BusinessAppliedForPSRNumber {

  import utils.MappingUtils.Implicits._

  private val registrationNumberType = notEmptyStrip
      .andThen(extendedReferenceNumberRule("error.invalid.msb.psr.number"))

  implicit val formRule: Rule[UrlFormEncoded, BusinessAppliedForPSRNumber] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "appliedFor").read[Boolean].withMessage("error.required.msb.psr.options") flatMap {
      case true =>
         (__ \ "regNumber").read(registrationNumberType) map BusinessAppliedForPSRNumberYes.apply
      case false => Rule.fromMapping { _ => Valid(BusinessAppliedForPSRNumberNo) }
    }
  }

  implicit val formWrites: Write[BusinessAppliedForPSRNumber, UrlFormEncoded] = Write {
    case BusinessAppliedForPSRNumberYes(regNum) => Map("appliedFor" -> Seq("true"),
                                                       "regNumber" -> Seq(regNum))
    case BusinessAppliedForPSRNumberNo => Map("appliedFor" -> Seq("false"))

  }

  implicit val jsonReads: Reads[BusinessAppliedForPSRNumber] =
    (__ \ "appliedFor").read[Boolean] flatMap {
      case true => (__ \ "regNumber").read[String] map BusinessAppliedForPSRNumberYes.apply
      case false => Reads(_ => JsSuccess(BusinessAppliedForPSRNumberNo))
  }

  implicit val jsonWrites = Writes[BusinessAppliedForPSRNumber] {
    case BusinessAppliedForPSRNumberYes(value) => Json.obj(
      "appliedFor" -> true,
      "regNumber" -> value
    )
    case BusinessAppliedForPSRNumberNo => Json.obj("appliedFor" -> false)
  }

}

