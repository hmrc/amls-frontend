package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class AboutTheBusinessSpec extends PlaySpec with MockitoSugar {

  val previouslyRegistered = PreviouslyRegisteredYes("12345678")

  val regForVAT = RegisteredForVATYes("123456789")

  "AboutTheBusiness" must {
    val completeJson = Json.obj(
      "previouslyRegistered" -> true,
      "prevMLRRegNo" -> "12345678"
    )

    val completeModel = AboutTheBusiness(Some(PreviouslyRegisteredYes("12345678")))

    "Serialise as expected" in {

      Json.toJson(completeModel) must
        be(completeJson)
    }

    "Deserialise as expected" in {

      completeJson.as[AboutTheBusiness] must
        be(completeModel)
    }
  }

  "Partially complete AboutTheBusiness" must {

    val partialJson = Json.obj(
      "previouslyRegistered" -> true,
      "prevMLRRegNo" -> "12345678"
    )

    val partialModel = AboutTheBusiness(Some(previouslyRegistered), None)

    "Serialise as expected" in {

      Json.toJson(partialModel) must
        be(partialJson)
    }

    "Deserialise as expected" in {

      partialJson.as[AboutTheBusiness] must
        be(partialModel)
    }
  }

  "None" when {
    val initial: Option[AboutTheBusiness] = None

    "Merged with previously registered with MLR" must {
      "return AboutTheBusiness with correct previously registered for MLR option" in {
        val result = initial.previouslyRegistered(previouslyRegistered)
        result must be (AboutTheBusiness(Some(previouslyRegistered), None))
      }
    }

    "Merged with RegisteredForVAT" must {
      "return AboutTheBusiness with correct VAT Registered option" in {
        val result = initial.registeredForVAT(regForVAT)
        result must be (AboutTheBusiness(None, Some(regForVAT)))
      }
    }
  }


  "AboutTheBusiness" when {

    "previouslyRegistered already set" when {

      val initial = AboutTheBusiness(Some(previouslyRegistered), None)

      "Merged with previously registered with MLR" must {
        "return AboutTheBusiness with correct previously registered status" in {
          val newPreviouslyRegistered = PreviouslyRegisteredYes("22222222")
          val result = initial.previouslyRegistered(newPreviouslyRegistered)
          result must be (AboutTheBusiness(Some(newPreviouslyRegistered), None))
        }
      }

      "Merged with RegisteredForVAT" must {
        "return AboutTheBusiness with correct VAT registration number" in {
          val newregForVAT = RegisteredForVATYes("012345678")
          val result = initial.registeredForVAT(newregForVAT)
          result must be (AboutTheBusiness(Some(previouslyRegistered), Some(newregForVAT)))
        }
      }
    }

    "AboutTheBusiness" when {

      "previouslyRegistered already set" when {

        val initial = AboutTheBusiness(None, Some(regForVAT))

        "return AboutTheBusiness with correct VAT registration number" must {
          "return AboutTheBusiness with correct previously registered status" in {
            val newPreviouslyRegistered = PreviouslyRegisteredYes("22222222")
            val result = initial.previouslyRegistered(newPreviouslyRegistered)
            result must be(AboutTheBusiness(Some(newPreviouslyRegistered), Some(regForVAT)))
          }
        }

        "Merged with RegisteredForVAT" must {
          "Merged with previously registered with MLR" in {
            val newregForVAT = RegisteredForVATYes("012345678")
            val result = initial.registeredForVAT(newregForVAT)
            result must be(AboutTheBusiness(None, Some(newregForVAT)))
          }
        }
      }
    }
  }

}