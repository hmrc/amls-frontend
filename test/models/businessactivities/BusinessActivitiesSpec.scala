package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Json}

class BusinessActivitiesSpec extends PlaySpec with MockitoSugar {

  val businessFranchise = BusinessFranchiseYes("test test")

  "BusinessActivities" must {
    val completeJson = Json.obj(
      "businessFranchise" -> true,
      "franchiseName" -> "test test"
    )

    val completeModel = BusinessActivities(businessFranchise = Some(businessFranchise))

    "Serialise as expected" in {

      Json.toJson(completeModel) must
        be(completeJson)
    }

    "Deserialise as expected" in {

      completeJson.as[BusinessActivities] must
        be(completeModel)
    }

  }

  "Partially complete BusinessActivities" must {

    val partialJson = Json.obj(
      "businessFranchise" -> true,
      "franchiseName" -> "test test"
    )

    val partialModel = BusinessActivities(businessFranchise = Some(businessFranchise))

    "Serialise as expected" in {

      Json.toJson(partialModel) must
        be(partialJson)
    }

    "Deserialise as expected" in {

      partialJson.as[BusinessActivities] must
        be(partialModel)
    }

  }

  "None" must {

    val initial: Option[BusinessActivities] = None

    "Merged with BusinessFranchise" in {
       val result = initial.businessFranchise(businessFranchise)
       result must be (BusinessActivities(Some(businessFranchise), None))

    }
  }

  "BusinessActivities" must {

    val initial = BusinessActivities(Some(businessFranchise), None)

    "Merged with BusinessFranchise" in{
       val newFranchiseName = BusinessFranchiseYes("test")
       val result = initial.businessFranchise(newFranchiseName)
       result must be (BusinessActivities(Some(newFranchiseName), None))
    }

  }

}
