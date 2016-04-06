package models.responsiblepeople

import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import play.api.libs.json._

sealed trait TimeAtAddress
case object ZeroToFiveMonths extends TimeAtAddress
case object SixToElevenMonths extends TimeAtAddress
case object OneToThreeYears extends TimeAtAddress
case object ThreeYearsPlus extends TimeAtAddress

object TimeAtAddress {

  implicit val timeAtAddressFormRead = Rule[String, TimeAtAddress] {
    case "01" => Success(ZeroToFiveMonths)
    case "02" => Success(SixToElevenMonths)
    case "03" => Success(OneToThreeYears)
    case "04" => Success(ThreeYearsPlus)
    case _ =>  Failure(Seq(Path -> Seq(ValidationError(Messages("error.required.timeAtAddress")))))
  }

  implicit val timeAtAddressFormWrite = Write[TimeAtAddress, Seq[String]] {
    case ZeroToFiveMonths => Seq("01")
    case SixToElevenMonths => Seq("02")
    case OneToThreeYears => Seq("03")
    case ThreeYearsPlus => Seq("04")
  }

  implicit val jsonReads: Reads[TimeAtAddress] =
    Reads {
      case JsString("01") => JsSuccess(ZeroToFiveMonths)
      case JsString("02") => JsSuccess(SixToElevenMonths)
      case JsString("03") => JsSuccess(OneToThreeYears)
      case JsString("04") => JsSuccess(ThreeYearsPlus)
      case _ => JsError((JsPath \ "timeAtAddress") -> ValidationError("error.required.timeAtAddress"))
    }

  implicit val jsonWrites =
    Writes[TimeAtAddress] {
      case ZeroToFiveMonths => JsString("01")
      case SixToElevenMonths => JsString("02")
      case OneToThreeYears => JsString("03")
      case ThreeYearsPlus => JsString("04")
    }
}