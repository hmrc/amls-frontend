package models.businessactivities


import models.FormTypes._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.libs.json.{Json, Reads, Writes}

sealed trait CustomersOutsideUK {

  def toLines: Seq[String] = this match {
    case CustomersOutsideUKYes(values) =>
        Seq(
          Some(values.country_1),
          values.country_2,
          values.country_3,
          values.country_4,
          values.country_5,
          values.country_6,
          values.country_7,
          values.country_8,
          values.country_9,
          values.country_10
        ).flatten
    case CustomersOutsideUKNo => Seq.empty
  }
}

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
    import utils.MappingUtils.Implicits._

    import play.api.data.mapping.forms.Rules._
    (__ \ "isOutside").read[Option[Boolean]] flatMap {
      case Some(true) =>
       __.read[Countries].fmap(CustomersOutsideUKYes.apply)
      case Some(false) => Rule.fromMapping { _ => Success(CustomersOutsideUKNo) }
      case _ => Path \ "isOutside" -> Seq(ValidationError("error.required.ba.select.country"))
    }
  }

  implicit val formRuleCountry: Rule[UrlFormEncoded, Countries] = From[UrlFormEncoded] { __ =>
   import play.api.data.mapping.forms.Rules._
        ((__ \ "country_1").read(customNotEmpty("error.required.ba.country.name") compose customRegex(countryRegex, "error.invalid.country")) and
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
        "country_2" -> countries.country_2.toSeq,
        "country_3" -> countries.country_3.toSeq,
        "country_4" -> countries.country_4.toSeq,
        "country_5" -> countries.country_5.toSeq,
        "country_6" -> countries.country_6.toSeq,
        "country_7" -> countries.country_7.toSeq,
        "country_8" -> countries.country_8.toSeq,
        "country_9" -> countries.country_9.toSeq,
        "country_10" -> countries.country_10.toSeq
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
          (__ \ "country_2").readNullable[String] and
          (__ \ "country_3").readNullable[String] and
          (__ \ "country_4").readNullable[String] and
          (__ \ "country_5").readNullable[String] and
          (__ \ "country_6").readNullable[String] and
          (__ \ "country_7").readNullable[String] and
          (__ \ "country_8").readNullable[String] and
          (__ \ "country_9").readNullable[String] and
          (__ \ "country_10").readNullable[String]) (Countries.apply _).fmap(CustomersOutsideUKYes.apply)
      case false => Reads(_ => JsSuccess(CustomersOutsideUKNo))
    }
  }

  implicit val jsonWrites = Writes[CustomersOutsideUK] {
    case CustomersOutsideUKYes(countries) => {
      Json.obj("isOutside" -> true,
              "country_1" -> countries.country_1,
              "country_2" -> countries.country_2,
              "country_3" -> countries.country_3,
              "country_4" -> countries.country_4,
              "country_5" -> countries.country_5,
              "country_6" -> countries.country_6,
              "country_7" -> countries.country_7,
              "country_8" -> countries.country_8,
              "country_9" -> countries.country_9,
              "country_10" -> countries.country_10)
    }
    case CustomersOutsideUKNo => Json.obj("isOutside" -> false)
  }
}