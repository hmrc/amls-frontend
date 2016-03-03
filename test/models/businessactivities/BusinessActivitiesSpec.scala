package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class BusinessActivitiesSpec extends PlaySpec with MockitoSugar {

  val businessFranchise = BusinessFranchiseYes("test test")
  val transactionRecord = TransactionRecordYes(Set(Paper, DigitalSoftware("software")))
  val involvedInOther = InvolvedInOtherYes("test")
  val someTurnover = ExpectedBusinessTurnover.First

  "BusinessActivities" must {
    val completeJson = Json.obj(
      "involvedInOther" -> true,
      "details" -> "test" ,
      "expectedBusinessTurnover" -> "01",
      "businessFranchise" -> true,
      "franchiseName" -> "test test",
      "isRecorded" -> true,
      "transactions" -> Seq("01")
    )

    val completeModel = BusinessActivities(involvedInOther = Some(involvedInOther),
                                           expectedBusinessTurnover = Some(ExpectedBusinessTurnover.First),
                                           businessFranchise = Some(businessFranchise),
                                           transactionRecord = Some(TransactionRecordYes(Set(Paper))))

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
       result must be (BusinessActivities(None,None, Some(businessFranchise), None))

    }

    "Merged with TransactionRecord" in {
      val result = initial.transactionRecord(transactionRecord)
      result must be (BusinessActivities(None, None, None, Some(transactionRecord)))

    }
  }

  "BusinessActivities" must {

    val initial = BusinessActivities(Some(involvedInOther),  Some(someTurnover), Some(businessFranchise), Some(transactionRecord))

    "Merge BusinessFranchise" in{
       val newFranchiseName = BusinessFranchiseYes("test test")
       val result = initial.businessFranchise(newFranchiseName)
       result must be (BusinessActivities(Some(involvedInOther),  Some(someTurnover), Some(businessFranchise), Some(transactionRecord)))
    }

    "Merged with TransactionRecord" in {
      val newRecords = TransactionRecordYes(Set(Paper))
      val result = initial.transactionRecord(newRecords)
      result must be (BusinessActivities(Some(involvedInOther), Some(someTurnover), Some(businessFranchise), Some(newRecords)))
    }

    "Merge InvolvedInOther" in{
      val newInvolvedInOther= InvolvedInOtherYes("test")
      val result = initial.involvedInOther(newInvolvedInOther)
      result must be (BusinessActivities(Some(newInvolvedInOther),  Some(someTurnover), Some(businessFranchise), Some(transactionRecord)))
    }

    "Merged with TurnoverExpectIn12Months" must {
      "return TurnoverExpectIn12Months with correct turnover in the business set" in {
        val newTurnover = ExpectedBusinessTurnover.First
        val result = initial.expectedBusinessTurnover(newTurnover)
        result must be (BusinessActivities(Some(involvedInOther),  Some(newTurnover), Some(businessFranchise), Some(transactionRecord)))
      }
    }

  }

}
