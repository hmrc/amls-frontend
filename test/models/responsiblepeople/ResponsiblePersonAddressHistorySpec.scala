package models.responsiblepeople

import models.Country
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ZeroToFiveMonths}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class ResponsiblePersonAddressHistorySpec extends PlaySpec with MockitoSugar {

  val DefaultCurrentAddress = ResponsiblePersonCurrentAddress(PersonAddressUK("Line 1", "Line 2", None, None, "NE981ZZ"), ZeroToFiveMonths)
  val DefaultAdditionalAddress = ResponsiblePersonAddress(PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain", "ES")), SixToElevenMonths)
  val DefaultAdditionalExtraAddress = ResponsiblePersonAddress(PersonAddressUK("Line 1", "Line 2", None, None, "NE1234"), OneToThreeYears)

  val NewCurrentAddress = ResponsiblePersonCurrentAddress(PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain", "ES")), ZeroToFiveMonths)
  val NewAdditionalAddress = ResponsiblePersonAddress(PersonAddressNonUK("Line 1", "Line 2", None, None, Country("France", "FR")), ZeroToFiveMonths)
  val NewAdditionalExtraAddress = ResponsiblePersonAddress(PersonAddressNonUK("Line 1", "Line 2", None, None, Country("UK", "UK")), SixToElevenMonths)

  val DefaultAddressHistory = ResponsiblePersonAddressHistory(
    currentAddress = Some(DefaultCurrentAddress),
    additionalAddress = Some(DefaultAdditionalAddress),
    additionalExtraAddress = Some(DefaultAdditionalExtraAddress)
  )

  val NewAddressHistory = ResponsiblePersonAddressHistory(
    currentAddress = Some(NewCurrentAddress),
    additionalAddress = Some(NewAdditionalAddress),
    additionalExtraAddress = Some(NewAdditionalExtraAddress)
  )

  "ResponsiblePersonAddressHistory" must {

    "update the model with current address" in {
      val updated = DefaultAddressHistory.currentAddress(NewCurrentAddress)
      updated.currentAddress must be(Some(NewCurrentAddress))
    }

    "update the model with new additionalAddress" in {
      val updated = DefaultAddressHistory.additionalAddress(NewAdditionalAddress)
      updated.additionalAddress must be(Some(NewAdditionalAddress))
    }

    "update the model with new additionalExtraAddress" in {
      val updated = DefaultAddressHistory.additionalExtraAddress(NewAdditionalExtraAddress)
      updated.additionalExtraAddress must be(Some(NewAdditionalExtraAddress))
    }

    "validate complete json" must {

      val completeJson = Json.obj(
        "currentAddress" -> Json.obj(
          "personAddress" -> Json.obj(
            "personAddressLine1" -> "Line 1",
            "personAddressLine2" -> "Line 2",
            "personAddressPostCode" -> "NE981ZZ"
          ),
          "timeAtAddress" -> Json.obj(
            "timeAtAddress" -> "01"
          )
        ),
        "additionalAddress" -> Json.obj(
          "personAddress" -> Json.obj(
            "personAddressLine1" -> "Line 1",
            "personAddressLine2" -> "Line 2",
            "personAddressCountry" -> "ES"
          ),
          "timeAtAddress" -> Json.obj(
            "timeAtAddress" -> "02"
          )
        ),
        "additionalExtraAddress" -> Json.obj(
          "personAddress" -> Json.obj(
            "personAddressLine1" -> "Line 1",
            "personAddressLine2" -> "Line 2",
            "personAddressPostCode" -> "NE1234"
          ),
          "timeAtAddress" -> Json.obj(
            "timeAtAddress" -> "03"
          )
        ))

      "Serialise as expected" in {
        Json.toJson(DefaultAddressHistory) must be(completeJson)
      }

      "Deserialise as expected" in {
        completeJson.as[ResponsiblePersonAddressHistory] must be(DefaultAddressHistory)
      }

    }
  }

  "Successfully validate if the model is complete" when {

    "the model is fully complete" in {

      val initial = ResponsiblePersonAddressHistory(
        Some(DefaultCurrentAddress),
        Some(DefaultAdditionalAddress),
        Some(DefaultAdditionalExtraAddress)
      )

      initial.isComplete must be(true)
    }

    "the model has a current address" in {
      val initial = ResponsiblePersonAddressHistory(Some(DefaultCurrentAddress), None, None)
      initial.isComplete must be(true)
    }

    "the model is incomplete" in {
      val initial = ResponsiblePersonAddressHistory(None, None, None)
      initial.isComplete must be(false)
    }

  }

  "Merge with existing model" when {

    val initial = ResponsiblePersonAddressHistory(
      Some(DefaultCurrentAddress),
      Some(DefaultAdditionalAddress),
      Some(DefaultAdditionalExtraAddress))

    "Merged with add person" must {
      "return ResponsiblePeople with correct add person" in {
        val result = initial.currentAddress(NewCurrentAddress)
        result must be (ResponsiblePersonAddressHistory(Some(NewCurrentAddress), Some(DefaultAdditionalAddress), Some(DefaultAdditionalExtraAddress)))
      }
    }

    "Merged with DefaultPersonResidenceType" must {
      "return ResponsiblePeople with correct DefaultPersonResidenceType" in {
        val result = initial.additionalAddress(NewAdditionalAddress)
        result must be (ResponsiblePersonAddressHistory(Some(DefaultCurrentAddress), Some(NewAdditionalAddress), Some(DefaultAdditionalExtraAddress)))
      }
    }

    "Merged with DefaultPreviousHomeAddress" must {
      "return ResponsiblePeople with correct DefaultPreviousHomeAddress" in {
        val result = initial.additionalExtraAddress(NewAdditionalExtraAddress)
        result must be (ResponsiblePersonAddressHistory(Some(DefaultCurrentAddress), Some(DefaultAdditionalAddress), Some(NewAdditionalExtraAddress)))
      }
    }
  }
}