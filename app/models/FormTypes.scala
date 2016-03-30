package models

import org.joda.time.LocalDate
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded

object FormTypes {

  import play.api.data.mapping.forms.Rules._
  import utils.MappingUtils.Implicits._

  val maxNameTypeLength = 35
  val maxDescriptionTypeLength = 140
  val maxAddressLength = 35
  val maxPostCodeTypeLength = 10
  val maxVRNTypeLength = 9
  val maxPhoneNumberLength = 30
  val maxEMailLength = 100
  val minLengthDayOrMonth = 1
  val maxLengthDayOrMonth = 2
  val yearLength = 4

  val maxLengthPremisesTradingName = 120
  val maxOtherBusinessActivityTypeLength = 255

  val maxAccountName = 40
  val maxIBANLength = 34
  val maxNonUKBankAccountNumberLength = 40
  val maxUKBankAccountNumberLength = 8
  val maxSortCodeLength = 6
  val maxEmployeeLength = 11
  val minAccountantRefNoTypeLength = 11
  val maxRoleWithinBusinessOtherType = 255
  val maxTypeOfBusinessLength = 40

  val notEmptyStrip = Rule.zero[String] fmap { _.trim }

  val indivNameType = notEmpty compose maxLength(maxNameTypeLength)

  val descriptionType = notEmptyStrip compose notEmpty compose maxLength(maxDescriptionTypeLength)

  val vrnTypeRegex = "^[0-9]{9}$".r
  val vrnType = notEmpty.withMessage("error.required.vat.number") compose pattern(vrnTypeRegex).withMessage("error.invalid.vat.number")

  val validateAddress = maxLength(maxAddressLength).withMessage("error.max.length.address.line")

  val postcodeType = notEmpty.withMessage("error.required.postcode")  compose maxLength(maxPostCodeTypeLength).withMessage("error.invalid.postcode")

  val phoneNumberType = notEmpty compose maxLength(maxPhoneNumberLength) compose pattern("[0-9]+".r)

  val emailRegex = "^.+@.+$".r

  val emailType = notEmpty compose maxLength(maxEMailLength) compose pattern(emailRegex)

  val dayRegex = "(0?[1-9]|[12][0-9]|3[01])".r
  val dayType = notEmpty.withMessage("error.required.tp.date") compose pattern(dayRegex).withMessage("error.invalid.tp.date")

  val monthRegex = "(0?[1-9]|1[012])".r
  val monthType =  notEmpty.withMessage("error.required.tp.month") compose pattern(monthRegex).withMessage("error.invalid.tp.date")

  val yearRegex = "((19|20)\\d\\d)".r
  val yearType =  notEmpty.withMessage("error.required.tp.year") compose pattern(yearRegex).withMessage("error.invalid.tp.date")

  val premisesTradingNameType = notEmpty.withMessage("error.required.tp.trading.name") compose
    maxLength(maxLengthPremisesTradingName).withMessage("error.invalid.tp.trading.name")

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

  val accountNameType = notEmptyStrip compose notEmpty compose maxLength(maxAccountName)

  val sortCodeRegex = "^[0-9]{6}".r
  val sortCodeType = notEmpty compose pattern(sortCodeRegex)

  val ukBankAccountNumberRegex = "^[0-9]{8}$".r
  val ukBankAccountNumberType = notEmpty compose maxLength(maxUKBankAccountNumberLength) compose pattern(ukBankAccountNumberRegex)

  val nonUKBankAccountNumberType = notEmpty compose maxLength(maxNonUKBankAccountNumberLength) compose pattern("^[0-9a-zA-Z_]+$".r)

  val ibanType = notEmpty compose maxLength(maxIBANLength) compose pattern("^[0-9a-zA-Z_]+$".r)

  val accountantRefNoType = notEmpty compose maxLength(minAccountantRefNoTypeLength) compose minLength(minAccountantRefNoTypeLength)

  val declarationNameType = notEmptyStrip compose notEmpty compose maxLength(maxNameTypeLength)

  val roleWithinBusinessOtherType = notEmptyStrip compose notEmpty compose maxLength(maxRoleWithinBusinessOtherType)

  val typeOfBusinessType = notEmptyStrip compose notEmpty compose maxLength(maxTypeOfBusinessLength)
}