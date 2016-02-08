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
                               startOfTradingDate: HMRCLocalDate,
                               isResidential: IsResidential)

object YourTradingPremises {

  implicit val jsonReadsYourTradingPremises = {
    ((JsPath \ "tradingName").read[String] and
      JsPath.read[TradingPremisesAddress] and
      JsPath.read[PremiseOwner] and
      JsPath.read[HMRCLocalDate] and
      JsPath.read[IsResidential]) (YourTradingPremises.apply _)
  }

  implicit val jsonWritesYourTradingPremises: Writes[YourTradingPremises] = (
    (JsPath \ "tradingName").write[String] and
      JsPath.write[TradingPremisesAddress] and
      JsPath.write[PremiseOwner] and
      JsPath.write[HMRCLocalDate] and
      JsPath.write[IsResidential]
    ) (unlift(YourTradingPremises.unapply))


  implicit val formRuleYourTradingPremises: Rule[UrlFormEncoded, YourTradingPremises] = From[UrlFormEncoded] { urlFormValue =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (
      (urlFormValue \ "tradingName").read(descriptionType) ~
        urlFormValue.read[TradingPremisesAddress] ~
        urlFormValue.read[PremiseOwner] ~
        urlFormValue.read[HMRCLocalDate] ~
        urlFormValue.read[IsResidential]
      ).apply(YourTradingPremises.apply _)
  }


  implicit val formWriteYourTradingPremises: Write[YourTradingPremises, UrlFormEncoded] = To[UrlFormEncoded] { yourTradingPremises =>

    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift

    (
      (yourTradingPremises \ "tradingName").write[String] ~
        yourTradingPremises.write[TradingPremisesAddress] ~
        yourTradingPremises.write[PremiseOwner] ~
        yourTradingPremises.write[HMRCLocalDate] ~
        yourTradingPremises.write[IsResidential]) (unlift(YourTradingPremises.unapply _))
  }

  //Read and Write for LocalDate
  implicit val jsonReadsToLocalDate: Reads[LocalDate] = {
    (JsPath \ "startOfTradingDate").read[String].map(dateString => LocalDate.parse(dateString))
  }

  implicit val jsonWriteLocalDate: Writes[LocalDate] = Writes[LocalDate] {
    case localDate => (JsPath \ "startOfTradingDate").write[String].writes(localDate.toString)
  }

}

case class HMRCLocalDate(yyyy: String,
                         mm: String,
                         dd: String)


object HMRCLocalDate {


  implicit val jsonReadsHMRCLocalDate: Reads[HMRCLocalDate] = {
    (JsPath \ "startOfTradingDate").read[String].map {
      dateString => {
        val Array(yyyy, mm, dd) = dateString.split("-")
        HMRCLocalDate(yyyy, mm, dd)
      }
    }
  }

  implicit val jsonWritesHMRCLocalDate: Writes[HMRCLocalDate] = Writes[HMRCLocalDate] {
    case localDate => {
      (JsPath \ "startOfTradingDate").write[String]
        .writes(localDate.yyyy + "-" +
          localDate.mm + "-" +
          localDate.dd
        )
    }
  }

  implicit val formRuleHMRCLocalDate: Rule[UrlFormEncoded, HMRCLocalDate] = From[UrlFormEncoded] { urlFormEncoded =>
    import models.FormTypes._
    import play.api.data.mapping.forms.Rules._

    (
      (urlFormEncoded \ "yyyy").read(yearType) ~
        (urlFormEncoded \ "mm").read(monthType) ~
        (urlFormEncoded \ "dd").read(dayType)
      ).apply(HMRCLocalDate.apply _)
  }

  implicit val formWriteHMRCLocalDate: Write[HMRCLocalDate, UrlFormEncoded] = To[UrlFormEncoded] { hmrcLocalDate =>

    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (hmrcLocalDate \ "yyyy").write[String] ~
        (hmrcLocalDate \ "mm").write[String] ~
        (hmrcLocalDate \ "dd").write[String]) (unlift(HMRCLocalDate.unapply _)
    )
  }
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

  implicit val formRulePremiseOwner: Rule[UrlFormEncoded, PremiseOwner] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._

    (__ \ "premiseOwner").read[Boolean] flatMap {
      case true => Rule.fromMapping { _ => Success(PremiseOwnerSelf) }
      case false => Rule.fromMapping { _ => Success(PremiseOwnerAnother) }
    }
  }

  implicit val formWritePremiseOwner: Write[PremiseOwner, UrlFormEncoded] = To[UrlFormEncoded] { __ =>

    Write {
      case PremiseOwnerSelf => Map("premiseOwner" -> Seq("true"))
      case PremiseOwnerAnother => Map("premiseOwner" -> Seq("false"))
      case _ => Map("premiseOwner" -> Seq(""))
    }
  }

}

sealed trait IsResidential

case object ResidentialYes extends IsResidential

case object ResidentialNo extends IsResidential

object IsResidential {

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


  implicit val formRuleIsResidential: Rule[UrlFormEncoded, IsResidential] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._

    (__ \ "isResidential").read[Boolean] flatMap {
      case true => Rule.fromMapping { _ => Success(ResidentialYes) }
      case false => Rule.fromMapping { _ => Success(ResidentialNo) }
    }
  }

  implicit val formWriteIsResidential: Write[IsResidential, UrlFormEncoded] = To[UrlFormEncoded] { __ =>

    Write {
      case ResidentialYes => Map("isResidential" -> Seq("true"))
      case ResidentialNo => Map("isResidential" -> Seq("false"))
    }
  }


}
