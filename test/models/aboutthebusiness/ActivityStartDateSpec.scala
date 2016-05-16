package models.aboutthebusiness

import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.Success


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

      "successfully write the model" in {

        ActivityStartDate.formWrites.writes(ActivityStartDate(new LocalDate(1990, 2, 24)))  mustBe Map(
          "startDate.day" -> Seq("24"),
          "startDate.month" -> Seq("2"),
          "startDate.year" -> Seq("1990")
        )
      }
    }
  }

}
