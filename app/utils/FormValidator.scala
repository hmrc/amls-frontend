package utils

import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.Mapping
import config.AmlsPropertiesReader._
import uk.gov.hmrc.play.validators.Validators.isPostcodeLengthValid
import play.api.data.FormError
import play.api.data.Forms
import play.api.data.format.Formatter
import scala.collection.mutable.ListBuffer

trait FormValidator {
  private lazy val ninoRegex = """^$|^[A-Z,a-z]{2}[0-9]{6}[A-D,a-d]{1}$""".r
  private lazy val emailRegex = """(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?""".r
  private lazy val postCodeRegex = "(([gG][iI][rR] {0,}0[aA]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) {0,}[0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))".r
  private lazy val phoneNoRegex = "^[A-Z0-9 \\)\\/\\(\\-\\*#]{1,27}$".r
  private lazy val currencyRegex = """^(\d{1,11}+)$""".r
  private lazy val sortCodeRegex = """^\d{2}(-|\s*)?\d{2}\1\d{2}$""".r
  private lazy val accountNumberRegex = """^(\d){8}$""".r
  private lazy val ibanRegex = """^[a-zA-Z]{2}[0-9]{2}[a-zA-Z0-9]{11,30}$""".r


  def stopOnFirstFail[T](constraints: Constraint[T]*) = Constraint { field: T =>
    constraints.toList dropWhile (_(field) == Valid) match {
      case Nil => Valid
      case constraint :: _ => constraint(field)
    }
  }

  def mandatoryText(blankValueMessageKey:String, invalidLengthMessageKey: String,
                    validationMaxLengthProperty: String): Mapping[String] = {
    val constraint = Constraint("Blank and length")( {
      t:String => t match {
        case t if t.length == 0 => Invalid(blankValueMessageKey)
        case t if t.length > getProperty(validationMaxLengthProperty).toInt =>
          Invalid(invalidLengthMessageKey)
        case _ => Valid
      }
    } )
    text.verifying(constraint)
  }

  private def mandatoryNinoFormatter(blankValueMessageKey: String, invalidLengthMessageKey: String,
                                     invalidValueMessageKey: String) = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(n) =>
          val s = n.trim.replaceAll(" ", "")
          s match{
            case p if p.length==0 => Left(Seq(FormError(key, blankValueMessageKey)))
            case num => {
              if (num.length > getProperty("validationMaxLengthNINO").toInt) {
                Left(Seq(FormError(key, invalidLengthMessageKey)))
              } else {
                ninoRegex.findFirstIn(num) match {
                  case None => Left(Seq(FormError(key, invalidValueMessageKey)))
                  case _ => Right(num)
                }
              }
            }
          }
        case _ => Left(Seq(FormError(key, "Nothing to validate")))
      }
    }
    override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)
  }

  def mandatoryNino( blankValueMessageKey: String,
                            invalidLengthMessageKey: String,
                            invalidValueMessageKey: String) =
    Forms.of[String](mandatoryNinoFormatter(blankValueMessageKey,
      invalidLengthMessageKey, invalidValueMessageKey))

  def mandatoryEmail(blankValueMessageKey: String, invalidLengthMessageKey: String,
                     invalidValueMessageKey: String) : Mapping[String] = {
    val blankConstraint = Constraint("Blank")( {
      t:String => t match {
        case t if t.length == 0 => Invalid(blankValueMessageKey)
        case t if t.length > getProperty("validationMaxLengthEmail").toInt =>
          Invalid(invalidLengthMessageKey)
        case _ => Valid
      }
    } )

    val valueConstraint = Constraint("Value")( {
      t:String => t match {
        case t if t.matches(emailRegex.regex) => Valid
        case _ => Invalid(invalidValueMessageKey)
      }
    } )
    text.verifying(stopOnFirstFail(blankConstraint, valueConstraint))
  }

  private def mandatoryPhoneNumberFormatter(blankValueMessageKey: String,
                                    invalidLengthMessageKey: String,
                                    invalidValueMessageKey: String) = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(n) =>
          val s = n.trim.toUpperCase
          val t = if (s.length > 0 && s.charAt(0)=='+') "00" + s.substring(1) else s
          t match{
            case p if p.length==0 => Left(Seq(FormError(key, blankValueMessageKey)))
            case num => {
              if (num.length > getProperty("validationMaxLengthPhoneNo").toInt) {
                Left(Seq(FormError(key, invalidLengthMessageKey)))
              } else {
                phoneNoRegex.findFirstIn(num) match {
                  case None => Left(Seq(FormError(key, invalidValueMessageKey)))
                  case _ => Right(num)
                }
              }
            }
          }

        case _ => Left(Seq(FormError(key, "Nothing to validate")))
      }
    }
    override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)
  }

  def mandatoryPhoneNumber( blankValueMessageKey: String,
                            invalidLengthMessageKey: String,
                            invalidValueMessageKey: String) =
    Forms.of[String](mandatoryPhoneNumberFormatter(blankValueMessageKey,
      invalidLengthMessageKey, invalidValueMessageKey))

  private def getAddrDetails(data: Map[String, String], addr1Key: String,
                             addr2Key: String, addr3Key:String,
                             addr4Key:String, postcodeKey:String,
                             countryCodeKey: String) = {
    (data.getOrElse(addr1Key, ""), data.getOrElse(addr2Key, ""),
      data.getOrElse(addr3Key, ""), data.getOrElse(addr4Key, ""),
      data.getOrElse(postcodeKey, ""), data.getOrElse(countryCodeKey, ""))
  }

  private def validateOptionalAddressLine(addrKey:String, addr:String, maxLength:Int,
                                          invalidAddressLineMessageKey:String,
                                          errors: ListBuffer[FormError] ): Unit = {
    addr match {
      case a if a.length > maxLength => errors += FormError(addrKey, invalidAddressLineMessageKey)
      case _ => {}
    }
  }

  private def validateMandatoryAddressLine(addrKey:String, addr:String, maxLength:Int,
                                           blankMessageKey:String,
                                           invalidAddressLineMessageKey: String,
                                           errors: ListBuffer[FormError]): Unit = {
    addr match {
      case a if a.length == 0 => errors += FormError(addrKey, blankMessageKey)
      case a if a.length > maxLength => errors += FormError(addrKey, invalidAddressLineMessageKey)
      case _ => {}
    }
  }

  private def validatePostcode(postcode:String, postcodeKey: String,
                               blankPostcodeMessageKey: String,
                               invalidPostcodeMessageKey: String,
                               errors: ListBuffer[FormError]) = {
    postcode match {
      case a if a.length == 0 => errors += FormError(postcodeKey, blankPostcodeMessageKey)
      case a if a.length > 0 &&  postCodeRegex.findFirstIn(a).isEmpty =>
        errors +=FormError(postcodeKey, invalidPostcodeMessageKey)
      case a if !isPostcodeLengthValid(a) =>
        errors += FormError(postcodeKey, invalidPostcodeMessageKey)
      case _ => {}
    }
  }

  private def addressFormatter(addr2Key: String, addr3Key:String, addr4Key:String,
                 postcodeKey:String, countryCodeKey: String,
                 blankMandatoryAddrLineMessageKey: String,
                 blankAllMandatoryAddrLinesMessageKey: String,
                 invalidAddressLineMessageKey:String,
                 blankPostcodeMessageKey:String, invalidPostcodeMessageKey: String) = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]) = {
      val errors = new scala.collection.mutable.ListBuffer[FormError]()
      val addr = getAddrDetails(data, key, addr2Key, addr3Key, addr4Key, postcodeKey, countryCodeKey)
      if (blankAllMandatoryAddrLinesMessageKey.length > 0 &&
        addr._1.length==0 && addr._2.length==0) {
        errors += FormError(key, blankAllMandatoryAddrLinesMessageKey)
        errors += FormError(addr2Key, "")
      } else {
        validateMandatoryAddressLine(key, addr._1,
          getProperty("validationMaxLengthAddresslines").trim.toInt, blankMandatoryAddrLineMessageKey,
          invalidAddressLineMessageKey, errors)
        validateMandatoryAddressLine(addr2Key, addr._2,
          getProperty("validationMaxLengthAddresslines").trim.toInt, blankMandatoryAddrLineMessageKey,
          invalidAddressLineMessageKey, errors)
        validateOptionalAddressLine(addr3Key, addr._3,
          getProperty("validationMaxLengthAddresslines").trim.toInt, invalidAddressLineMessageKey, errors)
        validateOptionalAddressLine(addr4Key, addr._4,
          getProperty("validationMaxLengthAddresslines").trim.toInt, invalidAddressLineMessageKey, errors)
      }
      if (addr._6.length==0 || addr._6 == getProperty("ukIsoCountryCode")) {
        validatePostcode(addr._5, postcodeKey, blankPostcodeMessageKey, invalidPostcodeMessageKey, errors)
      }
      if (errors.isEmpty) {
        Right(addr._1)
      } else {
        Left(errors.toList)
      }
    }

    override def unbind(key: String, value: String): Map[String, String] = {
      Map(key -> value.toString)
    }
  }

  def address( addr2Key: String, addr3Key:String, addr4Key:String,
               postcodeKey:String, countryCodeKey: String,
               blankMandatoryAddrLineMessageKey: String, blankAllMandatoryAddrLinesMessageKey: String,
               invalidAddressLineMessageKey:String,
               blankPostcodeMessageKey:String, invalidPostcodeMessageKey: String) =
    Forms.of(addressFormatter(addr2Key, addr3Key, addr4Key, postcodeKey, countryCodeKey,
      blankMandatoryAddrLineMessageKey, blankAllMandatoryAddrLinesMessageKey, invalidAddressLineMessageKey,
      blankPostcodeMessageKey, invalidPostcodeMessageKey))

  private def cleanMoneyString(moneyString: String) =
    currencyRegex.findFirstIn(moneyString.replace(",","")).getOrElse("")

  private def optionalCurrencyFormatter(invalidFormatMessageKey: String) = new Formatter[Option[BigDecimal]] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[BigDecimal]] = {
      data.get(key) match {
        case Some(num) => {
          num.trim match{
            case "" => Right(None)
            case numTrimmed => {
              try {
                val bigDecimalMoney = BigDecimal(cleanMoneyString(numTrimmed))
                Right(Some(bigDecimalMoney))
              } catch {
                case e: NumberFormatException => Left(Seq(FormError(key, invalidFormatMessageKey)))
              }
            }
          }
        }
        case _ => Left(Seq(FormError(key, invalidFormatMessageKey)))
      }
    }
    override def unbind(key: String, value: Option[BigDecimal]): Map[String, String] = Map(key -> value.getOrElse("").toString)
  }

  def optionalCurrency(invalidFormatMessageKey: String) = Forms.of[Option[BigDecimal]](optionalCurrencyFormatter(invalidFormatMessageKey))


  def mandatoryAccountNumber(emptyMessageKey: String, invalidMessageKey: String): Mapping[String] = {
    val constraint = Constraint("Blank and invalid")( {
      t: String => t match {
        case "" => Invalid(emptyMessageKey)
        case x if !x.matches(accountNumberRegex.regex) => Invalid(invalidMessageKey)
        case _ => Valid
      }
    } )
    text.verifying(constraint)
  }

  def mandatorySortCode(emptyMessageKey: String, invalidMessageKey: String): Mapping[String] = {
    val constraint = Constraint("Blank and invalid")( {
      t: String => t match {
        case "" => Invalid(emptyMessageKey)
        case x if !x.matches(sortCodeRegex.regex) => Invalid(invalidMessageKey)
        case _ => Valid
      }
    } )
    text.verifying(constraint)
  }

  private def mandatoryIbanFormatter(blankValueMessageKey: String, invalidValueMessageKey: String) = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(n) =>
          val s = n.trim.replaceAll(" ", "")
          s match{
            case p if p.length==0 => Left(Seq(FormError(key, blankValueMessageKey)))
            case num => {
              ibanRegex.findFirstIn(num) match {
                case None => Left(Seq(FormError(key, invalidValueMessageKey)))
                case _ => Right(num)
              }
            }
          }
        case _ => Left(Seq(FormError(key, "Nothing to validate")))
      }
    }
    override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)
  }

  def mandatoryIban(blankValueMessageKey: String, invalidValueMessageKey: String) =
    Forms.of[String](mandatoryIbanFormatter(blankValueMessageKey, invalidValueMessageKey))

}

object FormValidator extends FormValidator