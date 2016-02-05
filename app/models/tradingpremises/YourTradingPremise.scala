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
    (__ \ "tradingName").write[String] and
      (__).write[TradingPremisesAddress] and
      (__).write[PremiseOwner] and
      (__).write[HMRCLocalDate] and
      (__).write[IsResidential]
    ) (unlift(YourTradingPremises.unapply))


  implicit val formRuleYourTradingPremises: Rule[UrlFormEncoded, YourTradingPremises] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (
        (__ \ "tradingName").read[String] ~
          (__).read[TradingPremisesAddress] ~
          (__).read[PremiseOwner] ~
          (__).read[HMRCLocalDate] ~
          (__).read[IsResidential]
        ).apply(YourTradingPremises.apply _)
    }


  implicit val formWritesYourTradingPremises: Write[YourTradingPremises, UrlFormEncoded] = To[UrlFormEncoded] { __ =>

    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift

    (
      (__ \ "tradingName").write[String] ~
        __.write[TradingPremisesAddress] ~
        __.write[PremiseOwner] ~
        (__ \ "startOfTradingDate").write[HMRCLocalDate] ~
        __.write[IsResidential]) (unlift(YourTradingPremises.unapply _))
  }

  //Read and Write for LocalDate
  implicit val jsonReadsToLocalDate: Reads[LocalDate] = {
    (JsPath \ "startOfTradingDate").read[String].map(dateString => LocalDate.parse(dateString))
  }

  implicit val writeLocalDateToJSONString: Writes[LocalDate] = Writes[LocalDate] { case localDate => (JsPath \ "startOfTradingDate").write[String].writes(localDate.toString) }

}

case class HMRCLocalDate(yyyy: String,
                         mm: String,
                         dd: String)


object HMRCLocalDate {


  implicit val jsonReadsToCreateHMRCLocalDate: Reads[HMRCLocalDate] = {
    (JsPath \ "startOfTradingDate").read[String].map {
      dateString => {
        println("******************JSON READ Local date******************" + dateString)
        val Array(yyyy, mm, dd) = dateString.split("-")
        HMRCLocalDate(yyyy, mm, dd)
      }
    }
  }

  implicit val writeHMRCLocalDateToJSONString: Writes[HMRCLocalDate] = Writes[HMRCLocalDate] {
    case localDate => {
      println("*********************JSON WRITE Local date*****************" + localDate)
      (JsPath \ "startOfTradingDate").write[String]
        .writes(localDate.yyyy + "-" +
          localDate.mm + "-" +
          localDate.dd
        )
    }
  }

  implicit val formRuleHMRCLocalDate: Rule[UrlFormEncoded, HMRCLocalDate] = From[UrlFormEncoded] { __ =>
    import models.FormTypes._
    import play.api.data.mapping.forms.Rules._

    println("********************************Inside FORM RULE Local date******************************")
    (
      (__ \ "yyyy").read(yearType) ~
        (__ \ "mm").read(dayOrMonthType) ~
        (__ \ "dd").read(dayOrMonthType)
      ).apply(HMRCLocalDate.apply _)
  }

  implicit val formWritesToFormHMRCLocalDate: Write[HMRCLocalDate, UrlFormEncoded] = To[UrlFormEncoded] { __ =>

    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift

    println("********************************Inside FORM WRITE Local date******************************")
    (
      (__ \ "yyyy").write[String] ~
        (__ \ "mm").write[String] ~
        (__ \ "dd").write[String]) (unlift(HMRCLocalDate.unapply _))

    /*
        Write {
          case x: HMRCLocalDate =>
            Map("yyyy" -> Seq("1999"),
              "mm" -> Seq("09"),
              "dd" -> Seq("09")
            )
        }
    */


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

  implicit val formRuleFromForm: Rule[UrlFormEncoded, PremiseOwner] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._

    (__ \ "premiseOwner").read[Boolean] flatMap {
      case true => Rule.fromMapping { _ => Success(PremiseOwnerSelf) }
      case false => Rule.fromMapping { _ => Success(PremiseOwnerAnother) }
    }
  }

  implicit val formWritesToForm: Write[PremiseOwner, UrlFormEncoded] = To[UrlFormEncoded] { __ =>

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


  implicit val formRuleFromForm: Rule[UrlFormEncoded, IsResidential] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._

    (__ \ "isResidential").read[Boolean] flatMap {
      case true => Rule.fromMapping { _ => Success(ResidentialYes) }
      case false => Rule.fromMapping { _ => Success(ResidentialNo) }
    }
  }

  implicit val formWritesToForm: Write[IsResidential, UrlFormEncoded] = To[UrlFormEncoded] { __ =>

    Write {
      case ResidentialYes => Map("isResidential" -> Seq("true"))
      case ResidentialNo => Map("isResidential" -> Seq("false"))
    }
  }


}
