package utils

import org.joda.time.LocalDate
import play.api.data.Forms._
import play.api.data.validation._
import uk.gov.hmrc.play.validators.Validators._
import play.api.data.Mapping
import config.AmlsPropertiesReader._



trait FormValidator {
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

  def getProperty(key: String) = propertyResource.getString(key).trim

//  def containsValidPostCodeCharacters(value: String): Boolean =
//    postCodeFormat.r.findFirstIn(value).isDefined

  //  def amlsIsPostcodeLengthValid(value: String) = {
  //    value.length <=getProperty("validationMaxLengthPostcode").trim.toInt && isPostcodeLengthValid(value)
  //  }

  def validateNinoFormat = {
    nino: String => {
      val ninoMinusSpaces = nino.replaceAll("\\s", "")
      ninoMinusSpaces.length <= getProperty("validationMaxLengthNINO").toInt && ninoMinusSpaces.matches(ninoRegex)
    }
  }

  def amlsMandatoryEmailWithDomain(blankValueMessageKey: String, invalidLengthMessageKey: String, invalidValueMessageKey: String) : Mapping[String] = {
    val blankConstraint = Constraint("Blank")( {
      t:String => t match {
        case t if t.length == 0 => Invalid(blankValueMessageKey)
        case t if t.length > getProperty("validationMaxLengthEmail").toInt => Invalid(invalidLengthMessageKey)
        case _ => Valid
      }
    } )
    text.verifying(stopOnFirstFail(blankConstraint, amlsEmailWithDomain(invalidValueMessageKey)))
  }

  def amlsEmailWithDomain(errorMessageKeyInvalidFormat:String = "error.email") =
    Constraints.pattern(emailFormat.r, "constraint.email", errorMessageKeyInvalidFormat)

}

object FormValidator extends FormValidator