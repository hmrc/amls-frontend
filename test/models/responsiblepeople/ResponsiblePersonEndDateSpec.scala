package models.tradingpremises

import models.responsiblepeople.ResponsiblePersonEndDate
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}


class ResponsiblePersonEndDateSpec extends PlaySpec {

  "ResponsiblePersonEndDate" must {
    "Form" must {
      "read successfully" in {
        val model =  Map (
            "endDate.day" -> Seq("24"),
            "endDate.month" -> Seq("2"),
            "endDate.year" -> Seq("1990")
        )
        // scalastyle:off
        ResponsiblePersonEndDate.formRule.validate(model) must be (Success(ResponsiblePersonEndDate(new LocalDate(1990, 2, 24))))

      }

      "throw error message when data entered is invalid" in {
        val model =  Map (
          "endDate.day" -> Seq("2466"),
          "endDate.month" -> Seq("2"),
          "endDate.year" -> Seq("1990")
        )
        ResponsiblePersonEndDate.formRule.validate(model) must be(Failure(Seq(Path \ "endDate" -> Seq(
          ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))

      }

      "throw error message when data entered is empty" in {
        val model =  Map (
          "endDate.day" -> Seq(""),
          "endDate.month" -> Seq(""),
          "endDate.year" -> Seq("")
        )
        ResponsiblePersonEndDate.formRule.validate(model) must be(Failure(Seq(Path \ "endDate" -> Seq(
          ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))

      }

      "successfully write the model" in {

        ResponsiblePersonEndDate.formWrites.writes(ResponsiblePersonEndDate(new LocalDate(1990, 2, 24)))  mustBe Map(
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("2"),
          "endDate.year" -> Seq("1990")
        )
      }
    }

    "Json" should {

      "Read and write successfully" in {

        ResponsiblePersonEndDate.format.reads(ResponsiblePersonEndDate.format.writes(ResponsiblePersonEndDate(new LocalDate(1990, 2, 24)))) must be(
          JsSuccess(ResponsiblePersonEndDate(new LocalDate(1990, 2, 24)), JsPath \ "endDate"))

      }

      "write successfully" in {
        ResponsiblePersonEndDate.format.writes(ResponsiblePersonEndDate(new LocalDate(1990, 2, 24))) must be(Json.obj("endDate" ->"1990-02-24"))
      }
    }
  }
 }
