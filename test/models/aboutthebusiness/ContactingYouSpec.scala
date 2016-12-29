package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.Success
import play.api.libs.json.Json

class ContactingYouSpec extends PlaySpec with MockitoSugar {
  "Contacting You Form Details" must {

    "successfully validate" when {
      "given a phone number, email and letterToThisAddress" in {

        val data = Map(
          "phoneNumber" -> Seq("1234567890"),
          "email" -> Seq("test@test.com"),
          "letterToThisAddress" -> Seq("true")
        )

        ContactingYouForm.formRule.validate(data) must
          be(Success(ContactingYouForm("1234567890", "test@test.com", true)))
      }
    }

    val completeJson = Json.obj(
      "phoneNumber" -> "1234567890",
      "email" -> "test@test.com",
      "letterToThisAddress" -> true
    )

    val completeModel = ContactingYouForm("1234567890", "test@test.com", true)

    "serialize as expected" in {
      Json.toJson(completeModel) must be(completeJson)
    }

    "deserialize as expected" in {
      completeJson.as[ContactingYouForm] must be(completeModel)
    }

    "write correct data" in {
      val model = ContactingYou("1234567890", "test@test.com")
      ContactingYou.formWrites.writes(model) must
        be(Map(
          "phoneNumber" -> Seq("1234567890"),
          "email" -> Seq("test@test.com")
        ))
    }

  }
}