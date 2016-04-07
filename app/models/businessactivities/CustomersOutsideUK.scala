package models.businessactivities

import models.Country
import models.FormTypes._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{From, Rule, Success, Write}
import play.api.data.mapping._
import play.api.libs.json.{Json, Reads, Writes}

sealed trait CustomersOutsideUK {

  def toLines: Seq[String] = this match {
    case CustomersOutsideUKYes(values) =>
        Seq(
          Some(values.country_1.toString),
          values.country_2 map { _.toString },
          values.country_3 map { _.toString },
          values.country_4 map { _.toString },
          values.country_5 map { _.toString },
          values.country_6 map { _.toString },
          values.country_7 map { _.toString },
          values.country_8 map { _.toString },
          values.country_9 map { _.toString },
          values.country_10 map { _.toString }
        ).flatten
    case CustomersOutsideUKNo => Seq.empty
  }
}

case object CustomersOutsideUKNo extends  CustomersOutsideUK

case class CustomersOutsideUKYes(countries: Countries) extends  CustomersOutsideUK

case class Countries (
                       country_1: Country,
                       country_2: Option[Country] = None,
                       country_3: Option[Country] = None,
                       country_4: Option[Country] = None,
                       country_5: Option[Country] = None,
                       country_6: Option[Country] = None,
                       country_7: Option[Country] = None,
                       country_8: Option[Country] = None,
                       country_9: Option[Country] = None,
                       country_10: Option[Country] = None
                     )

object CustomersOutsideUK {


  implicit val formRule: Rule[UrlFormEncoded, CustomersOutsideUK] = From[UrlFormEncoded] { __ =>
    import utils.MappingUtils.Implicits._
    import play.api.data.mapping.forms.Rules._
    (__ \ "isOutside").read[Boolean].withMessage("error.required.ba.select.country") flatMap {
      case true =>
       __.read[Countries].fmap(CustomersOutsideUKYes.apply)
      case false => Rule.fromMapping { _ => Success(CustomersOutsideUKNo) }
    }
  }

  implicit val formRuleCountry: Rule[UrlFormEncoded, Countries] = From[UrlFormEncoded] { __ =>
    import utils.MappingUtils.Implicits._
   import play.api.data.mapping.forms.Rules._
        ((__ \ "country_1").read[Country].withMessage("error.required.ba.country.name") and
          (__ \ "country_2").read[Option[Country]] and
          (__ \ "country_3").read[Option[Country]] and
          (__ \ "country_4").read[Option[Country]] and
          (__ \ "country_5").read[Option[Country]] and
          (__ \ "country_6").read[Option[Country]] and
          (__ \ "country_7").read[Option[Country]] and
          (__ \ "country_8").read[Option[Country]] and
          (__ \ "country_9").read[Option[Country]] and
          (__ \ "country_10").read[Option[Country]]
          )(Countries.apply _)
    }

  implicit val formWrites: Write[CustomersOutsideUK, UrlFormEncoded] = Write {
    case CustomersOutsideUKYes(countries) => {
      Map("isOutside" -> Seq("true"),
        "country_1" -> Seq(countries.country_1.code),
        "country_2" -> (countries.country_2.toSeq map { _.code }),
        "country_3" -> (countries.country_3.toSeq map { _.code }),
        "country_4" -> (countries.country_4.toSeq map { _.code }),
        "country_5" -> (countries.country_5.toSeq map { _.code }),
        "country_6" -> (countries.country_6.toSeq map { _.code }),
        "country_7" -> (countries.country_7.toSeq map { _.code }),
        "country_8" -> (countries.country_8.toSeq map { _.code }),
        "country_9" -> (countries.country_9.toSeq map { _.code }),
        "country_10" -> (countries.country_10.toSeq map { _.code })
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
        ((__ \ "country_1").read[Country] and
          (__ \ "country_2").readNullable[Country] and
          (__ \ "country_3").readNullable[Country] and
          (__ \ "country_4").readNullable[Country] and
          (__ \ "country_5").readNullable[Country] and
          (__ \ "country_6").readNullable[Country] and
          (__ \ "country_7").readNullable[Country] and
          (__ \ "country_8").readNullable[Country] and
          (__ \ "country_9").readNullable[Country] and
          (__ \ "country_10").readNullable[Country]
          )(Countries.apply _).fmap(CustomersOutsideUKYes.apply)
      case false => Reads(_ => JsSuccess(CustomersOutsideUKNo))
    }
  }

  implicit val jsonWrites = Writes[CustomersOutsideUK] {
    case CustomersOutsideUKYes(countries) => {
      Json.obj(
        "isOutside" -> true,
        "country_1" -> countries.country_1,
        "country_2" -> countries.country_2,
        "country_3" -> countries.country_3,
        "country_4" -> countries.country_4,
        "country_5" -> countries.country_5,
        "country_6" -> countries.country_6,
        "country_7" -> countries.country_7,
        "country_8" -> countries.country_8,
        "country_9" -> countries.country_9,
        "country_10" -> countries.country_10
      )
    }
    case CustomersOutsideUKNo => Json.obj("isOutside" -> false)
  }
}
