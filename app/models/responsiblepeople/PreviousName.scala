package models.responsiblepeople

import org.joda.time.LocalDate
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json.Json
import utils.DateHelper

case class PreviousName(
                       firstName: Option[String],
                       middleName: Option[String],
                       lastName: Option[String],
                       date: LocalDate
                       ) {

  val formattedDate = DateHelper.formatDate(date)

  def formattedPreviousName(that: PersonName): String = that match {
      case PersonName(first, middle, last, _, _) =>
        Seq(
          firstName orElse Some(first),
          middleName orElse middle,
          lastName orElse Some(last)
        ).flatten[String].mkString(" ")
  }

}

object PreviousName {

  import models.FormTypes._

  implicit val formR: Rule[UrlFormEncoded, PreviousName] =
    From[UrlFormEncoded] { __ =>

      import jto.validation.forms.Rules._

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

      import jto.validation.forms.Writes._
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
