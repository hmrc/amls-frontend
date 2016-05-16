package models.aboutthebusiness

import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}


class ActivityStartDateSpec extends PlaySpec {
  "ActivityStartDate" must {

    "Form" must {

      "read successfully" in {
        val model =  Map (
            "startDate.day" -> Seq("24"),
            "startDate.month" -> Seq("2"),
            "startDate.year" -> Seq("1990")
        )
        // scalastyle:off
        ActivityStartDate.formRule.validate(model) must be (Success(ActivityStartDate(new LocalDate(1990, 2, 24))))

      }

      "throw error message when data entered is invalid" in {
        val model =  Map (
          "startDate.day" -> Seq("2466"),
          "startDate.month" -> Seq("2"),
          "startDate.year" -> Seq("1990")
        )
        ActivityStartDate.formRule.validate(model) must be(Failure(Seq(Path \ "startDate" -> Seq(
          ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))

      }

      "throw error message when data entered is empty" in {
        val model =  Map (
          "startDate.day" -> Seq(""),
          "startDate.month" -> Seq(""),
          "startDate.year" -> Seq("")
        )
        ActivityStartDate.formRule.validate(model) must be(Failure(Seq(Path \ "startDate" -> Seq(
          ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))

      }

      "successfully write the model" in {

        ActivityStartDate.formWrites.writes(ActivityStartDate(new LocalDate(1990, 2, 24)))  mustBe Map(
          "startDate.day" -> Seq("24"),
          "startDate.month" -> Seq("2"),
          "startDate.year" -> Seq("1990")
        )
      }
    }

    "Json" should {
      "Read and write successfully" in {
        ActivityStartDate.format.reads(ActivityStartDate.format.writes(ActivityStartDate(new LocalDate(1990, 2, 24)))) must be(
          JsSuccess(ActivityStartDate(new LocalDate(1990, 2, 24)), JsPath \ "startDate"))

      }

    }


  }

}
