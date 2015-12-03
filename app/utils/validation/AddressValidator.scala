package utils.validation

import play.api.data.{Forms, FormError}
import play.api.data.format.Formatter
import scala.collection.mutable.ListBuffer
import uk.gov.hmrc.play.validators.Validators.isPostcodeLengthValid

object AddressValidator extends FormValidator {

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
                               blankPostcodeMessageKey:String,
                               invalidPostcodeMessageKey: String,
                               maxLengthAddressLines:Int,
                               ukISOCountryCode:String
                                ) = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]) = {
      val errors = new scala.collection.mutable.ListBuffer[FormError]()
      val addr = getAddrDetails(data, key, addr2Key, addr3Key, addr4Key, postcodeKey, countryCodeKey)
      if (blankAllMandatoryAddrLinesMessageKey.length > 0 &&
        addr._1.length==0 && addr._2.length==0) {
        errors += FormError(key, blankAllMandatoryAddrLinesMessageKey)
        errors += FormError(addr2Key, "")
      } else {

        validateMandatoryAddressLine(key, addr._1,
          maxLengthAddressLines, blankMandatoryAddrLineMessageKey,
          invalidAddressLineMessageKey, errors)
        validateMandatoryAddressLine(addr2Key, addr._2,
          maxLengthAddressLines, blankMandatoryAddrLineMessageKey,
          invalidAddressLineMessageKey, errors)
        validateOptionalAddressLine(addr3Key, addr._3,
          maxLengthAddressLines, invalidAddressLineMessageKey, errors)
        validateOptionalAddressLine(addr4Key, addr._4,
          maxLengthAddressLines, invalidAddressLineMessageKey, errors)
      }
      if (addr._6.length==0 || addr._6 == ukISOCountryCode) {
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
               blankPostcodeMessageKey:String, invalidPostcodeMessageKey: String,
               maxLengthAddressLines:Int,
               ukISOCountryCode:String) =
    Forms.of(addressFormatter(addr2Key, addr3Key, addr4Key, postcodeKey, countryCodeKey,
      blankMandatoryAddrLineMessageKey, blankAllMandatoryAddrLinesMessageKey, invalidAddressLineMessageKey,
      blankPostcodeMessageKey, invalidPostcodeMessageKey, maxLengthAddressLines, ukISOCountryCode))




}
