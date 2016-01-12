package models.aboutyou

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError

class YourDetailsSpec extends PlaySpec with MockitoSugar {

  "Form Details" must {

    "successfully validate given a first name and last name" in {

      val data = Map(
        "firstName" -> Seq("foo"),
        "lastName" -> Seq("bar")
      )

      YourDetails.formRule.validate(data) must
        be(Success(YourDetails("foo", None, "bar")))
    }

    "successfully validate given a first name, middle name and last name" in {

      val data = Map(
        "firstName" -> Seq("foo"),
        "middleName" -> Seq("bar"),
        "lastName" -> Seq("baz")
      )

      YourDetails.formRule.validate(data) must
        be(Success(YourDetails("foo", Some("bar"), "baz")))
    }

    "fail to validate when given invalid data" in {

      YourDetails.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "firstName") -> Seq(ValidationError("error.required")),
          (Path \ "lastName") -> Seq(ValidationError("error.required"))
        )))
    }

    "write correct data" in {

      val model = YourDetails("foo", Some("bar"), "baz")

      YourDetails.formWrites.writes(model) must
        be(Map(
          "firstName" -> Seq("foo"),
          "middleName" -> Seq("bar"),
          "lastName" -> Seq("baz")
        ))
    }
  }
}
