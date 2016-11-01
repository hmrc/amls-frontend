package models.responsiblepeople

import models.responsiblepeople.ResponsiblePersonEndDate
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}


class ResponsiblePersonEndDateSpec extends PlaySpec {

  val validYear = 1990
  val validDay = 24
  val validMonth = 2

  "ResponsiblePersonEndDate Form" must {
    "successfully read the model" in {
      val validModel = Map(
        "endDate.day" -> Seq("24"),
        "endDate.month" -> Seq("2"),
        "endDate.year" -> Seq("1990")
      )

      ResponsiblePersonEndDate.formRule.validate(validModel) must be(
        Success(ResponsiblePersonEndDate(new LocalDate(validYear, validMonth, validDay))))
    }

    "successfully write the model" in {
      ResponsiblePersonEndDate.formWrites.writes(ResponsiblePersonEndDate(new LocalDate(validYear, validMonth, validDay))) must be(
        Map(
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("2"),
          "endDate.year" -> Seq("1990")
        ))
    }

    "throw error message" when {
      "day entered is invalid" in {
        val errorDayModel = Map(
          "endDate.day" -> Seq("2466"),
          "endDate.month" -> Seq("2"),
          "endDate.year" -> Seq("1990")
        )

        ResponsiblePersonEndDate.formRule.validate(errorDayModel) must be(
          Failure(Seq(Path \ "endDate" -> Seq(ValidationError("error.invalid.tp.date")))))
      }

      "month entered is invalid" in {
        val errorDayModel = Map(
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("29"),
          "endDate.year" -> Seq("1990")
        )

        ResponsiblePersonEndDate.formRule.validate(errorDayModel) must be(
          Failure(Seq(Path \ "endDate" -> Seq(ValidationError("error.invalid.tp.month")))))
      }

      "year entered is too long" in {
        val errorDayModel = Map(
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("11"),
          "endDate.year" -> Seq("199000")
        )

        ResponsiblePersonEndDate.formRule.validate(errorDayModel) must be(
          Failure(Seq(Path \ "endDate" -> Seq(ValidationError("error.invalid.tp.year")))))
      }

      "year entered is too short" in {
        val errorDayModel = Map(
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("11"),
          "endDate.year" -> Seq("16")
        )

        ResponsiblePersonEndDate.formRule.validate(errorDayModel) must be(
          Failure(Seq(Path \ "endDate" -> Seq(ValidationError("error.invalid.tp.year")))))
      }

      "all fields are empty" in {
        val noContentModel = Map(
          "endDate.day" -> Seq(""),
          "endDate.month" -> Seq(""),
          "endDate.year" -> Seq("")
        )

        ResponsiblePersonEndDate.formRule.validate(noContentModel) must be(
          Failure(Seq(Path \ "endDate" -> Seq(ValidationError("error.required.tp.year")),
          Path \ "endDate" -> Seq(ValidationError("error.required.tp.month")),
          Path \ "endDate" -> Seq(ValidationError("error.required.tp.date")))))
      }
    }
  }

  "ResponsiblePersonEndDate Json" must {

    "Read and write successfully" in {

      ResponsiblePersonEndDate.format.reads(ResponsiblePersonEndDate.format.writes(ResponsiblePersonEndDate(new LocalDate(1990, 2, 24)))) must be(
        JsSuccess(ResponsiblePersonEndDate(new LocalDate(1990, 2, 24)), JsPath \ "endDate"))

    }

    "write successfully" in {
      ResponsiblePersonEndDate.format.writes(ResponsiblePersonEndDate(new LocalDate(1990, 2, 24))) must be(Json.obj("endDate" -> "1990-02-24"))
    }
  }

}
