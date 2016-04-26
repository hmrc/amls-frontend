package models.responsiblepeople

import org.joda.time.LocalDate
import play.api.data.mapping._
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json.Json
import utils.DateHelper

case class PreviousName(
                       firstName: Option[String],
                       middleName: Option[String],
                       lastName: Option[String],
                       date: LocalDate
                       ) {

  val formattedDate = DateHelper.formatDate(date)

  private def nameFormatter(f: String, m: String, l: String) = Seq(f, m, l).mkString(" ")

  def formattedPreviousName(that: PersonName) = this match {
    case PreviousName(Some(f), None, None, _) => nameFormatter(f, that.middleName.getOrElse(""), that.lastName)
    case PreviousName(None, Some(m), None, _) => nameFormatter(that.firstName, m, that.lastName)
    case PreviousName(None, None, Some(l), _) => nameFormatter(that.firstName, that.middleName.getOrElse(""), l)
    case PreviousName(None, Some(m), Some(l), _) => nameFormatter(that.firstName, m, l)
    case PreviousName(Some(f), None, Some(l), _) => nameFormatter(f, that.middleName.getOrElse(""), l)
    case PreviousName(Some(f), Some(m), None, _) => nameFormatter(f, m, that.lastName)
    case PreviousName(Some(f), Some(m), Some(l), _) => nameFormatter(f, m, l)
  }

}

object PreviousName {

  import models.FormTypes._

  implicit val formR: Rule[UrlFormEncoded, PreviousName] =
    From[UrlFormEncoded] { __ =>

      import play.api.data.mapping.forms.Rules._

      type I = (Option[String], Option[String], Option[String])

      val iR = Rule[I, I] {
        case names @ (first, middle, last) if names.productIterator.collectFirst {
          case Some(_) => true
        }.isDefined =>
          Success(names)
        case _ =>
          Failure(Seq(Path -> Seq(ValidationError("error.rp.previous.invalid"))))
      }

      // Defining this here because it helps out the compiler with typechecking
      val builder: (I, LocalDate) => PreviousName = {
        case ((first, middle, last), date) =>
          PreviousName(first, middle, last, date)
      }

      (((
        (__ \ "firstName").read(optionR(firstNameType)) ~
        (__ \ "middleName").read(optionR(middleNameType)) ~
        (__ \ "lastName").read(optionR(lastNameType))
      ).tupled compose iR) ~ (
        (__ \ "date").read(localDateRule)
      ))(builder)
    }

  implicit val formW: Write[PreviousName, UrlFormEncoded] =
    To[UrlFormEncoded] { __ =>

      import play.api.data.mapping.forms.Writes._
      import play.api.libs.functional.syntax._

      (
        (__ \ "firstName").write[Option[String]] ~
        (__ \ "middleName").write[Option[String]] ~
        (__ \ "lastName").write[Option[String]] ~
        (__ \ "date").write(localDateWrite)
      )(unlift(PreviousName.unapply))
    }

  implicit val format = Json.format[PreviousName]
}
