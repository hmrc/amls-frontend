package models.tradingpremises

import org.joda.time.LocalDate
import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, To, Write, _}
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes._
import play.api.libs.json.{Writes, _}

case class YourTradingPremises(tradingName: String,
                               tradingPremisesAddress: TradingPremisesAddress,
                               premiseOwner: PremiseOwner,
                               startOfTradingDate: LocalDate,
                               isResidential: IsResidential)

object YourTradingPremises {

  implicit val jsonReadsYourTradingPremises = {
    ((JsPath \ "tradingName").read[String] and
      JsPath.read[TradingPremisesAddress] and
      JsPath.read[PremiseOwner] and
      JsPath.read[LocalDate] and
      JsPath.read[IsResidential]) (YourTradingPremises.apply _)
  }

  implicit val jsonWritesYourTradingPremises: Writes[YourTradingPremises] = (
    (__ \ "tradingName").write[String] and
      (__).write[TradingPremisesAddress] and
      (__).write[PremiseOwner] and
      (__ \ "startOfTradingDate").write[LocalDate] and
      (__).write[IsResidential]
    ) (unlift(YourTradingPremises.unapply))


  implicit val formRule: Rule[UrlFormEncoded, YourTradingPremises] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (
        (__ \ "tradingName").read[String] ~
          (__).read[TradingPremisesAddress] ~
          (__).read[PremiseOwner] ~
          (__ \ "startOfTradingDate").read[LocalDate] ~
          (__).read[IsResidential]
        ).apply(YourTradingPremises.apply _)
    }


  implicit val formWrites: Write[YourTradingPremises, UrlFormEncoded] = To[UrlFormEncoded] { __ =>

    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift

    (
      (__ \ "tradingName").write[String] ~
        __.write[TradingPremisesAddress] ~
        __.write[PremiseOwner] ~
        (__ \ "startOfTradingDate").write[LocalDate] ~
        __.write[IsResidential]) (unlift(YourTradingPremises.unapply _))
  }

  //Read and Write for LocalDate
  implicit val readsJSONStringToLocalDate: Reads[LocalDate] =  {
    (JsPath \ "startOfTradingDate").read[String].map(dateString => LocalDate.parse(dateString))
  }

  implicit val writeLocalDateToJSONString: Writes[LocalDate] = Writes[LocalDate] { case localDate => (JsPath \ "startOfTradingDate").write[String].writes(localDate.toString) }

}

sealed trait PremiseOwner

case object PremiseOwnerSelf extends PremiseOwner

case object PremiseOwnerAnother extends PremiseOwner


object PremiseOwner {

  implicit val jsonReadsPremiseOwner: Reads[PremiseOwner] = {
    (JsPath \ "premiseOwner").read[Boolean] fmap {
      case true => PremiseOwnerSelf
      case false => PremiseOwnerAnother
    }
  }

  implicit val jsonWritesPremiseOwner: Writes[PremiseOwner] = Writes[PremiseOwner] {
    case PremiseOwnerSelf => (JsPath \ "premiseOwner").write[Boolean].writes(true)
    case PremiseOwnerAnother => (JsPath \ "premiseOwner").write[Boolean].writes(false)
  }

  implicit val formRule: Rule[UrlFormEncoded, PremiseOwner] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._

    (__ \ "premiseOwner").read[Boolean] flatMap {
      case true => Rule.fromMapping { _ => Success(PremiseOwnerSelf) }
      case false => Rule.fromMapping { _ => Success(PremiseOwnerAnother) }
    }
  }

  implicit val formWrites: Write[PremiseOwner, UrlFormEncoded] = To[UrlFormEncoded] { __ =>

    Write {
      case PremiseOwnerSelf => Map("premiseOwner" -> Seq("true"))
      case PremiseOwnerAnother => Map("premiseOwner" -> Seq("false"))
    }
  }

}

sealed trait IsResidential

case object ResidentialYes extends IsResidential

case object ResidentialNo extends IsResidential

object IsResidential {

  implicit val formRule: Rule[UrlFormEncoded, IsResidential] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._

    (__ \ "isResidential").read[Boolean] flatMap {
      case true => Rule.fromMapping { _ => Success(ResidentialYes) }
      case false => Rule.fromMapping { _ => Success(ResidentialNo) }
    }
  }

  implicit val formWrites: Write[IsResidential, UrlFormEncoded] = To[UrlFormEncoded] { __ =>

    Write {
      case ResidentialYes => Map("isResidential" -> Seq("true"))
      case ResidentialNo => Map("isResidential" -> Seq("false"))
    }
  }

  implicit val jsonReadsIsResidential: Reads[IsResidential] = {
    (JsPath \ "isResidential").read[Boolean] fmap {
      case true => ResidentialYes
      case false => ResidentialNo
    }
  }

  implicit val jsonWritesIsResidential: Writes[IsResidential] = Writes[IsResidential] {
    case ResidentialYes => (JsPath \ "isResidential").write[Boolean].writes(true)
    case ResidentialNo => (JsPath \ "isResidential").write[Boolean].writes(false)
  }
}
