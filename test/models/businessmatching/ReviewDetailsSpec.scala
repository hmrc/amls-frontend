package models.businessmatching

import models.Country
import models.businesscustomer.ReviewDetails
import models.businesscustomer.Address
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}

class ReviewDetailsSpec extends PlaySpec with MockitoSugar {
  suite =>

  val model = ReviewDetails(
    businessName = "Name",
    businessType = Some(BusinessType.SoleProprietor),
    businessAddress = Address(
      "1 Test Street", "Test Town", None, None, None, Country("United Kingdom", "GB")
    ),
    safeId = "safeId"
  )

  val json = Json.obj(
    "businessName" -> "Name",
    "businessType" -> "Sole Trader",
    "businessAddress" -> Json.obj(
      "line_1" -> "1 Test Street",
      "line_2" -> "Test Town",
      "country" -> "GB"
    ),
    "safeId" -> "safeId"
  )

  "Review Details Model" must {

    "validate correctly with a `Some` business type" in {

      Json.fromJson[ReviewDetails](json) mustEqual JsSuccess(model)
      Json.toJson(model) mustEqual (json)
    }

    "validate successfully with an invalid business type" in {

      val model = suite.model.copy(
        businessType = None
      )

      val json = Json.obj(
        "businessName" -> "Name",
        "businessType" -> "corporate body",
        "businessAddress" -> Json.obj(
          "line_1" -> "1 Test Street",
          "line_2" -> "Test Town",
          "country" -> "GB"
        ),
        "safeId" -> "safeId"
      )

      Json.fromJson[ReviewDetails](json) mustEqual JsSuccess(model)
    }

    "validate correctly with a `None` business type" in {

      val model = suite.model.copy(businessType = None)

      val json = Json.obj(
        "businessName" -> "Name",
        "businessAddress" -> Json.obj(
          "line_1" -> "1 Test Street",
          "line_2" -> "Test Town",
          "country" -> "GB"
        ),
        "safeId" -> "safeId"
      )

      Json.fromJson[ReviewDetails](json) mustEqual JsSuccess(model)
    }
  }
}
