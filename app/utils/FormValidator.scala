package utils

import org.joda.time.LocalDate
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.Mapping
import config.AmlsPropertiesReader._
import play.api.data.FormError


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

//  def containsValidPostCodeCharacters(value: String): Boolean =
//    postCodeFormat.r.findFirstIn(value).isDefined

  //  def amlsIsPostcodeLengthValid(value: String) = {
  //    value.length <=getProperty("validationMaxLengthPostcode").trim.toInt && isPostcodeLengthValid(value)
  //  }

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
}

object FormValidator extends FormValidator