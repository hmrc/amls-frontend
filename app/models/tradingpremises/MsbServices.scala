package models.tradingpremises

import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import play.api.libs.json._
import utils.{JsonMapping, TraversableValidators}

sealed trait MsbService {

  val getMessage = this match {
    case TransmittingMoney => Messages("msb.services.list.lbl.01")
    case CurrencyExchange => Messages("msb.services.list.lbl.02")
    case ChequeCashingNotScrapMetal => Messages("msb.services.list.lbl.03")
    case ChequeCashingScrapMetal => Messages("msb.services.list.lbl.04")
  }
}

case object TransmittingMoney extends MsbService
case object CurrencyExchange extends MsbService
case object ChequeCashingNotScrapMetal extends MsbService
case object ChequeCashingScrapMetal extends MsbService

case class MsbServices(services : Set[MsbService])

object MsbService {

  implicit val serviceR = Rule[String, MsbService] {
    case "01" => Success(TransmittingMoney)
    case "02" => Success(CurrencyExchange)
    case "03" => Success(ChequeCashingNotScrapMetal)
    case "04" => Success(ChequeCashingScrapMetal)
    case _ => Failure(Seq(Path -> Seq(ValidationError("error.invalid"))))
  }

  implicit val serviceW = Write[MsbService, String] {
    case TransmittingMoney => "01"
    case CurrencyExchange => "02"
    case ChequeCashingNotScrapMetal => "03"
    case ChequeCashingScrapMetal => "04"
  }

  // TODO: Create generic rules that will remove the need for this
  implicit val jsonR: Rule[JsValue, MsbService] = {
    import play.api.data.mapping.json.Rules._
    stringR compose serviceR
  }

  // TODO: Create generic writes that will remove the need for this
  implicit val jsonW: Write[MsbService, JsValue] = {
    import play.api.data.mapping.json.Writes._
    serviceW compose string
  }
}

sealed trait MsbServices0 {

  import JsonMapping._

  private implicit def rule[A]
  (implicit
   p: Path => RuleLike[A, Set[MsbService]]
  ): Rule[A, MsbServices] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits.RichRule

      val required =
        TraversableValidators.minLengthR[Set[MsbService]](1) withMessage "error.required.msb.services"

      (__ \ "msbServices").read(required) fmap MsbServices.apply
    }

  private implicit def write[A]
  (implicit
   p: Path => WriteLike[Set[MsbService], A]
  ): Write[MsbServices, A] =
    To[A] { __ =>

      import play.api.libs.functional.syntax.unlift

      (__ \ "msbServices").write[Set[MsbService]] contramap unlift(MsbServices.unapply)
    }

  val jsonR: Reads[MsbServices] = {
    import play.api.data.mapping.json.Rules.{JsValue => _, pickInJson => _, _}
    implicitly[Reads[MsbServices]]
  }

  val jsonW: Writes[MsbServices] = {
    import play.api.data.mapping.json.Writes._
    implicitly[Writes[MsbServices]]
  }

  val formR: Rule[UrlFormEncoded, MsbServices] = {
    import play.api.data.mapping.forms.Rules._
    implicitly[Rule[UrlFormEncoded, MsbServices]]
  }

  val formW: Write[MsbServices, UrlFormEncoded] = {
    import play.api.data.mapping.forms.Writes._
    import utils.MappingUtils.writeM
    implicitly[Write[MsbServices, UrlFormEncoded]]
  }
}

object MsbServices {

  private object Cache extends MsbServices0

  implicit val jsonR: Reads[MsbServices] = Cache.jsonR
  implicit val jsonW: Writes[MsbServices] = Cache.jsonW
  implicit val formR: Rule[UrlFormEncoded, MsbServices] = Cache.formR
  implicit val formW: Write[MsbServices, UrlFormEncoded] = Cache.formW
}

