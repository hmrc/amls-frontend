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
  val minLengthDayOrMonth = 1
  val maxLengthDayOrMonth = 2
  val yearLength = 4
  val maxRedressOtherTypeLength = 255
  val maxLengthPremisesTradingName = 120

  val maxAccountName = 40
  val maxIBANLength = 34
  val maxNonUKBankAccountNumberLength = 40
  val maxUKBankAccountNumberLength = 8
  val maxSortCodeLength = 6

  val indivNameType = notEmpty compose maxLength(maxNameTypeLength)

  val descriptionType = notEmpty compose maxLength(maxDescriptionTypeLength)

  val prevMLRRegNoType = notEmpty compose maxLength(maxPrevMLRRegNoLength) compose pattern("^([0-9]{8}|[0-9]{15})$".r)

  val vrnType = notEmpty compose maxLength(maxVRNTypeLength) compose pattern("^[0-9]{9}$".r)

  val addressType = notEmpty compose maxLength(maxAddressLength)

  val postcodeType = notEmpty compose maxLength(maxPostCodeTypeLength)

  val countryType = notEmpty compose maxLength(maxCountryTypeLength)

  val phoneNumberType = notEmpty compose maxLength(maxPhoneNumberLength) compose pattern("[0-9]+".r)

  val emailType = notEmpty compose maxLength(maxEMailLength)

  val penalisedType = notEmpty compose maxLength(maxPenalisedTypeLength)

  val agentNameType = notEmpty compose maxLength(maxAgentNameLength)

  val dayType = notEmpty compose minLength(minLengthDayOrMonth) compose maxLength(maxLengthDayOrMonth) compose pattern("(0?[1-9]|[12][0-9]|3[01])".r)

  val monthType = notEmpty compose minLength(minLengthDayOrMonth) compose maxLength(maxLengthDayOrMonth) compose pattern("(0?[1-9]|1[012])".r)

  val yearType = notEmpty compose minLength(yearLength) compose maxLength(yearLength) compose pattern("((19|20)\\d\\d)".r)

  val redressOtherType = notEmpty compose maxLength(maxRedressOtherTypeLength)

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

  val accountNameType = notEmpty compose maxLength(maxAccountName)

  val sortCodeType = notEmpty compose maxLength(maxSortCodeLength) compose pattern("^[0-9]{6}".r)//compose pattern("\\d{2}-?\\d{2}-?\\d{2}".r)

  val ukBankAccountNumberType = notEmpty compose maxLength(maxUKBankAccountNumberLength) compose pattern("^[0-9]{8}$".r)

  val nonUKBankAccountNumberType = notEmpty compose maxLength(maxNonUKBankAccountNumberLength) compose pattern("^[0-9a-zA-Z_]+$".r)

  val ibanType = notEmpty compose maxLength(maxIBANLength) compose pattern("^[0-9a-zA-Z_]+$".r)

}