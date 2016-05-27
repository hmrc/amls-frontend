package models.moneyservicebusiness

import play.api.data.mapping._
import play.api.data.validation.ValidationError
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

  implicit def formR[A]
  (implicit
    p: Path => RuleLike[A, Set[MsbService]]
  ): Rule[A, MsbServices] =
    From[A] { __ =>

      import utils.MappingUtils.Implicits._

      val required =
        TraversableValidators.minLength[Set[MsbService]](1) withMessage "error.required.msb.services"

      (__ \ "msbServices").read(required) fmap MsbServices.apply
    }

  implicit def formW[A]
  (implicit
    p: Path => WriteLike[Set[MsbService], A]
  ): Write[MsbServices, A] =
    To[A] { __ =>

      import play.api.libs.functional.syntax.unlift

      (__ \ "msbServices").write[Set[MsbService]] contramap unlift(MsbServices.unapply)
    }
}

