package models.aboutthebusiness

import models.tradingpremises.ActivityEndDate
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.Success
import play.api.libs.json.Json


class DateOfChangeSpec extends PlaySpec {
  "DateOfChange" must {

    "read the form correctly when given a valid date" in {
      val model =    Map (
        "dateOfChange.day" -> Seq("24"),
        "dateOfChange.month" -> Seq("2"),
        "dateOfChange.year" -> Seq("1990")
      )

      // scalastyle:off
      DateOfChange.formRule.validate(model) must be (Success(DateOfChange(new LocalDate(1990, 2, 24))))

    }

    "read from JSON correctly" in {

      val json = Json.obj(
        "dateOfChange" -> Json.obj(
          "day" -> 24,
          "month" -> 2,
          "year" -> 2016
        )
      )

      val result = Json.fromJson[DateOfChange](json)
      result.get.dateOfChange must be(new LocalDate(2016,2,24))
    }

    "write to JSON correctly" in {

      val date = DateOfChange(new LocalDate(2016,2,24))
      val json = Json.obj(
        "dateOfChange" -> Json.obj(
          "day" -> 24,
          "month" -> 2,
          "year" -> 2016
        )
      )

      val result = Json.toJson(date)
      result must be(json)
    }
  }
}
