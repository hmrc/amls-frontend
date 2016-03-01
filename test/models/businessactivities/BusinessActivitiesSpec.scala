package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Json}

class BusinessActivitiesSpec extends PlaySpec with MockitoSugar {

  val businessFranchise = BusinessFranchiseYes("test test")
  val involvedInOther = InvolvedInOtherYes("test")
  val  someTurnoverAmls = FirstTurnoverAmls

  "BusinessActivities" must {
    val completeJson = Json.obj(
      "involvedInOther" -> true,
      "details" -> "test" ,
      "turnoverOverExpectIn12MOnths" -> "01",
      "businessFranchise" -> true,
      "franchiseName" -> "test test"
    )

    val completeModel = BusinessActivities(involvedInOther = Some(involvedInOther), businessFranchise = Some(businessFranchise), turnerOverExpectIn12MonthsRelatedToAMLS = Some(FirstTurnoverAmls) )

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
      "involvedInOther" -> true,
      "details" -> "test"

    )

    val partialModel = BusinessActivities(involvedInOther = Some(involvedInOther))

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
       result must be (BusinessActivities(None,None,Some(businessFranchise) ))

    }


  }

  "BusinessActivities" must {

    val initial = BusinessActivities(Some(involvedInOther),  Some(someTurnoverAmls), Some(businessFranchise) )

    "Merge BusinessFranchise" in{
       val newFranchiseName = BusinessFranchiseYes("test test")
       val result = initial.businessFranchise(newFranchiseName)
       result must be (BusinessActivities(Some(involvedInOther),  Some(someTurnoverAmls), Some(businessFranchise)))
    }

    "Merge InvolvedInOther" in{
      val newInvolvedInOther= InvolvedInOtherYes("test")
      val result = initial.involvedInOther(newInvolvedInOther)
      result must be ( BusinessActivities(Some(involvedInOther),  Some(someTurnoverAmls), Some(businessFranchise)))
    }

    "Merged with TurnoverExpectIn12MonthsRelatedToAMLS" must {
      "return TurnoverExpectIn12MonthsRelatedToAMLS with correct turnover in the business set" in {
        val newTurnover = FirstTurnoverAmls
        val result = initial.turnerOverExpectIn12MonthsRelatedToAMLS(newTurnover)
        result must be ( BusinessActivities(Some(involvedInOther),  Some(newTurnover), Some(businessFranchise)))
      }
    }

  }

}
