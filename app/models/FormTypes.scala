package models

import org.joda.time.LocalDate
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded

object FormTypes {

  import play.api.data.mapping.forms.Rules._

  val maxNameTypeLength = 35
  val maxDescriptionTypeLength = 140
  val maxAddressLength = 35
  val maxPostCodeTypeLength = 10
  val maxCountryTypeLength = 2
  val maxPrevMLRRegNoLength = 15
  val maxVRNTypeLength = 9
  val maxPhoneNumberLength = 30
  val maxEMailLength = 100
  val maxPenalisedTypeLength = 255
  val maxAgentNameLength = 140
  val maxRedressOtherTypeLength = 255

  val indivNameType =
    notEmpty compose maxLength(maxNameTypeLength)

  val descriptionType =
    notEmpty compose maxLength(maxDescriptionTypeLength)

  val prevMLRRegNoType =  notEmpty compose maxLength(maxPrevMLRRegNoLength) compose pattern("^([0-9]{8}|[0-9]{15})$".r)

  val vrnType = notEmpty compose maxLength(maxVRNTypeLength) compose pattern("^[0-9]{9}$".r)

  val addressType =
    notEmpty compose maxLength(maxAddressLength)

  val postCodeType =
    notEmpty compose maxLength(maxPostCodeTypeLength)

  val countryType =
    notEmpty compose maxLength(maxCountryTypeLength)

  val phoneNumberType = notEmpty compose maxLength(maxPhoneNumberLength) compose pattern("[0-9]+".r)

  val emailType = notEmpty compose maxLength(maxEMailLength)

  val penalisedType = notEmpty compose maxLength(maxPenalisedTypeLength)

  val agentNameType = notEmpty compose maxLength(maxAgentNameLength)
  val redressOtherType = notEmpty compose maxLength(maxRedressOtherTypeLength)

  val dayType = notEmpty compose minLength(minLengthDayOrMonth) compose maxLength(maxLengthDayOrMonth) compose pattern("(0?[1-9]|[12][0-9]|3[01])".r)

  val monthType = notEmpty compose minLength(minLengthDayOrMonth) compose maxLength(maxLengthDayOrMonth) compose pattern("(0?[1-9]|1[012])".r)

  val yearType = notEmpty compose minLength(yearLength) compose maxLength(yearLength) compose pattern("((19|20)\\d\\d)".r)

  val premisesTradingNameType = maxLength(maxLengthPremisesTradingName)

  val localDateRule: Rule[UrlFormEncoded, LocalDate] =
    From[UrlFormEncoded] { __ =>
      (
        (__ \ "year").read[String] ~
        (__ \ "month").read[String] ~
        (__ \ "day").read[String]
      )( (y, m, d) => s"$y-$m-$d" ) compose jodaLocalDateRule("yyyy-MM-dd")
    }.repath( _ => Path)

  val localDateWrite: Write[LocalDate, UrlFormEncoded] =
   To[UrlFormEncoded] { __ =>
     import play.api.data.mapping.forms.Writes._
     (
       (__ \ "year").write[String] ~
       (__ \ "month").write[String] ~
       (__ \ "day").write[String]
     )( d => (d.year.getAsString, d.monthOfYear.getAsString, d.dayOfMonth.getAsString))
   }
}

}