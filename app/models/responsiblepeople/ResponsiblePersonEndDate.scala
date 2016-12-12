package models.responsiblepeople

import models.FormTypes._
import org.joda.time.{DateTime, DateTimeFieldType, LocalDate}
import play.api.data.mapping.forms._
import play.api.data.mapping._
import play.api.data.validation.{Invalid, ValidationError}
import play.api.libs.json.Json

case class ResponsiblePersonEndDate(endDate: LocalDate)

object ResponsiblePersonEndDate {

  implicit val format = Json.format[ResponsiblePersonEndDate]

  implicit val formRule: Rule[UrlFormEncoded, ResponsiblePersonEndDate] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    __.read(peopleEndDateRule) fmap ResponsiblePersonEndDate.apply
//    localDateRule.validate(form) map { r =>
//
//    }
//
//    val endDate = LocalDate.parse(form.get("endDate").get.head)
//    val positionStartDate = LocalDate.parse(form.get("positionStartDate").get.head)
//
//    if (endDate.isAfter(LocalDate.now()))
//      Failure(Seq(Path -> Seq(ValidationError("not valid"))))
//    else if (endDate.isBefore(positionStartDate) || endDate.isEqual(positionStartDate)) {
//      Failure(Seq(Path -> Seq(ValidationError("error.expected.future.date.after.start"))))
//    } else {
//      Success(ResponsiblePersonEndDate(endDate))
//    }


    //    val x = (__.read(readReleativeDate())) ~


    //fmap ResponsiblePersonEndDate.apply
    //    import play.api.data.mapping.forms.Rules._
    //
    //    val x = ((__ \ "endDate").read(peopleEndDateRule compose localDateFutureRule) ~
    //      (__ \ "positionStartDate").read(localDateFutureRule))((a,b) => a)


    //fmap ResponsiblePersonEndDate.apply
  }

  implicit val formWrites: Write[ResponsiblePersonEndDate, UrlFormEncoded] =
    Write {
      case ResponsiblePersonEndDate(b) => Map(
        "endDate.day" -> Seq(b.get(DateTimeFieldType.dayOfMonth()).toString),
        "endDate.month" -> Seq(b.get(DateTimeFieldType.monthOfYear()).toString),
        "endDate.year" -> Seq(b.get(DateTimeFieldType.year()).toString)
      )
    }
}
