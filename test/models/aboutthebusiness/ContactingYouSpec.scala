package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.libs.json.Json

class ContactingYouSpec extends PlaySpec with MockitoSugar {
  "Contacting You Form Details" must {

    val completeJson = Json.obj(
      "phoneNumber" -> "1234567890",
      "email" -> "test@test.com",
      "website" -> "http://www.test.com",
      "letterToThisAddress" -> true
    )

    val completeModel = ContactingYouForm("1234567890", "test@test.com", Some("http://www.test.com"), true)

    "successfully validate given a phone number, email and letterToThisAddress" in {

      val data = Map(
        "phoneNumber" -> Seq("1234567890"),
        "email" -> Seq("test@test.com"),
        "letterToThisAddress" -> Seq("true")
      )

      ContactingYouForm.formRule.validate(data) must
        be(Success(ContactingYouForm("1234567890", "test@test.com", None, true)))
    }

    "successfully validate given a phone number, email, website address and letterToThisAddress " in {

      val data = Map(
        "phoneNumber" -> Seq("1234567890"),
        "email" -> Seq("test@test.com"),
        "website" -> Seq("http://www.test.com"),
        "letterToThisAddress" -> Seq("true")
      )

      ContactingYouForm.formRule.validate(data) must
        be(Success(ContactingYouForm("1234567890", "test@test.com", Some("http://www.test.com"), true)))
    }

    "serialize as expected" in {
      Json.toJson(completeModel) must be(completeJson)
    }

    "deserialize as expected" in {
     completeJson.as[ContactingYouForm] must be(completeModel)
    }

  }
}