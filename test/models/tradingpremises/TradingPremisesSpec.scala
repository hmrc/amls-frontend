package models.tradingpremises

import models.businessmatching._
import models.registrationprogress.{Completed, NotStarted, Started}
import org.joda.time.LocalDate
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => meq}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.StatusConstants


class TradingPremisesSpec extends WordSpec with MustMatchers with MockitoSugar with OneAppPerSuite {

  val ytp = YourTradingPremises(
    "foo",
    Address(
      "1",
      "2",
      None,
      None,
      "asdfasdf"
    ),
    Some(true),
    Some(new LocalDate(1990, 2, 24))
  )

  val businessStructure = SoleProprietor
  val agentName = AgentName("test")
  val agentCompanyName = AgentCompanyDetails("test", Some("12345678"))
  val agentPartnership = AgentPartnership("test")
  val wdbd = WhatDoesYourBusinessDo(
    Set(
      BillPaymentServices,
      EstateAgentBusinessService,
      MoneyServiceBusiness)
  )
  val msbServices = MsbServices(Set(TransmittingMoney, CurrencyExchange))
  val completeModel = TradingPremises(
    Some(RegisteringAgentPremises(true)),
    Some(ytp),
    Some(businessStructure),
    Some(agentName),
    Some(agentCompanyName),
    Some(agentPartnership),
    Some(wdbd),
    Some(msbServices),
    false,
    Some(123456),
    Some("Added"),
    Some(ActivityEndDate(new LocalDate(1999, 1, 1)))

  )

  val incompleteModel = TradingPremises(Some(RegisteringAgentPremises(true)),
    Some(ytp), Some(businessStructure), Some(agentName), None, None, None, None)

  val completeJson = Json.obj("registeringAgentPremises" -> Json.obj("agentPremises" -> true),
    "yourTradingPremises" -> Json.obj("tradingName" -> "foo",
      "addressLine1" -> "1",
      "addressLine2" -> "2",
      "postcode" -> "asdfasdf",
      "isResidential" -> true,
      "startDate" -> "1990-02-24"),
    "businessStructure" -> Json.obj("agentsBusinessStructure" -> "01"),
    "agentName" -> Json.obj("agentName" -> "test"),
    "agentCompanyDetails" -> Json.obj("agentCompanyName" -> "test", "companyRegistrationNumber" -> "12345678"),
    "agentPartnership" -> Json.obj("agentPartnership" -> "test"),
    "whatDoesYourBusinessDoAtThisAddress" -> Json.obj("activities" -> Json.arr("02", "03", "05")),
    "msbServices" -> Json.obj("msbServices" -> Json.arr("01", "02")),
    "hasChanged" -> false,
    "lineId" -> 123456,
    "status" -> "Added",
    "endDate" -> Json.obj("endDate" -> "1999-01-01")
  )

  "TradingPremises" must {

    "return a TP model reflecting the provided data" when {
      "given 'your agent' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.businessStructure(LimitedLiabilityPartnership)
        newTP must be(tp.copy(businessStructure = Some(LimitedLiabilityPartnership), hasChanged = true))
      }

      "given 'agent name' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.agentName(agentName)
        newTP must be(tp.copy(agentName = Some(agentName), hasChanged = true))
      }

      "given 'agent company name' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.agentCompanyDetails(agentCompanyName)
        newTP must be(tp.copy(agentCompanyDetails = Some(agentCompanyName), hasChanged = true))
      }

      "given 'agent partnership' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.agentPartnership(agentPartnership)
        newTP must be(tp.copy(agentPartnership = Some(agentPartnership), hasChanged = true))
      }

      "given 'your trading premises' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.yourTradingPremises(ytp)
        newTP must be(tp.copy(yourTradingPremises = Some(ytp), hasChanged = true))
      }

      "given 'what does your business do' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.whatDoesYourBusinessDoAtThisAddress(wdbd)
        newTP must be(tp.copy(whatDoesYourBusinessDoAtThisAddress = Some(wdbd), hasChanged = true))
      }

      "given 'agent premises' data" in {
        val tp = TradingPremises()
        val newTP = tp.registeringAgentPremises(RegisteringAgentPremises(true))
        newTP must be(tp.copy(registeringAgentPremises = Some(RegisteringAgentPremises(true)), hasChanged = true))
      }

      "given 'msb' data" in {
        val tp = TradingPremises(None, None, None)
        val newTP = tp.msbServices(msbServices)
        newTP must be(tp.copy(msbServices = Some(msbServices), hasChanged = true))
      }
    }

    "Serialise as expected" in {
      Json.toJson(completeModel) must
        be(completeJson)
    }

    "deserialise correctly when hasChanged field is missing from the Json" in {
      (completeJson - "hasChanged").as[TradingPremises] must be(completeModel)
    }

    "Deserialise as expected" in {
      completeJson.as[TradingPremises] must be(completeModel)
      TradingPremises.writes.writes(completeModel) must be(completeJson)
    }

    "isComplete" must {
      "return true" when {
        "tradingPremises contains complete data" in {
          completeModel.isComplete must be(true)
        }
      }

      "return false" when {
        "tradingPremises contains incomplete data" in {
          val tradingPremises = TradingPremises(Some(RegisteringAgentPremises(true)), None)
          tradingPremises.isComplete must be(false)
        }
        "tradingPremises no data" in {
          val tradingPremises = TradingPremises(None, None)
          tradingPremises.isComplete must be(true)
        }
      }

    }
  }

  "Amendment and Variation flow" when {
    "the section is complete with all the trading premises being removed" must {
      "successfully redirect to what you need page" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(TradingPremises(status = Some(StatusConstants.Deleted), hasChanged = true),
            TradingPremises(status = Some(StatusConstants.Deleted), hasChanged = true))))
        val section = TradingPremises.section(mockCacheMap)

        section.hasChanged must be(true)
        section.status must be(NotStarted)
        section.call must be(controllers.tradingpremises.routes.TradingPremisesAddController.get(true))
      }
    }

    "the section is complete with all the trading premises being removed and has one incomplete model" must {
      "successfully redirect to what you need page" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(TradingPremises(status = Some(StatusConstants.Deleted), hasChanged = true),
            TradingPremises(status = Some(StatusConstants.Deleted), hasChanged = true),
            TradingPremises(Some(RegisteringAgentPremises(true)), None))))
        val section = TradingPremises.section(mockCacheMap)

        section.hasChanged must be(true)
        section.status must be(Started)
        section.call must be(controllers.tradingpremises.routes.WhatYouNeedController.get(3))
      }
    }

    "the section is complete with one of the trading premises object being removed" must {
      "successfully redirect to check your answers page" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(TradingPremises(status = Some(StatusConstants.Deleted), hasChanged = true),
            completeModel)))
        val section = TradingPremises.section(mockCacheMap)

        section.hasChanged must be(true)
        section.status must be(Completed)
        section.call must be(controllers.tradingpremises.routes.SummaryController.answers())
      }
    }

    "the section is complete with all the trading premises unchanged" must {
      "successfully redirect to check your answers page" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(completeModel, completeModel)))
        val section = TradingPremises.section(mockCacheMap)

        section.hasChanged must be(false)
        section.status must be(Completed)
        section.call must be(controllers.tradingpremises.routes.SummaryController.answers())
      }
    }

    "the section is complete with all the trading premises being modified" must {
      "successfully redirect to check your answers page" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(TradingPremises(status = Some(StatusConstants.Updated), hasChanged = true),
            TradingPremises(status = Some(StatusConstants.Updated), hasChanged = true))))
        val section = TradingPremises.section(mockCacheMap)

        section.hasChanged must be(true)
        section.status must be(Completed)
        section.call must be(controllers.tradingpremises.routes.SummaryController.answers())
      }
    }
  }

  it when {

    "the section consists of just 1 empty Trading premises" must {
      "return a result indicating NotStarted" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(TradingPremises())))

        TradingPremises.section(mockCacheMap).status must be(models.registrationprogress.NotStarted)
      }
    }

    "the section consists of a partially complete model followed by a completely empty one" must {
      "return a result indicating partial completeness" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(incompleteModel, TradingPremises())))

        TradingPremises.section(mockCacheMap).status must be(models.registrationprogress.Started)
      }
    }

    "the section consists of a complete model followed by an empty one" must {
      "return a result indicating completeness" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(completeModel, TradingPremises())))

        TradingPremises.section(mockCacheMap).status must be(models.registrationprogress.Completed)
      }
    }

    "has a completed model, an empty one and an incomplete one" when {
      "return the correct index" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(completeModel, TradingPremises(), incompleteModel)))

        TradingPremises.section(mockCacheMap).call.url must be(controllers.tradingpremises.routes.WhatYouNeedController.get(3).url)
      }
    }

  }

  "anyChanged" must {
    val originalBankDetails = Seq(TradingPremises(None, None))
    val originalBankDetailsChanged = Seq(TradingPremises(None, None, hasChanged = true))

    "return false" when {
      "no BankDetails within the sequence have changed" in {
        val res = TradingPremises.anyChanged(originalBankDetails)
        res must be(false)
      }
    }
    "return true" when {
      "at least one BankDetails within the sequence has changed" in {
        val res = TradingPremises.anyChanged(originalBankDetailsChanged)
        res must be(true)
      }
    }
  }

}
