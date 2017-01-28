package models.supervision

import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Path, Invalid, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class AnotherBodySpec extends PlaySpec with MockitoSugar {

  "Form Rules and Writes" must {

    "successfully validate given no selected" in {
      val urlFormEncoded = Map("anotherBody" -> Seq("false"))
      val expected = Valid(AnotherBodyNo)
      AnotherBody.formRule.validate(urlFormEncoded) must be(expected)
    }

    "successfully validate given yes selected with valid data" in {

      val urlFormEncoded = Map(
        "anotherBody" -> Seq("true"),
        "supervisorName" -> Seq("Name"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("2"),
        "startDate.year" -> Seq("1990"),
        "endDate.day" -> Seq("24"),
        "endDate.month" -> Seq("2"),
        "endDate.year" -> Seq("1998"),
        "endingReason" -> Seq("Reason")
      )

      val start = new LocalDate(1990, 2, 24) //scalastyle:off magic.number
      val end = new LocalDate(1998, 2, 24)   //scalastyle:off magic.number
      val expected = Valid(AnotherBodyYes("Name", start, end, "Reason"))

      AnotherBody.formRule.validate(urlFormEncoded) must be(expected)
    }

    "successfully write No" in {
      val expected = Map("anotherBody" -> Seq("false"))
      AnotherBody.formWrites.writes(AnotherBodyNo) must be(expected)
    }

    "successfully write Yes" in {

      val expected = Map(
        "anotherBody" -> Seq("true"),
        "supervisorName" -> Seq("Name"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("2"),
        "startDate.year" -> Seq("1990"),
        "endDate.day" -> Seq("24"),
        "endDate.month" -> Seq("2"),
        "endDate.year" -> Seq("1998"),
        "endingReason" -> Seq("Reason")
      )

      val start = new LocalDate(1990, 2, 24) //scalastyle:off magic.number
      val end = new LocalDate(1998, 2, 24)   //scalastyle:off magic.number
      val input = AnotherBodyYes("Name", start, end, "Reason")

      AnotherBody.formWrites.writes(input) must be(expected)
    }

    "show an error with missing values when Yes selected" in {
      val urlFormEncoded = Map("anotherBody" -> Seq("true"))
      val expected = Invalid(
        Seq((Path \ "supervisorName") -> Seq(ValidationError("error.required")),
        (Path \ "startDate") -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")),
        (Path \ "endDate") -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")),
        (Path \ "endingReason") -> Seq(ValidationError("error.required")))
      )
      AnotherBody.formRule.validate(urlFormEncoded) must be(expected)
    }

  }


  "Json read and writes" must {

    "Serialise AnotherBodyNo as expected" in {
      Json.toJson(AnotherBodyNo) must be(Json.obj("anotherBody" -> false))
    }

    "Serialise AnotherBodyYes service as expected" in {

      val start = new LocalDate(1990, 2, 24) //scalastyle:off magic.number
      val end = new LocalDate(1998, 2, 24)   //scalastyle:off magic.number
      val input = AnotherBodyYes("Name", start, end, "Reason")

      val expectedJson = Json.obj(
        "anotherBody" -> true,
        "supervisorName" -> "Name",
        "startDate" -> "1990-02-24",
        "endDate" -> "1998-02-24",
        "endingReason" -> "Reason"
      )

      Json.toJson(input) must be(expectedJson)
    }

    "Deserialise AnotherBodyNo as expected" in {
      val json = Json.obj("anotherBody" -> false)
      val expected = JsSuccess(AnotherBodyNo, JsPath)
      Json.fromJson[AnotherBody](json) must be (expected)
    }

    "Deserialise AnotherBodyYes as expected" in {

      val input = Json.obj(
        "anotherBody" -> true,
        "supervisorName" -> "Name",
        "startDate" -> "1990-02-24",
        "endDate" -> "1998-02-24",
        "endingReason" -> "Reason"
      )

      val start = new LocalDate(1990, 2, 24) //scalastyle:off magic.number
      val end = new LocalDate(1998, 2, 24)   //scalastyle:off magic.number
      val expected = AnotherBodyYes("Name", start, end, "Reason")

      Json.fromJson[AnotherBody](input) must be (JsSuccess(expected, JsPath))
    }
    
    "fail when on missing all data" in {
      Json.fromJson[AnotherBody](Json.obj()) must
        be(JsError((JsPath \ "anotherBody") -> play.api.data.validation.ValidationError("error.path.missing")))
    }
  }
}