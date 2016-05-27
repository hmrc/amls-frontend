package models.moneyservicebusiness

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._
import utils.TraversableValidators

sealed trait MsbService


case object TransmittingMoney extends MsbService
case object CurrencyExchange extends MsbService
case object ChequeCashingNotScrapMetal extends MsbService
case object ChequeCashingScrapMetal extends MsbService

case class MsbServices(services : Set[MsbService])

object MsbService {

  implicit val serviceFormR = Rule[String, MsbService] {
    case "01" => Success(TransmittingMoney)
    case "02" => Success(CurrencyExchange)
    case "03" => Success(ChequeCashingNotScrapMetal)
    case "04" => Success(ChequeCashingScrapMetal)
    case "05" => Failure(Seq(Path -> Seq(ValidationError("error.invalid"))))
  }

  implicit val serviceFormW = Write[MsbService, String] {
    case TransmittingMoney => "01"
    case CurrencyExchange => "02"
    case ChequeCashingNotScrapMetal => "03"
    case ChequeCashingScrapMetal => "04"
  }
}

object MsbServices {

  import play.api.data.mapping.forms.Rules._
  import play.api.data.mapping.{PathNode, KeyPathNode, IdxPathNode}
  import play.api.libs.json

  implicit def nodeToJsNode(n: PathNode): json.PathNode = {
    n match {
      case KeyPathNode(key) =>
        json.KeyPathNode(key)
      case IdxPathNode(idx) =>
        json.IdxPathNode(idx)
    }
  }

  private def pathToJsPath(p: Path): JsPath =
    JsPath(p.path.map(nodeToJsNode _))

  implicit def errorConversion(errs: Seq[(Path, Seq[ValidationError])]): Seq[(JsPath, Seq[ValidationError])] =
    errs map {
      case (path, errors) =>
        (pathToJsPath(path), errors)
    }

  implicit def jsonR[A]
  (implicit
    rule: Rule[JsValue, A]
  ): Reads[A] =
    Reads {
      json =>
        rule.validate(json) match {
          case Success(a) =>
            JsSuccess(a)
          case Failure(errors) =>
            JsError(errors)
        }
    }

  implicit def jsonW[A]
  (implicit
    write: Write[A, JsValue]
  ): Writes[A] =
    Writes {
      a =>
        write.writes(a)
    }

  implicit def formR[A]: Rule[A, MsbServices] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits._

      val required =
        TraversableValidators.minLength[Set[MsbService]](1) withMessage "error.required.msb.services"

      (__ \ "msbServices").read(required) fmap MsbServices.apply
    }

  implicit def formW[A]: Write[MsbServices, A] =
    To[A] { __ =>
      import play.api.libs.functional.syntax.unlift
      (__ \ "msbServices").write[Set[MsbService]] contramap unlift(MsbServices.unapply)
    }

//  implicit val formW = Write[MsbServices, UrlFormEncoded] {
//    case MsbServices(services) =>
//      Map(
//        "msbServices[]" -> services.toSeq.map {
//          case TransmittingMoney => "01"
//          case CurrencyExchange => "02"
//          case ChequeCashingNotScrapMetal => "03"
//          case ChequeCashingScrapMetal => "04"
//        }
//      )
//  }

//  implicit val jsonReads : Reads[MsbServices] = {
//    import play.api.libs.json._
//
//    (__ \ "msbServices").read[Set[String]].flatMap[Set[MsbService]] { strs : Set[String] =>
//      strs.map {
//        case "01" => Reads(_ => JsSuccess(TransmittingMoney)) map identity[MsbService]
//        case "02" => Reads(_ => JsSuccess(CurrencyExchange)) map identity[MsbService]
//        case "03" => Reads(_ => JsSuccess(ChequeCashingNotScrapMetal)) map identity[MsbService]
//        case "04" => Reads(_ => JsSuccess(ChequeCashingScrapMetal)) map identity[MsbService]
//        case _ => Reads(_ => JsError(__ \ "msbServices", ValidationError("error.invalid")))
//      }.foldLeft[Reads[Set[MsbService]]](Reads[Set[MsbService]](_ => JsSuccess(Set.empty[MsbService]))
//      ){
//        (result, next) =>
//          next flatMap { service:MsbService =>
//            result.map { services =>
//              services + service
//            }
//          }
//      }
//    } map MsbServices.apply
//  }

//  implicit val jsonWrites : Writes[MsbServices] = Writes { msbServices: MsbServices =>
//    Json.obj("msbServices" -> msbServices.services.map {
//      case TransmittingMoney => "01"
//      case CurrencyExchange => "02"
//      case ChequeCashingNotScrapMetal => "03"
//      case ChequeCashingScrapMetal => "04"
//    })
//  }
}

