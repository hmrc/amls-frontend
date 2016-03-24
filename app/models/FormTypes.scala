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
  val maxOtherBusinessActivityTypeLength = 255

  val maxAccountName = 40
  val maxIBANLength = 34
  val maxNonUKBankAccountNumberLength = 40
  val maxUKBankAccountNumberLength = 8
  val maxSoftwareNameLength = 40
  val maxFranchiseName = 140
  val maxEmployeeLength = 11
  val minAccountantRefNoTypeLength = 11
  val maxRoleWithinBusinessOtherType = 255
  val maxTypeOfBusinessLength = 40


  def customNotEmpty(message:String) = validateWith[String](message) { !_.isEmpty }

  def customMaxLength(l: Int, message:String) = validateWith[String](message, l) { _.size <= l }

  def customRegex(regex: scala.util.matching.Regex, message:String) =  validateWith[String](message, regex) { regex.unapplySeq(_: String).isDefined }

  val notEmptyStrip = Rule.zero[String] fmap { _.trim }

  val indivNameType = notEmpty compose maxLength(maxNameTypeLength)

  val descriptionType = notEmptyStrip compose notEmpty compose maxLength(maxDescriptionTypeLength)

  val vrnType = notEmpty compose maxLength(maxVRNTypeLength) compose pattern("^[0-9]{9}$".r)

  val validateAddress = customMaxLength(maxAddressLength, "error.max.length.address.line")

  val addressType = customNotEmpty("error.required.address.line") compose validateAddress

  val postcodeType = customNotEmpty("error.required.postcode")  compose customMaxLength(maxPostCodeTypeLength, "error.invalid.postcode")

  val countryRegex = "^[a-zA-Z_]{2}+$".r

  val countryType = customNotEmpty("error.required.country") compose customRegex(countryRegex, "error.invalid.country")

  val phoneNumberType = notEmpty compose maxLength(maxPhoneNumberLength) compose pattern("[0-9]+".r)

  val emailRegex = "^.+@.+$".r

  val emailType = notEmpty compose maxLength(maxEMailLength) compose pattern(emailRegex)

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

  val accountNameType = notEmptyStrip compose notEmpty compose maxLength(maxAccountName)

  val sortCodeRegex = "^[0-9]{6}".r
  val sortCodeType = notEmpty compose pattern(sortCodeRegex)

  val ukBankAccountNumberRegex = "^[0-9]{8}$".r
  val ukBankAccountNumberType = notEmpty compose maxLength(maxUKBankAccountNumberLength) compose pattern(ukBankAccountNumberRegex)

  val nonUKBankAccountNumberType = notEmpty compose maxLength(maxNonUKBankAccountNumberLength) compose pattern("^[0-9a-zA-Z_]+$".r)

  val ibanType = notEmpty compose maxLength(maxIBANLength) compose pattern("^[0-9a-zA-Z_]+$".r)

  val softwareNameType =  notEmptyStrip compose notEmpty compose maxLength (maxSoftwareNameLength)

  val franchiseNameType =  notEmptyStrip compose notEmpty compose maxLength(maxFranchiseName)

  val OtherBusinessActivityType = notEmptyStrip compose customNotEmpty("error.required.ba.enter.text") compose maxLength(maxOtherBusinessActivityTypeLength)

  val employeeCountType = notEmptyStrip compose notEmpty compose maxLength(maxEmployeeLength) compose pattern("^[0-9]+$".r)

  val accountantRefNoType = notEmpty compose maxLength(minAccountantRefNoTypeLength) compose minLength(minAccountantRefNoTypeLength)

  val declarationNameType = notEmptyStrip compose notEmpty compose maxLength(maxNameTypeLength)

  val roleWithinBusinessOtherType = notEmptyStrip compose notEmpty compose maxLength(maxRoleWithinBusinessOtherType)

  val typeOfBusinessType = notEmptyStrip compose notEmpty compose maxLength(maxTypeOfBusinessLength)
}