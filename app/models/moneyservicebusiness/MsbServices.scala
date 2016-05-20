package models.moneyservicebusiness

import play.api.data.validation.ValidationError
import play.api.libs.json.{Reads, Writes, Json, Format}

sealed trait MsbService


case object TransmittingMoney extends MsbService
case object CurrencyExchange extends MsbService
case object ChequeCashingNotScrapMetal extends MsbService
case object ChequeCashingScrapMetal extends MsbService

case class MsbServices(services : Set[MsbService])

object MsbServices {
  implicit val jsonReads : Reads[MsbServices] = {
    import play.api.libs.json._

    (__ \ "msbServices").read[Set[String]].flatMap[Set[MsbService]] { strs : Set[String] =>
      strs.map {
        case "01" => Reads(_ => JsSuccess(TransmittingMoney)) map identity[MsbService]
        case "02" => Reads(_ => JsSuccess(CurrencyExchange)) map identity[MsbService]
        case "03" => Reads(_ => JsSuccess(ChequeCashingNotScrapMetal)) map identity[MsbService]
        case "04" => Reads(_ => JsSuccess(ChequeCashingScrapMetal)) map identity[MsbService]
        case _ => Reads(_ => JsError(__ \ "msbServices", ValidationError("error.invalid")))
      }.foldLeft[Reads[Set[MsbService]]](Reads[Set[MsbService]](_ => JsSuccess(Set.empty[MsbService]))
      ){
        (result, next) =>
          next flatMap { service:MsbService =>
            result.map { services =>
              services + service
            }
          }
      }
    } map MsbServices.apply
  }

  implicit val jsonWrites : Writes[MsbServices] = Writes { msbServices: MsbServices =>
    Json.obj("msbServices" -> msbServices.services.map {
      case TransmittingMoney => "01"
      case CurrencyExchange => "02"
      case ChequeCashingNotScrapMetal => "03"
      case ChequeCashingScrapMetal => "04"
    })
  }
}

