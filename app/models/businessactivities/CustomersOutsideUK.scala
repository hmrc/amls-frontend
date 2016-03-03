package models.businessactivities


import models.FormTypes._
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json._


sealed trait CustomersOutsideUK

case object CustomersOutsideUKNo extends  CustomersOutsideUK

case class CustomersOutsideUKYes(countries: Countries) extends  CustomersOutsideUK

case class Countries (
                       country_1: String,
                       country_2: Option[String] = None,
                       country_3: Option[String] = None,
                       country_4: Option[String] = None,
                       country_5: Option[String] = None,
                       country_6: Option[String] = None,
                       country_7: Option[String] = None,
                       country_8: Option[String] = None,
                       country_9: Option[String] = None,
                       country_10: Option[String] = None
                     )

object CustomersOutsideUK {

  implicit val formRule: Rule[UrlFormEncoded, CustomersOutsideUK] = From[UrlFormEncoded] { __ =>

    import play.api.data.mapping.forms.Rules._
    (__ \ "isOutside").read[Boolean] flatMap {
      case true =>
       __.read[Countries].fmap(CustomersOutsideUKYes.apply)
      case false => Rule.fromMapping { _ => Success(CustomersOutsideUKNo) }
    }
  }

  implicit val formRuleCountry: Rule[UrlFormEncoded, Countries] = From[UrlFormEncoded] { __ =>
   import play.api.data.mapping.forms.Rules._
        ((__ \ "country_1").read(countryType) and
          (__ \ "country_2").read(optionR(countryType)) and
          (__ \ "country_3").read(optionR(countryType)) and
          (__ \ "country_4").read(optionR(countryType)) and
          (__ \ "country_5").read(optionR(countryType)) and
          (__ \ "country_6").read(optionR(countryType)) and
          (__ \ "country_7").read(optionR(countryType)) and
          (__ \ "country_8").read(optionR(countryType)) and
          (__ \ "country_9").read(optionR(countryType)) and
          (__ \ "country_10").read(optionR(countryType)))(Countries.apply _)
    }

  implicit val formWrites: Write[CustomersOutsideUK, UrlFormEncoded] = Write {
    case CustomersOutsideUKYes(countries) => {
      Map("isOutside" -> Seq("true"),
        "country_1" -> Seq(countries.country_1),
        "country_2" -> Seq(countries.country_2.getOrElse("")),
        "country_3" -> Seq(countries.country_3.getOrElse("")),
        "country_4" -> Seq(countries.country_4.getOrElse("")),
        "country_5" -> Seq(countries.country_5.getOrElse("")),
        "country_6" -> Seq(countries.country_6.getOrElse("")),
        "country_7" -> Seq(countries.country_7.getOrElse("")),
        "country_8" -> Seq(countries.country_8.getOrElse("")),
        "country_9" -> Seq(countries.country_9.getOrElse("")),
        "country_10" -> Seq(countries.country_10.getOrElse(""))
      )
    }
    case CustomersOutsideUKNo => Map("isOutside" ->  Seq("false"))
  }

  implicit val jsonReads: Reads[CustomersOutsideUK] = {
    import play.api.libs.json._
    import play.api.libs.json.Reads._
    import play.api.libs.functional.syntax._
    (__ \ "isOutside").read[Boolean] flatMap {
      case true =>
        ((__ \ "country_1").read[String] and
          (__ \ "country_2").read[Option[String]] and
          (__ \ "country_3").read[Option[String]] and
          (__ \ "country_4").read[Option[String]] and
          (__ \ "country_5").read[Option[String]] and
          (__ \ "country_6").read[Option[String]] and
          (__ \ "country_7").read[Option[String]] and
          (__ \ "country_8").read[Option[String]] and
          (__ \ "country_9").read[Option[String]] and
          (__ \ "country_10").read[Option[String]]) (Countries.apply _).fmap(CustomersOutsideUKYes.apply)
      case false => Reads(_ => JsSuccess(CustomersOutsideUKNo))
    }
  }

  implicit val jsonWrites = Writes[CustomersOutsideUK] {
    case CustomersOutsideUKYes(countries) => {
      Json.obj("isOutside" -> true,
              "country_1" -> countries.country_1,
              "country_2" -> countries.country_1,
              "country_3" -> countries.country_1,
              "country_4" -> countries.country_1,
              "country_5" -> countries.country_1,
              "country_6" -> countries.country_1,
              "country_7" -> countries.country_1,
              "country_8" -> countries.country_1,
              "country_9" -> countries.country_1,
              "country_10" -> countries.country_1)
    }
    case CustomersOutsideUKNo => Json.obj("isOutside" -> false)
  }
}