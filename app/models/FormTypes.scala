/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import cats.data.Validated.{Invalid, Valid}
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import models.businessmatching.{BusinessActivities, BusinessActivity}
import org.joda.time.LocalDate
import utils.DateHelper.localDateOrdering
import utils.TraversableValidators.minLengthR

import scala.util.matching.Regex

object FormTypes {

  import jto.validation.forms.Rules._
  import utils.MappingUtils.Implicits._

  /** Lengths **/

  val maxNameTypeLength = 35
  val maxDescriptionTypeLength = 140
  val maxAddressLength = 35
  val maxPhoneNumberLength = 24
  val maxEmailLength = 100
  val minAccountantRefNoTypeLength = 11
  val maxNonUKPassportLength = 40

  /** Regex **/

  val vrnTypeRegex = "^[0-9]{9}$".r
  private val phoneNumberRegex = "^[0-9 ()+\u2010\u002d]{1,24}$".r
  private val addressTypeRegex = "^[A-Za-z0-9 !'‘’\"“”(),./\u2014\u2013\u2010\u002d]{1,35}$".r
  val emailRegex = "(?:[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-zA-Z0-9-]*[a-zA-Z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])".r
  
  val dayRegex = "(0?[1-9]|[12][0-9]|3[01])".r
  val monthRegex = "(0?[1-9]|1[012])".r
  val yearRegexPost1900 = "((19|20)\\d\\d)".r
  val yearRegexFourDigits = "(?<!\\d)(?!0000)\\d{4}(?!\\d)".r
  val corporationTaxRegex = "^[0-9]{10}$".r
  val ninoRegex = "(AA|AB|AE|AH|AK|AL|AM|AP|AR|AS|AT|AW|AX|AY|AZ|BA|BB|BE|BH|BK|BL|BM|BT|CA|CB|CE|CH|CK|CL|CR|EA|EB|EE|EH|EK|EL|EM|EP|ER|ES|ET|EW|EX|EY|EZ|GY|HA|HB|HE|HH|HK|HL|HM|HP|HR|HS|HT|HW|HX|HY|HZ|JA|JB|JC|JE|JG|JH|JJ|JK|JL|JM|JN|JP|JR|JS|JT|JW|JX|JY|JZ|KA|KB|KE|KH|KK|KL|KM|KP|KR|KS|KT|KW|KX|KY|KZ|LA|LB|LE|LH|LK|LL|LM|LP|LR|LS|LT|LW|LX|LY|LZ|MA|MN|MW|MX|NA|NB|NE|NH|NL|NM|NP|NR|NS|NW|NX|NY|NZ|OA|OB|OE|OH|OK|OL|OM|OP|OR|OS|OX|PA|PB|PC|PE|PG|PH|PJ|PK|PL|PM|PN|PP|PR|PS|PT|PW|PX|PY|RA|RB|RE|RH|RK|RM|RP|RR|RS|RT|RW|RX|RY|RZ|SA|SB|SC|SE|SG|SH|SJ|SK|SL|SM|SN|SP|SR|SS|ST|SW|SX|SY|SZ|TA|TB|TE|TH|TK|TL|TM|TN|TP|TR|TS|TT|TW|TX|TY|TZ|WA|WB|WE|WK|WL|WM|WP|YA|YB|YE|YH|YK|YL|YM|YP|YR|YS|YT|YW|YX|YY|YZ|ZA|ZB|ZE|ZH|ZK|ZL|ZM|ZP|ZR|ZS|ZT|ZW|ZX|ZY)[0-9]{6}[A-D]".r

  private val basicPunctuationRegex = "^[a-zA-Z0-9\u00C0-\u00FF !#$%&'‘’\"“”«»()*+,./:;=?@\\[\\]|~£€¥\\u005C\u2014\u2013\u2010\u005F\u005E\u0060\u000A\u000D\u002d]+$".r
  private val postcodeRegex = "^[A-Za-z]{1,2}[0-9][0-9A-Za-z]?\\s?[0-9][A-Za-z]{2}$".r

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

  def basicPunctuationPattern(msg: String = "err.text.validation") = regexWithMsg(basicPunctuationRegex, msg)
  val postcodePattern = regexWithMsg(postcodeRegex, "error.invalid.postcode")

  val referenceNumberRegex = """^[0-9]{8}|[a-zA-Z0-9]{15}$""".r
  def referenceNumberRule(msg: String = "error.invalid.mlr.number") = regexWithMsg(referenceNumberRegex, msg)

  val extendedReferenceNumberRegex = """^[0-9]{6}$""".r
  def extendedReferenceNumberRule(msg: String) = regexWithMsg(extendedReferenceNumberRegex, msg)

  /** Name Rules **/

  private val commonNameRegex = "^[a-zA-Z\\u00C0-\\u00FF '‘’\\u2014\\u2013\\u2010\\u002d]+$".r
  val commonNameRegexRule = regexWithMsg(commonNameRegex, "error.invalid.common_name.validation")

  private val middleNameLength = maxWithMsg(maxNameTypeLength, "error.invalid.length.middlename")
  val middleNameType = notEmpty andThen middleNameLength

  /** VAT Registration Number Rules **/

  private val vrnRequired = required("error.required.vat.number")
  private val vrnRegex = regexWithMsg(vrnTypeRegex, "error.invalid.vat.number")

  val vrnType = vrnRequired andThen vrnRegex

  /** Corporation Tax Type Rules **/

  private val corporationTaxRequired = required("error.required.atb.corporation.tax.number")
  private val corporationTaxPattern = regexWithMsg(corporationTaxRegex, "error.invalid.atb.corporation.tax.number")
  private val addressTypePattern = regexWithMsg(addressTypeRegex, "err.text.validation")

  val corporationTaxType = corporationTaxRequired andThen corporationTaxPattern

  /** Address Rules **/

  val validateAddress = maxLength(maxAddressLength).withMessage("error.max.length.address.line") andThen addressTypePattern

  private val postcodeRequired = required("error.invalid.postcode")

  val postcodeType = postcodeRequired andThen postcodePattern


  /** Contact Details Rules **/

  private val nameMaxLength = 140
  val nameRequired = required("error.required.yourname")
  val nameType = maxLength(nameMaxLength).withMessage("error.invalid.yourname")

  private val phoneNumberRequired = required("error.required.phone.number")
  private val phoneNumberLength = maxWithMsg(maxPhoneNumberLength, "error.max.length.phone")
  private val phoneNumberPattern = regexWithMsg(phoneNumberRegex, "err.invalid.phone.number")

  private val emailRequired = required("error.required.rp.email")
  private val emailLength = maxWithMsg(maxEmailLength, "error.max.length.rp.email")
  private val emailPattern = regexWithMsg(emailRegex, "error.invalid.rp.email")

  private val dayRequired = required("error.required.tp.date")
  private val dayPattern = regexWithMsg(dayRegex, "error.invalid.tp.date")

  private val monthRequired = required("error.required.tp.month")
  private val monthPattern = regexWithMsg(monthRegex, "error.invalid.tp.month")

  private val yearRequired = required("error.required.tp.year")
  private val yearPatternPost1900 = regexWithMsg(yearRegexPost1900, "error.invalid.year.post1900")
  private val yearPattern = regexWithMsg(yearRegexFourDigits, "error.invalid.year")

  val phoneNumberType = phoneNumberRequired andThen phoneNumberLength andThen phoneNumberPattern
  val emailType = emailRequired andThen emailLength andThen emailPattern
  val dayType = dayRequired andThen dayPattern
  val monthType = monthRequired andThen monthPattern
  private val yearTypePost1900: Rule[String, String] = yearRequired andThen yearPatternPost1900
  private val yearType: Rule[String, String] = yearRequired andThen yearPattern

  def localDateRuleWithPattern : Rule[UrlFormEncoded, LocalDate] = From[UrlFormEncoded] { __ =>
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
  val localDateFutureRule: Rule[UrlFormEncoded, LocalDate] = localDateRuleWithPattern andThen futureDateRule

  val dateOfChangeActivityStartDateRuleMapping = Rule.fromMapping[(Option[LocalDate], LocalDate), LocalDate] {
    case (Some(d1), d2) if d2.isAfter(d1) => Valid(d2)
    case (None, d2) => Valid(d2)
    case (Some(activityStartDate), _) => Invalid(Seq(
      ValidationError("error.expected.dateofchange.date.after.activitystartdate", activityStartDate.toString("dd-MM-yyyy"))))
  }

  val confirmEmailMatchRuleMapping = Rule.fromMapping[(String, String), (String,String)] {
    case email@(s1, s2) if s1.equals(s2) => Valid(email)
    case _ => Invalid(Seq(ValidationError(List("error.mismatch.atb.email"))))
  }

  val dateOfChangeActivityStartDateRule = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    ((__ \ "activityStartDate").read(optionR(jodaLocalDateR("yyyy-MM-dd"))) ~
      (__ \ "dateOfChange").read(localDateFutureRule)).tupled.andThen(dateOfChangeActivityStartDateRuleMapping).repath(_ => Path \ "dateOfChange")
  }

  val confirmEmailMatchRule = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    ((__ \ "email").read(emailType) ~
      (__ \ "confirmEmail").read(emailType)).tupled.andThen(confirmEmailMatchRuleMapping)
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

  def businessActivityRule(msg: String) = From[UrlFormEncoded] { __ =>
    (__ \ "businessActivities").read(minLengthR[Set[BusinessActivity]](1).withMessage(msg)) map (BusinessActivities(_))
  }

  /** Business Identifier Rules */

  //TODO: Add error messages

  val accountantRefNoType = notEmpty
    .andThen(maxLength(minAccountantRefNoTypeLength))
    .andThen(minLength(minAccountantRefNoTypeLength))

  val declarationNameType = notEmptyStrip
    .andThen(notEmpty)
    .andThen(maxLength(maxNameTypeLength))
    .andThen(regexWithMsg(commonNameRegex, "err.text.validation"))

  def genericNameRule(requiredMsg: String = "", maxLengthMsg: String = "error.invalid.common_name.length") =
    notEmptyStrip
      .andThen(notEmpty.withMessage(requiredMsg))
      .andThen(commonNameRegexRule)
      .andThen(maxWithMsg(maxNameTypeLength, maxLengthMsg))

  /** Personal Identification Rules **/

  private val ninoRequired = required("error.required.nino")
  private val ninoPattern = regexWithMsg(ninoRegex, "error.invalid.nino")
  private val ninoTransforms = removeSpacesRule andThen removeDashRule andThen transformUppercase

  val ninoType = ninoTransforms andThen ninoRequired andThen ninoPattern

}
