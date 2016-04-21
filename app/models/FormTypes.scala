package models

import org.joda.time.LocalDate
import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms.UrlFormEncoded

import scala.util.matching.Regex

object FormTypes {

  import play.api.data.mapping.forms.Rules._
  import utils.MappingUtils.Implicits._

  /** Lengths **/

  val maxNameTypeLength = 35
  val maxDescriptionTypeLength = 140
  val maxAddressLength = 35
  val maxPostCodeTypeLength = 10
  val maxPhoneNumberLength = 30
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
  val phoneNumberRegex = "[0-9]+".r
  val emailRegex = "^.+@.+$".r
  val dayRegex = "(0?[1-9]|[12][0-9]|3[01])".r
  val monthRegex = "(0?[1-9]|1[012])".r
  val yearRegex = "((19|20)\\d\\d)".r
  val corporationTaxRegex = "^[0-9]{10}$".r
  val sortCodeRegex = "^[0-9]{6}".r
  val ukBankAccountNumberRegex = "^[0-9]{8}$".r
  val nonUKBankAccountNumberRegex = "^[0-9a-zA-Z_]+$".r
  val ibanRegex = "^[0-9a-zA-Z_]+$".r
  val ninoRegex = "^^[A-Z]{2}[0-9]{6}[A-Z]{1}$".r
  val passportRegex = "^[0-9a-zA-Z_]{9}+$".r

  /** Helper Functions **/

  def maxWithMsg(length: Int, msg: String) = maxLength(length).withMessage(msg)
  def regexWithMsg(regex: Regex, msg: String) = pattern(regex).withMessage(msg)
  def required(msg: String) = notEmpty.withMessage(msg)
  val notEmptyStrip = Rule.zero[String] fmap { _.trim }

  /** Name Rules **/

  private val firstNameRequired = required("error.required.firstname")
  private val firstNameLength = maxWithMsg(maxNameTypeLength, "error.invalid.length.firstname")
  private val middleNameLength = maxWithMsg(maxNameTypeLength, "error.invalid.length.middlename")
  private val lastNameRequired = required("error.required.lastname")
  private val lastNameLength = maxWithMsg(maxNameTypeLength, "error.invalid.length.lastname")

  val firstNameType = firstNameRequired compose firstNameLength
  val middleNameType = middleNameLength
  val lastNameType = lastNameRequired compose lastNameLength

  /** VAT Registration Number Rules **/

  private val vrnRequired = required("error.required.vat.number")
  private val vrnRegex = regexWithMsg(vrnTypeRegex, "error.invalid.vat.number")

  val vrnType = vrnRequired compose vrnRegex

  /** Corporation Tax Type Rules **/

  private val corporationTaxRequired = required("error.required.atb.corporation.tax.number")
  private val corporationTaxPattern = regexWithMsg(corporationTaxRegex, "error.invalid.atb.corporation.tax.number")

  val corporationTaxType = corporationTaxRequired compose corporationTaxPattern

  /** Address Rules **/

  val addressType = notEmpty compose maxLength(maxAddressLength)

  val validateAddress = maxLength(maxAddressLength).withMessage("error.max.length.address.line")

  private val postcodeRequired = required("error.required.postcode")
  private val postcodeLength =  maxWithMsg(maxPostCodeTypeLength, "error.invalid.postcode")

  val postcodeType = postcodeRequired compose postcodeLength

  /** Contact Details Rules **/

  private val phoneNumberRequired = required("error.required.rp.phone")
  private val phoneNumberLength = maxWithMsg(maxPhoneNumberLength, "error.max.length.rp.phone")
  private val phoneNumberPattern = regexWithMsg(phoneNumberRegex, "error.invalid.rp.phone")

  private val emailRequired = required("error.required.rp.email")
  private val emailLength = maxWithMsg(maxEmailLength, "error.max.length.rp.email")
  private val emailPattern =regexWithMsg(emailRegex, "error.invalid.rp.email")

  private val dayRequired = required("error.required.tp.date")
  private val dayPattern = regexWithMsg(dayRegex, "error.invalid.tp.date")

  private val monthRequired = required("error.required.tp.month")
  private val monthPattern = regexWithMsg(monthRegex, "error.invalid.tp.date")

  private val yearRequired = required("error.required.tp.year")
  private val yearPattern = regexWithMsg(yearRegex, "error.invalid.tp.date")

  val phoneNumberType = phoneNumberRequired compose phoneNumberLength compose phoneNumberPattern
  val emailType = emailRequired compose emailLength compose emailPattern
  val dayType = dayRequired compose dayPattern
  val monthType = monthRequired compose monthPattern
  val yearType = yearRequired compose yearPattern

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

  /** Bank details Rules **/

  //TODO: Add error messages

  val accountNameType = notEmptyStrip compose notEmpty compose maxLength(maxAccountName)
  val sortCodeType = notEmpty compose pattern(sortCodeRegex)
  val ukBankAccountNumberType = notEmpty compose maxLength(maxUKBankAccountNumberLength) compose pattern(ukBankAccountNumberRegex)
  val nonUKBankAccountNumberType = notEmpty compose maxLength(maxNonUKBankAccountNumberLength) compose pattern(nonUKBankAccountNumberRegex)
  val ibanType = notEmpty compose maxLength(maxIBANLength) compose pattern(ibanRegex)

  /** Business Identifier Rules */

  //TODO: Add error messages

  val accountantRefNoType = notEmpty compose maxLength(minAccountantRefNoTypeLength) compose minLength(minAccountantRefNoTypeLength)
  val declarationNameType = notEmptyStrip compose notEmpty compose maxLength(maxNameTypeLength)
  val roleWithinBusinessOtherType = notEmptyStrip compose notEmpty compose maxLength(maxRoleWithinBusinessOtherType)
  val typeOfBusinessType = notEmptyStrip compose notEmpty compose maxLength(maxTypeOfBusinessLength)

  /** Personal Identification Rules **/

  private val ninoRequired = required("error.required.nino")
  private val ninoPattern = regexWithMsg(ninoRegex, "error.invalid.nino")

  private val passportRequired = required("error.required.uk.passport")
  private val passportPattern = regexWithMsg(passportRegex, "error.invalid.uk.passport")

  private val nonUKPassportRequired = required("error.required.non.uk.passport")
  private val nonUkPassportLength = maxWithMsg(maxNonUKPassportLength, "error.invalid.non.uk.passport")

  val ninoType = ninoRequired compose ninoPattern
  val ukPassportType = passportRequired compose passportPattern
  val noUKPassportType = nonUKPassportRequired compose nonUkPassportLength


}
