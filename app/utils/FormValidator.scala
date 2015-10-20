package utils

import org.joda.time.LocalDate
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.Mapping
import config.AmlsPropertiesReader._
import play.api.data.FormError
import uk.gov.hmrc.play.validators.Validators.isPostcodeLengthValid

trait FormValidator {

  import play.api.data.Forms
  import play.api.data.format.Formatter

  val ninoRegex = """^$|^[A-Z,a-z]{2}[0-9]{6}[A-D,a-d]{1}$"""
  val emailFormat = """(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"""
  val postCodeFormat = "(([gG][iI][rR] {0,}0[aA]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) {0,}[0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))"
  val phoneNoFormat = "^[A-Z0-9 \\)\\/\\(\\-\\*#]{1,27}$"

  def stopOnFirstFail[T](constraints: Constraint[T]*) = Constraint { field: T =>
    constraints.toList dropWhile (_(field) == Valid) match {
      case Nil => Valid
      case constraint :: _ => constraint(field)
    }
  }

  def mandatoryText(blankValueMessageKey:String, invalidLengthMessageKey: String, validationMaxLengthProperty: String): Mapping[String] = {
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

  def mandatoryNino(blankValueMessageKey: String, invalidLengthMessageKey: String, invalidValueMessageKey: String) : Mapping[String] = {
    val blankConstraint = Constraint("Blank")( {
      t:String => t match {
        case t if t.length == 0 => Invalid(blankValueMessageKey)
        case t if t.replaceAll("\\s", "").length > getProperty("validationMaxLengthNINO").toInt =>
          Invalid(invalidLengthMessageKey)
        case _ => Valid
      }
    } )

    val valueConstraint = Constraint("Value")( {
      t:String => t match {
        case t if t.replaceAll("\\s", "").matches(ninoRegex) => Valid
        case _ => Invalid(invalidValueMessageKey)
      }
    } )

    text.verifying(stopOnFirstFail(blankConstraint, valueConstraint))
  }

  def mandatoryEmailWithDomain(blankValueMessageKey: String, invalidLengthMessageKey: String, invalidValueMessageKey: String) : Mapping[String] = {
    val blankConstraint = Constraint("Blank")( {
      t:String => t match {
        case t if t.length == 0 => Invalid(blankValueMessageKey)
        case t if t.length > getProperty("validationMaxLengthEmail").toInt => Invalid(invalidLengthMessageKey)
        case _ => Valid
      }
    } )
    text.verifying(stopOnFirstFail(blankConstraint, emailWithDomain(invalidValueMessageKey)))
  }

  def emailWithDomain(errorMessageKeyInvalidFormat:String = "error.email") =
    Constraints.pattern(emailFormat.r, "constraint.email", errorMessageKeyInvalidFormat)

  def mandatoryPhoneNumber( blankValueMessageKey: String,
                            invalidLengthMessageKey: String,
                            invalidValueMessageKey: String) =
    Forms.of[String](mandatoryPhoneNumberFormatter(blankValueMessageKey, invalidLengthMessageKey, invalidValueMessageKey))

  def mandatoryPhoneNumberFormatter(blankValueMessageKey: String,
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
              } else if (!validatePhoneNumber(num)) {
                import play.api.data.FormError
                Left(Seq(FormError(key, invalidValueMessageKey)))
              } else {
                Right(num)
              }
            }
          }

        case _ => Left(Seq(FormError(key, "Nothing to validate")))
      }
    }
    override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)
  }

  private def validatePhoneNumber = {
    s: String => phoneNoFormat.r.findFirstIn(s) match {
      case Some(x) => true
      case None => false
    }
  }

  def isNotFutureDate = {
    date: LocalDate => !date.isAfter(LocalDate.now())
  }


  private def getAddrDetails(data: Map[String, String],
                             addr1Key: String,
                             addr2Key: String,
                             addr3Key:String,
                             addr4Key:String,
                             postcodeKey:String,
                             countryCodeKey: String) = {
    (data.getOrElse(addr1Key, ""),
      data.getOrElse(addr2Key, ""),
      data.getOrElse(addr3Key, ""),
      data.getOrElse(addr4Key, ""),
      data.getOrElse(postcodeKey, ""),
      data.getOrElse(countryCodeKey, ""))
  }

  private def validateOptionalAddressLine(addrKey:String, addr:String, maxLength:Int,
                                          invalidAddressLineMessageKey:String,
                                          errors: scala.collection.mutable.ListBuffer[FormError] ): Unit = {
    addr match {
      case a if a.length > maxLength => errors += FormError(addrKey, invalidAddressLineMessageKey)
      case _ => {}
    }
  }

  private def containsValidPostCodeCharacters(value: String): Boolean =
    !postCodeFormat.r.findFirstIn(value).isEmpty

  private def ihtIsPostcodeLengthValid(value: String) = {
    value.length <=getProperty("validationMaxLengthPostcode").trim.toInt && isPostcodeLengthValid(value)
  }

  private def validateMandatoryAddressLine(addrKey:String, addr:String, maxLength:Int,
                                           blankMessageKey:String,
                                           invalidAddressLineMessageKey: String,
                                           errors: scala.collection.mutable.ListBuffer[FormError]): Unit = {
    addr match {
      case a if a.length == 0 => errors += FormError(addrKey, blankMessageKey)
      case a if a.length > maxLength => errors += FormError(addrKey, invalidAddressLineMessageKey)
      case _ => {}
    }
  }

  private def validatePostcode(postcode:String, postcodeKey: String, blankPostcodeMessageKey: String,
                               invalidPostcodeMessageKey: String,
                               errors: scala.collection.mutable.ListBuffer[FormError]) = {
    postcode match {
      case a if a.length == 0 => errors += FormError(postcodeKey, blankPostcodeMessageKey)
      case a if a.length > 0 && !containsValidPostCodeCharacters(a) =>
        errors +=FormError(postcodeKey, invalidPostcodeMessageKey)
      case a if a.length > 0 && !ihtIsPostcodeLengthValid(a) =>
        errors += FormError(postcodeKey, invalidPostcodeMessageKey)
      case _ => {}
    }
  }

  def addressFormatter(addr2Key: String, addr3Key:String, addr4Key:String,
                 postcodeKey:String, countryCodeKey: String, allLinesBlankMessageKey:String,
                 blankFirstTwoAddrLinesMessageKey: String, invalidAddressLineMessageKey:String,
                 blankPostcodeMessageKey:String, invalidPostcodeMessageKey: String,
                 blankCountryCode: String,
                 blankBothFirstTwoAddrLinesMessageKey: Option[String] = None) = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]) = {
      val errors = new scala.collection.mutable.ListBuffer[FormError]()
      val addr = getAddrDetails(data, key, addr2Key, addr3Key, addr4Key, postcodeKey, countryCodeKey)

      if (addr._1.length == 0 && addr._2.length == 0) {
        errors += FormError(key, allLinesBlankMessageKey)
        errors += FormError(addr2Key, "")
      } else if (blankBothFirstTwoAddrLinesMessageKey.isDefined &&
        addr._1.length==0 && addr._2.length==0) {
        errors += FormError(key, blankBothFirstTwoAddrLinesMessageKey.getOrElse(""))
        errors += FormError(addr2Key, "")
      } else {
        validateMandatoryAddressLine(key, addr._1,
          getProperty("validationMaxLengthAddresslines").trim.toInt, blankFirstTwoAddrLinesMessageKey,
          invalidAddressLineMessageKey, errors)
        validateMandatoryAddressLine(addr2Key, addr._2,
          getProperty("validationMaxLengthAddresslines").trim.toInt, blankFirstTwoAddrLinesMessageKey,
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
               postcodeKey:String, countryCodeKey: String, allLinesBlankMessageKey:String,
               blankFirstTwoAddrLinesMessageKey: String, invalidAddressLineMessageKey:String,
               blankPostcodeMessageKey:String, invalidPostcodeMessageKey: String,
               blankCountryCode: String,
               blankBothFirstTwoAddrLinesMessageKey: Option[String]) =
    Forms.of(addressFormatter(addr2Key, addr3Key, addr4Key, postcodeKey, countryCodeKey,
      allLinesBlankMessageKey, blankFirstTwoAddrLinesMessageKey, invalidAddressLineMessageKey,
      blankPostcodeMessageKey, invalidPostcodeMessageKey, blankCountryCode, blankBothFirstTwoAddrLinesMessageKey))
}

object FormValidator extends FormValidator