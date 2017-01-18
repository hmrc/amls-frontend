package models.tradingpremises

import models.DateOfChange
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import play.api.libs.json._
import utils.TraversableValidators

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

case class MsbServices(services : Set[MsbService], dateOfChange: Option[DateOfChange] = None)

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

  def applyWithoutDateOfChange(services: Set[MsbService]) = MsbServices(services)

  def unapplyWithoutDateOfChange(s: MsbServices) = Some(s.services)

}

sealed trait MsbServices0 {

  private implicit def rule[A]
  (implicit
   p: Path => RuleLike[A, Set[MsbService]]
  ): Rule[A, MsbServices] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits.RichRule

      val required =
        TraversableValidators.minLengthR[Set[MsbService]](1) withMessage "error.required.msb.services"

      (__ \ "msbServices").read(required) fmap MsbService.applyWithoutDateOfChange
    }

  private implicit def write[A]
  (implicit
   p: Path => WriteLike[Set[MsbService], A]
  ): Write[MsbServices, A] =
    To[A] { __ =>

      import play.api.libs.functional.syntax.unlift

      (__ \ "msbServices").write[Set[MsbService]] contramap unlift(MsbService.unapplyWithoutDateOfChange)
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

  def addDateOfChange(doc: Option[DateOfChange], obj: JsObject) =
    doc.fold(obj) { dateOfChange => obj + ("dateOfChange" -> DateOfChange.writes.writes(dateOfChange))}

  implicit val jsonWrites = new Writes[MsbServices] {
    def writes(s: MsbServices): JsValue = {
      val values = s.services map { x => JsString(MsbService.serviceW.writes(x)) }

      addDateOfChange(s.dateOfChange, Json.obj(
        "msbServices" -> values
      ))
    }
  }

  implicit val msbServiceReader: Reads[Set[MsbService]] = {
    __.read[JsArray].map(a => a.value.map(MsbService.jsonR.validate(_).get).toSet)
  }

  implicit val jReads: Reads[MsbServices] = {
    import play.api.libs.functional.syntax._
    ((__ \ "msbServices").read[Set[MsbService]] and
      (__ \ "dateOfChange").readNullable[DateOfChange])(MsbServices.apply _)
  }

  implicit val formR: Rule[UrlFormEncoded, MsbServices] = Cache.formR
  implicit val formW: Write[MsbServices, UrlFormEncoded] = Cache.formW
}

