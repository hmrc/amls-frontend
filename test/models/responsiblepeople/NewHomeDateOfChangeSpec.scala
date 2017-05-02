package models.responsiblepeople

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, _}


class NewHomeDateOfChangeSpec extends PlaySpec {
  "NewHomeDateOfChange" must {

    "read the form correctly when given a valid date" in {
      val model =    Map (
        "dateOfChange.day" -> Seq("24"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("1990")
      )

      NewHomeDateOfChange.formRule.validate(model) must be (Valid(NewHomeDateOfChange(new LocalDate(1990, 2, 24))))

    }

    "fail form validation when given a future date" in {
      val model =    Map (
        "dateOfChange.day" -> Seq("24"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq(LocalDate.now().plusYears(1).getYear.toString)
      )

      NewHomeDateOfChange.formRule.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("error.future.date")))
        ))

    }

    "fail form validation when given a date before a business activities start date" in {
      val model = Map (
        "dateOfChange.day" -> Seq("24"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("2012"),
        "activityStartDate" -> Seq("2016-05-25")
      )

      NewHomeDateOfChange.formRule.validate(model) must be(
        Invalid(
          Seq(
            Path \ "dateOfChange" -> Seq(ValidationError("error.expected.dateofchange.date.after.activitystartdate", "25-05-2016")))
        ))
    }

    "Read and write successfully" in {
      NewHomeDateOfChange.format.reads(NewHomeDateOfChange.format.writes(NewHomeDateOfChange(new LocalDate(1990, 2, 24)))) must be(
        JsSuccess(NewHomeDateOfChange(new LocalDate(1990, 2, 24)), JsPath \ "dateOfChange"))

    }

    "write successfully" in {
      NewHomeDateOfChange.format.writes(NewHomeDateOfChange(new LocalDate(1990, 2, 24))) must be(Json.obj("dateOfChange" ->"1990-02-24"))
    }
  }
}
