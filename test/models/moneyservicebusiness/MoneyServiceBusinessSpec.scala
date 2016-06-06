package models.moneyservicebusiness

import models.registrationprogress.{Started, Completed, NotStarted, Section}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap
import org.mockito.Mockito._
import play.api.mvc.Call

class MoneyServiceBusinessSpec extends PlaySpec with MockitoSugar with MoneyServiceBusinessTestData {

  "MoneyServiceBusiness" should {

    "have an implicit conversion from Option which" when {

      "called with None" should {

        "return a default version of MoneyServiceBusiness" in {

          val res:MoneyServiceBusiness = None
          res must be (emptyModel)
        }
      }

      "called with a concrete value" should {
        "return the value passed in extracted from the option" in {
          val res:MoneyServiceBusiness = Some(completeModel)
          res must be (completeModel)
        }
      }
    }

    "Provide an implicit mongo-key" in {
      def x(implicit mongoKey: MongoKey[MoneyServiceBusiness]) = mongoKey()

      x must be("msb")
    }

    "have a section function that" when {
      implicit val cacheMap = mock[CacheMap]

      "model is empty" should {
        "return a NotStarted Section" in {
          when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)) thenReturn None
          MoneyServiceBusiness.section must be (Section(MoneyServiceBusiness.key, NotStarted, controllers.msb.routes.WhatYouNeedController.get()))
        }
      }

      "model is incomplete" should {
        "return a NotStarted Section" in {
          when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)) thenReturn Some(MoneyServiceBusiness(Some(MsbServices(
            Set(ChequeCashingScrapMetal)))))
          MoneyServiceBusiness.section must be (Section(MoneyServiceBusiness.key, Started, controllers.msb.routes.WhatYouNeedController.get()))
        }
      }

      "model is complete" should {
        "return a Completed Section" in {
          when(cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)) thenReturn Some(completeModel)
          MoneyServiceBusiness.section must be (Section(MoneyServiceBusiness.key, Completed, controllers.msb.routes.SummaryController.get()))
        }
      }
    }

    "have an isComplete function that" must {

      "correctly show if the model is complete" in  {
        completeModel.isComplete must be (true)
      }

      "correctly show if the model is incomplete" in {
        emptyModel.isComplete must be (false)
      }
    }

    "Serialise to expected Json" when {
      "model is complete" in {Json.toJson(completeModel) must be (completeJson)}
    }

    "Deserialise from Json as expected" when {
      "model is complete" in {completeJson.as[MoneyServiceBusiness] must be (completeModel)}
    }
  }
}

trait MoneyServiceBusinessTestData {
  private val msbService = MsbServices(Set(ChequeCashingScrapMetal, ChequeCashingNotScrapMetal))
  private val businessUseAnIPSP = BusinessUseAnIPSPYes("name" ,"123456789123456")

  val completeModel = MoneyServiceBusiness(Some(msbService),
    Some(ExpectedThroughput.Second),
    Some(businessUseAnIPSP),
    Some(IdentifyLinkedTransactions(true)),
    Some(BusinessAppliedForPSRNumberYes("123456"))

  )

  val emptyModel = MoneyServiceBusiness(None)

  val completeJson = Json.obj(
    "msbServices" -> Json.obj(
    "msbServices" -> Json.arr("04", "03")
    ),
    "throughput" -> Json.obj("throughput" -> "02"),
    "businessUseAnIPSP" -> Json.obj("useAnIPSP" -> true,
                                    "name" -> "name",
                                    "referenceNumber" -> "123456789123456"),
    "identifyLinkedTransactions" -> Json.obj("linkedTxn" -> true),
    "businessAppliedForPSRNumber" -> Json.obj("appliedFor" -> true,
                                      "regNumber" -> "123456")

  )

  val emptyJson = Json.obj("msbServices" -> Json.arr())
}
