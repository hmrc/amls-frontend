package models

import cats.data.Validated.{Invalid, Valid}
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import org.joda.time.LocalDate
import utils.DateHelper.localDateOrdering

import scala.util.matching.Regex

object FormTypes {

  import jto.validation.forms.Rules._
  import utils.MappingUtils.Implicits._

  /** Lengths **/

  val maxNameTypeLength = 35
  val maxDescriptionTypeLength = 140
  val maxAddressLength = 35
  val maxPostCodeTypeLength = 10
  val maxPhoneNumberLength = 24
  val maxEmailLength = 100
  val maxAccountName = 40
  val maxIBANLength = 34
  val maxNonUKBankAccountNumberLength = 40
  val maxUKBankAccountNumberLength = 8
  val minAccountantRefNoTypeLength = 11
  val maxRoleWithinBusinessOtherType = 255
  val maxTypeOfBusinessLength = 40
  val maxNonUKPassportLength = 40

  /** Regex **/

  val vrnTypeRegex = "^[0-9]{9}$".r
  val phoneNumberRegex = "[0-9()+\\- ]+".r
  val emailRegex = ("^.+" + //Any character 1 or more times
    "@" + //@ symbol
    "(" + //start of DNS label group
    "(?!\\-)" + //does not start with hyphen
    "[a-zA-Z0-9\\-]+" + //one or more alphanumerics or hyphen
    "(?<!\\-)" + //does not end with a hyphen
    "\\." + //dot
    ")*" + //zero or more dns labels followed by dot
    "(" + //start of Top level dns label (same as a DNS label but not dot after it)
    "(?!\\-)" +
    "[a-zA-Z0-9\\-]+" +
    "(?<!\\-)" +
    ")$").r
  val dayRegex = "(0?[1-9]|[12][0-9]|3[01])".r
  val monthRegex = "(0?[1-9]|1[012])".r
  val yearRegex = "((19|20)\\d\\d)".r
  val corporationTaxRegex = "^[0-9]{10}$".r
  val sortCodeRegex = "^[0-9]{6}".r
  val ukBankAccountNumberRegex = "^[0-9]{8}$".r
  val nonUKBankAccountNumberRegex = "^[0-9a-zA-Z_]+$".r
  val ibanRegex = "^[0-9a-zA-Z_]+$".r
  val ninoRegex = "(AA|AB|AE|AH|AK|AL|AM|AP|AR|AS|AT|AW|AX|AY|AZ|BA|BB|BE|BH|BK|BL|BM|BT|CA|CB|CE|CH|CK|CL|CR|EA|EB|EE|EH|EK|EL|EM|EP|ER|ES|ET|EW|EX|EY|EZ|GY|HA|HB|HE|HH|HK|HL|HM|HP|HR|HS|HT|HW|HX|HY|HZ|JA|JB|JC|JE|JG|JH|JJ|JK|JL|JM|JN|JP|JR|JS|JT|JW|JX|JY|JZ|KA|KB|KE|KH|KK|KL|KM|KP|KR|KS|KT|KW|KX|KY|KZ|LA|LB|LE|LH|LK|LL|LM|LP|LR|LS|LT|LW|LX|LY|LZ|MA|MW|MX|NA|NB|NE|NH|NL|NM|NP|NR|NS|NW|NX|NY|NZ|OA|OB|OE|OH|OK|OL|OM|OP|OR|OS|OX|PA|PB|PC|PE|PG|PH|PJ|PK|PL|PM|PN|PP|PR|PS|PT|PW|PX|PY|RA|RB|RE|RH|RK|RM|RP|RR|RS|RT|RW|RX|RY|RZ|SA|SB|SC|SE|SG|SH|SJ|SK|SL|SM|SN|SP|SR|SS|ST|SW|SX|SY|SZ|TA|TB|TE|TH|TK|TL|TM|TN|TP|TR|TS|TT|TW|TX|TY|TZ|WA|WB|WE|WK|WL|WM|WP|YA|YB|YE|YH|YK|YL|YM|YP|YR|YS|YT|YW|YX|YY|YZ|ZA|ZB|ZE|ZH|ZK|ZL|ZM|ZP|ZR|ZS|ZT|ZW|ZX|ZY)[0-9]{6}[A-D]".r
  val passportRegex = "^[0-9]{9}+$".r

  private val basicPunctuationRegex = "^[a-zA-Z0-9\u00C0-\u00FF !#$%&'‘’\"“”«»()*+,./:;=?@\\[\\]|~£€¥\\u005C\u2014\u2013\u2010\u005F\u005E\u0060\u002d]+$".r

  /** Helper Functions **/

  def maxWithMsg(length: Int, msg: String) = maxLength(length).withMessage(msg)

  def regexWithMsg(regex: Regex, msg: String) = pattern(regex).withMessage(msg)

  def required(msg: String) = notEmpty.withMessage(msg)

  def maxDateWithMsg(maxDate: LocalDate, msg: String) = max(maxDate).withMessage(msg)

  val notEmptyStrip = Rule.zero[String] map {
    _.trim
  }

  val valueOrNone = Rule.zero[String] map {
    case "" => None
    case str => Some(str)
  }

  val transformUppercase = Rule.zero[String] map {
    _.toUpperCase
  }

  implicit class RegexHelpers(regex: Regex) {
    def insensitive = s"(?i)${regex.pattern}".r
  }

  def removeCharacterRule(c: Char) = Rule.zero[String] map {
    _.replace(c.toString, "")
  }

  val removeSpacesRule: Rule[String, String] = removeCharacterRule(' ')
  val removeDashRule: Rule[String, String] = removeCharacterRule('-')

  val basicPunctuationPattern = regexWithMsg(basicPunctuationRegex, "err.text.validation")

  val extendedReferenceNumberRegex = """^[A-Za-z0-9\-\s]+$""".r
  def extendedReferenceNumberRule(msg: String) = regexWithMsg(extendedReferenceNumberRegex, msg)

  /** Name Rules **/

  private val firstNameRequired = required("error.required.firstname")
  private val firstNameLength = maxWithMsg(maxNameTypeLength, "error.invalid.length.firstname")
  private val middleNameLength = maxWithMsg(maxNameTypeLength, "error.invalid.length.middlename")
  private val lastNameRequired = required("error.required.lastname")
  private val lastNameLength = maxWithMsg(maxNameTypeLength, "error.invalid.length.lastname")

  val firstNameType = firstNameRequired andThen firstNameLength
  val middleNameType = notEmpty andThen middleNameLength
  val lastNameType = lastNameRequired andThen lastNameLength

  /** VAT Registration Number Rules **/

  private val vrnRequired = required("error.required.vat.number")
  private val vrnRegex = regexWithMsg(vrnTypeRegex, "error.invalid.vat.number")

  val vrnType = vrnRequired andThen vrnRegex

  /** Corporation Tax Type Rules **/

  private val corporationTaxRequired = required("error.required.atb.corporation.tax.number")
  private val corporationTaxPattern = regexWithMsg(corporationTaxRegex, "error.invalid.atb.corporation.tax.number")

  val corporationTaxType = corporationTaxRequired andThen corporationTaxPattern

  /** Address Rules **/

  val addressType = notEmpty andThen maxLength(maxAddressLength)

  val validateAddress = maxLength(maxAddressLength).withMessage("error.max.length.address.line")

  private val postcodeRequired = required("error.required.postcode")
  private val postcodeLength = maxWithMsg(maxPostCodeTypeLength, "error.invalid.postcode")

  val postcodeType = postcodeRequired andThen postcodeLength

  /** Contact Details Rules **/

  private val nameMaxLength = 140
  val nameRequired = required("error.required.yourname")
  val nameType = maxLength(nameMaxLength).withMessage("error.invalid.yourname")

  private val phoneNumberRequired = required("error.required.rp.phone")
  private val phoneNumberLength = maxWithMsg(maxPhoneNumberLength, "error.max.length.rp.phone")
  private val phoneNumberPattern = regexWithMsg(phoneNumberRegex, "error.invalid.rp.phone")

  private val emailRequired = required("error.required.rp.email")
  private val emailLength = maxWithMsg(maxEmailLength, "error.max.length.rp.email")
  private val emailPattern = regexWithMsg(emailRegex, "error.invalid.rp.email")

  private val dayRequired = required("error.required.tp.date")
  private val dayPattern = regexWithMsg(dayRegex, "error.invalid.tp.date")

  private val monthRequired = required("error.required.tp.month")
  private val monthPattern = regexWithMsg(monthRegex, "error.invalid.tp.month")

  private val yearRequired = required("error.required.tp.year")
  private val yearPattern = regexWithMsg(yearRegex, "error.invalid.tp.year")

  val phoneNumberType = phoneNumberRequired andThen phoneNumberLength andThen phoneNumberPattern
  val emailType = emailRequired andThen emailLength andThen emailPattern
  val dayType = dayRequired andThen dayPattern
  val monthType = monthRequired andThen monthPattern
  val yearType: Rule[String, String] = yearRequired andThen yearPattern

  val localDateRule: Rule[UrlFormEncoded, LocalDate] =
    From[UrlFormEncoded] { __ =>
      (
        (__ \ "year").read(yearType) ~
          (__ \ "month").read(monthType) ~
          (__ \ "day").read(dayType)
        ) ((y, m, d) => s"$y-$m-$d") orElse
        Rule[UrlFormEncoded, String](__ => Valid("INVALID DATE STRING")) andThen
        jodaLocalDateR("yyyy-MM-dd")
    }.repath(_ => Path)

  val localDateWrite: Write[LocalDate, UrlFormEncoded] =
    To[UrlFormEncoded] { __ =>
      import jto.validation.forms.Writes._
      (
        (__ \ "year").write[String] ~
          (__ \ "month").write[String] ~
          (__ \ "day").write[String]
        ) (d => (d.year.getAsString, d.monthOfYear.getAsString, d.dayOfMonth.getAsString))
    }

  val futureDateRule: Rule[LocalDate, LocalDate] = maxDateWithMsg(LocalDate.now, "error.future.date")
  val localDateFutureRule: Rule[UrlFormEncoded, LocalDate] = localDateRule andThen futureDateRule

  val dateOfChangeActivityStartDateRuleMapping = Rule.fromMapping[(Option[LocalDate], LocalDate), LocalDate] {
    case (Some(d1), d2) if d2.isAfter(d1) => Valid(d2)
    case (None, d2) => Valid(d2)
    case (Some(activityStartDate), _) => Invalid(Seq(
      ValidationError("error.expected.dateofchange.date.after.activitystartdate", activityStartDate.toString("dd-MM-yyyy"))))
  }

  val dateOfChangeActivityStartDateRule = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    ((__ \ "activityStartDate").read(optionR(jodaLocalDateR("yyyy-MM-dd"))) ~
      (__ \ "dateOfChange").read(localDateFutureRule)).tupled.andThen(dateOfChangeActivityStartDateRuleMapping).repath(_ => Path \ "dateOfChange")
  }

  val premisesEndDateRuleMapping = Rule.fromMapping[(LocalDate, LocalDate), LocalDate] {
    case (d1, d2) if d2.isAfter(d1) => Valid(d2)
    case (startDate, _) => Invalid(Seq(ValidationError("error.expected.tp.date.after.start", startDate.toString("dd-MM-yyyy"))))
  }

  val premisesEndDateRule = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    ((__ \ "premisesStartDate").read(jodaLocalDateR("yyyy-MM-dd")) ~
      (__ \ "endDate").read(localDateFutureRule)).tupled.andThen(premisesEndDateRuleMapping).repath(_ => Path \ "endDate")
  }

  val peopleEndDateRuleMapping = Rule.fromMapping[(LocalDate, LocalDate, String), LocalDate] {
    case (d1, d2, un) if d2.isAfter(d1) => Valid(d2)
    case (startDate, _, userName) => Invalid(Seq(ValidationError("error.expected.rp.date.after.start", userName, startDate.toString("dd-MM-yyyy"))))
  }

  val peopleEndDateRule = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    ((__ \ "positionStartDate").read(jodaLocalDateR("yyyy-MM-dd")) ~
      (__ \ "endDate").read(localDateFutureRule) ~
      (__ \ "userName").read[String]).tupled.andThen(peopleEndDateRuleMapping).repath(_ => Path \ "endDate")
  }

  /** Bank details Rules **/

  //TODO: Add error messages

  val accountNameType = notEmptyStrip
    .andThen(notEmpty.withMessage("error.bankdetails.accountname"))
    .andThen(maxLength(maxAccountName).withMessage("error.invalid.bankdetails.accountname"))
    .andThen(pattern(basicPunctuationRegex).withMessage("err.text.validation"))

  val sortCodeType = (removeDashRule andThen removeSpacesRule andThen notEmpty)
    .withMessage("error.invalid.bankdetails.sortcode")
    .andThen(pattern(sortCodeRegex).withMessage("error.invalid.bankdetails.sortcode"))


  val ukBankAccountNumberType = notEmpty
    .withMessage("error.bankdetails.accountnumber")
    .andThen(maxLength(maxUKBankAccountNumberLength).withMessage("error.max.length.bankdetails.accountnumber"))
    .andThen(pattern(ukBankAccountNumberRegex).withMessage("error.invalid.bankdetails.accountnumber"))

  val nonUKBankAccountNumberType = notEmpty
    .andThen(maxLength(maxNonUKBankAccountNumberLength).withMessage("error.max.length.bankdetails.account"))
    .andThen(pattern(nonUKBankAccountNumberRegex).withMessage("error.invalid.bankdetails.account"))

  val ibanType = notEmpty
    .andThen(maxLength(maxIBANLength).withMessage("error.max.length.bankdetails.iban"))
    .andThen(pattern(ibanRegex).withMessage("error.invalid.bankdetails.iban"))

  /** Business Identifier Rules */

  //TODO: Add error messages

  val accountantRefNoType = notEmpty
    .andThen(maxLength(minAccountantRefNoTypeLength))
    .andThen(minLength(minAccountantRefNoTypeLength))

  val declarationNameType = notEmptyStrip
    .andThen(notEmpty)
    .andThen(maxLength(maxNameTypeLength))

  val roleWithinBusinessOtherType = notEmptyStrip
    .andThen(notEmpty)
    .andThen(maxLength(maxRoleWithinBusinessOtherType))

  val typeOfBusinessType = notEmptyStrip
    .andThen(notEmpty.withMessage("error.required.bm.businesstype.type"))
    .andThen(maxLength(maxTypeOfBusinessLength).withMessage("error.invalid.bm.business.type"))

  /** Personal Identification Rules **/

  private val ninoRequired = required("error.required.nino")
  private val ninoPattern = regexWithMsg(ninoRegex, "error.invalid.nino")
  private val ninoTransforms = removeSpacesRule andThen removeDashRule andThen transformUppercase

  private val passportRequired = required("error.required.uk.passport")
  private val passportPattern = regexWithMsg(passportRegex, "error.invalid.uk.passport")

  private val nonUKPassportRequired = required("error.required.non.uk.passport")
  private val nonUkPassportLength = maxWithMsg(maxNonUKPassportLength, "error.invalid.non.uk.passport")

  val ninoType = ninoTransforms andThen ninoRequired andThen ninoPattern
  val ukPassportType = passportRequired andThen passportPattern
  val noUKPassportType = nonUKPassportRequired andThen nonUkPassportLength
}
