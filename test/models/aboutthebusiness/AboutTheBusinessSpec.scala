package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, Json}

class AboutTheBusinessSpec extends PlaySpec with MockitoSugar {

    val regOfficeOrMainPlaceUK =  RegOfficeOrMainPlaceOfBusinessUK("38B", "Longbenton", None, None, "NE7 7DX")

    "AboutTheBusiness complete model" must {

      val completeJson = Json.obj(
        "previouslyRegistered" -> true,
        "previouslyRegisteredYes" -> "12345678",
        "isUKOrOverseas" -> true,
        "addressLine1" -> "38B",
        "addressLine2" -> "Longbenton",
        "addressLine3" -> JsNull,
        "addressLine4" -> JsNull,
        "postCode" -> "NE7 7DX"
      )

      val completeModel = AboutTheBusiness(Some(PreviouslyRegisteredYes("12345678")),
        Some(regOfficeOrMainPlaceUK))

      "Serialise as expected" in {

        Json.toJson(completeModel) must
          be(completeJson)
      }

      "Deserialise as expected" in {

        completeJson.as[AboutTheBusiness] must
          be(completeModel)
      }
    }

    "About TheBusiness partial complete model" must {
      val partialJson = Json.obj(
        "isUKOrOverseas" -> true,
        "addressLine1" -> "38B",
        "addressLine2" -> "Longbenton",
        "addressLine3" -> JsNull,
        "addressLine4" -> JsNull,
        "postCode" -> "NE7 7DX"
      )

      val partialModel = AboutTheBusiness(None, Some(regOfficeOrMainPlaceUK))

      "Serialise as expected" in {

        Json.toJson(partialModel) must
          be(partialJson)
      }

      "Deserialise as expected" in {

        partialJson.as[AboutTheBusiness] must
          be(partialModel)
      }
    }

   "first time addition to the AboutTheBusiness model" when {
     val initial : Option[AboutTheBusiness] = None

     "add registeredOfficeUK to About the business model " must {
       "return AboutTheBusiness with correct values" in {
         val result = initial.whereIsRegOfficeOrMainPlaceOfBusiness(regOfficeOrMainPlaceUK)
         result must be(AboutTheBusiness(None, Some(regOfficeOrMainPlaceUK)))
       }
     }
   }

  "Modify toAboutTheBusiness model with existing values" when {
    val initial = AboutTheBusiness(None, Some(regOfficeOrMainPlaceUK))

    "merge registeredOfficeUK to About the business model " must {
      "return AboutTheBusiness with correct values" in {

        val result = initial.whereIsRegOfficeOrMainPlaceOfBusiness(regOfficeOrMainPlaceUK)
        result must be(AboutTheBusiness(None, Some(regOfficeOrMainPlaceUK)))

      }
    }
  }

}
