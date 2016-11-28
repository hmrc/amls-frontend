package services

import connectors.{AmlsConnector, DataCacheConnector}
import exceptions.NoEnrolmentException
import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.SoleProprietor
import models.confirmation.{BreakdownRow, Currency}
import models.estateagentbusiness.EstateAgentBusiness
import models.responsiblepeople.{PersonName, ResponsiblePeople}
import models.status.SubmissionDecisionApproved
import models.tradingpremises.TradingPremises
import models.{AmendVariationResponse, SubscriptionResponse}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Logger
import play.api.http.Status._
import play.api.test.FakeApplication
import play.api.test.Helpers.{OK => _, _}
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, OrgAccount}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, Principal}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.StatusConstants

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class SubmissionServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience with OneAppPerSuite {

  implicit override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.amounts.registration" -> 100))

  trait Fixture {

    val TestSubmissionService = new SubmissionService {
      override private[services] val cacheConnector = mock[DataCacheConnector]
      override private[services] val amlsConnector = mock[AmlsConnector]
      override private[services] val ggService = mock[GovernmentGatewayService]
      override private[services] val authEnrolmentsService = mock[AuthEnrolmentsService]
    }

    implicit val authContext = mock[AuthContext]
    val principle = Principal(None, Accounts(org = Some(OrgAccount("", Org("TestOrgRef")))))
    when {
      authContext.principal
    }.thenReturn(principle)

    implicit val headerCarrier = HeaderCarrier()

    val enrolmentResponse = HttpResponse(OK)

    val subscriptionResponse = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "amlsRef",
      registrationFee = 0,
      fpFee = None,
      premiseFee = 0,
      totalFees = 0,
      paymentReference = ""
    )

    val amendmentResponse = AmendVariationResponse(
      processingDate = "",
      etmpFormBundleNumber = "",
      registrationFee = 100,
      fpFee = Some(0),
      premiseFee = 0,
      totalFees = 100,
      paymentReference = Some("XA111123451111"),
      difference = Some(0)
    )

    val safeId = "safeId"
    val amlsRegistrationNumber = "amlsRegNo"
    val businessType = SoleProprietor

    val reviewDetails = mock[ReviewDetails]

    when {
      reviewDetails.safeId
    } thenReturn safeId
    when {
      reviewDetails.businessType
    } thenReturn Some(businessType)

    val businessMatching = mock[BusinessMatching]

    when {
      businessMatching.reviewDetails
    } thenReturn Some(reviewDetails)

    val cache = mock[CacheMap]

    when {
      cache.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn Some(businessMatching)
    when {
      cache.getEntry[EstateAgentBusiness](EstateAgentBusiness.key)
    } thenReturn Some(mock[EstateAgentBusiness])
    when {
      cache.getEntry[AboutTheBusiness](AboutTheBusiness.key)
    } thenReturn Some(mock[AboutTheBusiness])
    when {
      cache.getEntry[Seq[BankDetails]](BankDetails.key)
    } thenReturn Some(mock[Seq[BankDetails]])
    when {
      cache.getEntry[AmendVariationResponse](AmendVariationResponse.key)
    } thenReturn Some(amendmentResponse)
  }

  "SubmissionService" must {

    "successfully subscribe and enrol" in new Fixture {

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        TestSubmissionService.cacheConnector.save[SubscriptionResponse](eqTo(SubscriptionResponse.key), any())(any(), any(), any())
      } thenReturn Future.successful(CacheMap("", Map.empty))

      when {
        TestSubmissionService.amlsConnector.subscribe(any(), eqTo(safeId))(any(), any(), any(), any(), any())
      } thenReturn Future.successful(subscriptionResponse)

      when {
        TestSubmissionService.ggService.enrol(eqTo("amlsRef"), eqTo(safeId))(any(), any())
      } thenReturn Future.successful(enrolmentResponse)

      whenReady(TestSubmissionService.subscribe) {
        result =>
          result must equal(subscriptionResponse)
      }
    }

    "successfully submit amendment" in new Fixture {

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
      } thenReturn Future.successful(CacheMap("", Map.empty))

      when {
        TestSubmissionService.amlsConnector.update(any(), eqTo(amlsRegistrationNumber))(any(), any(), any(), any(), any())
      } thenReturn Future.successful(amendmentResponse)

      when {
        TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      }.thenReturn(Future.successful(Some(amlsRegistrationNumber)))


      whenReady(TestSubmissionService.update) {
        result =>
          result must equal(amendmentResponse)
      }
    }

    "successfully submit variation" in new Fixture {

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
      } thenReturn Future.successful(CacheMap("", Map.empty))

      when {
        TestSubmissionService.amlsConnector.variation(any(), eqTo(amlsRegistrationNumber))(any(), any(), any(), any(), any())
      } thenReturn Future.successful(amendmentResponse)

      when {
        TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      }.thenReturn(Future.successful(Some(amlsRegistrationNumber)))


      whenReady(TestSubmissionService.variation) {
        result =>
          result must equal(amendmentResponse)
      }
    }

    "successfully submit amendment returning submission data" in new Fixture {

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
      } thenReturn Some(Seq(TradingPremises()))

      when {
        cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
      } thenReturn Some(Seq(ResponsiblePeople()))

      when {
        TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
      } thenReturn Future.successful(CacheMap("", Map.empty))

      when {
        TestSubmissionService.amlsConnector.update(any(), eqTo(amlsRegistrationNumber))(any(), any(), any(), any(), any())
      } thenReturn Future.successful(amendmentResponse)

      val rows = Seq(
        BreakdownRow("confirmation.submission", 1, 100, 100)
      ) ++ Seq(
        BreakdownRow("confirmation.responsiblepeople", 1, 100, 0)
      ) ++ Seq(
        BreakdownRow("confirmation.tradingpremises", 1, 115, 0)
      )

      val response = Some(Some("XA111123451111"), Currency.fromBD(100), rows, Some(Currency.fromBD(0)))

      whenReady(TestSubmissionService.
        getAmendment) {
        result =>
          result must equal(response)
      }
    }

    "not include responsible people who have been deleted" in new Fixture {

      val people = Seq(
        ResponsiblePeople(Some(PersonName("Valid", None, "Person", None, None))),
        ResponsiblePeople(Some(PersonName("Deleted", None, "Person", None, None)), status = Some(StatusConstants.Deleted))
      )

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
      } thenReturn Some(Seq(TradingPremises()))

      when {
        cache.getEntry[AmendVariationResponse](eqTo(AmendVariationResponse.key))(any())
      } thenReturn Some(amendmentResponse)

      when(cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())) thenReturn Some(people)

      val result = await(TestSubmissionService.getAmendment)

      whenReady(TestSubmissionService.getAmendment) { result => result foreach {
        case (_, _, rows, _) => rows.filter(_.label == "confirmation.responsiblepeople").head.quantity mustBe 1
      }
      }

    }

    "not show negative fees for responsible people who have already been paid for" in new Fixture {

      val people = Seq(
        ResponsiblePeople(Some(PersonName("Unfit", Some("and"), "Unproper", None, None)), hasAlreadyPassedFitAndProper = Some(false)),
        ResponsiblePeople(Some(PersonName("Fit", Some("and"), "Proper", None, None)), hasAlreadyPassedFitAndProper = Some(true))
      )

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
      } thenReturn Some(Seq(TradingPremises()))

      when {
        cache.getEntry[AmendVariationResponse](eqTo(AmendVariationResponse.key))(any())
      } thenReturn Some(amendmentResponse)

      when(cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())) thenReturn Some(people)

      val result = await(TestSubmissionService.getAmendment)

      whenReady(TestSubmissionService.getAmendment) { result => result foreach {
        case (_, _, rows, _) =>
          val unpaidRow = rows.filter(_.label == "confirmation.unpaidpeople").head
          unpaidRow.perItm.value mustBe 0
          unpaidRow.total.value mustBe 0
      }
      }

    }

    "retrieve data from variation submission" in new Fixture {

      val variationResponse = AmendVariationResponse(
        processingDate = "",
        etmpFormBundleNumber = "",
        registrationFee = 100,
        fpFee = Some(0),
        premiseFee = 0,
        totalFees = 100,
        paymentReference = Some("12345"),
        difference = Some(0),
        addedResponsiblePeople = 1,
        addedFullYearTradingPremises = 1,
        halfYearlyTradingPremises = 3,
        zeroRatedTradingPremises = 1
      )

      val rpFee: BigDecimal = 100
      val tpFee: BigDecimal = 115
      val tpHalfFee: BigDecimal = tpFee / 2
      val tpTotalFee: BigDecimal = tpFee + (tpHalfFee * 3)
      val totalFee: BigDecimal = rpFee + tpTotalFee

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      } thenReturn Future.successful(Some("12345"))

      when {
        TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
      } thenReturn Future.successful(CacheMap("", Map.empty))

      when {
        cache.getEntry[AmendVariationResponse](any())(any())
      } thenReturn Some(variationResponse)

      val rows = Seq(
        BreakdownRow("confirmation.responsiblepeople", 1, Currency(100), Currency(rpFee))
      ) ++ Seq(
        BreakdownRow("confirmation.tradingpremises.zero", 1, Currency(0), Currency(0))
      ) ++ Seq(
        BreakdownRow("confirmation.tradingpremises.half", 3, Currency(57.50), Currency(tpHalfFee * 3))
      ) ++ Seq(
        BreakdownRow("confirmation.tradingpremises", 1, Currency(115), Currency(tpFee))
      )

      val response = Some(Some("12345"), Currency.fromBD(totalFee), rows)

      whenReady(TestSubmissionService.getVariation) {
        result =>
          result must equal(response)
      }

    }

    "return failed future when no enrolment" in new Fixture {

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      }.thenReturn(Future.successful(None))


      whenReady(TestSubmissionService.update.failed) {
        result =>
          result mustBe a[NoEnrolmentException]
      }
    }

    "notify user of variation fees to pay" when {

      val rpFee: BigDecimal = 100
      val tpFee: BigDecimal = 115
      val tpHalfFee: BigDecimal = tpFee / 2
      val tpTotalFee: BigDecimal = tpFee + (tpHalfFee * 3)
      val totalFee: BigDecimal = rpFee + tpTotalFee

      val testVariationResponse = AmendVariationResponse(
        processingDate = "",
        etmpFormBundleNumber = "",
        registrationFee = 100,
        fpFee = Some(0),
        premiseFee = 0,
        totalFees = 100,
        paymentReference = Some(""),
        difference = Some(0),
        addedResponsiblePeople = 0,
        addedFullYearTradingPremises = 0,
        halfYearlyTradingPremises = 0,
        zeroRatedTradingPremises = 0
      )

      "a Trading Premises has been added with a full year fee" in new Fixture {

        val variationResponse = testVariationResponse.copy(
          addedFullYearTradingPremises = 1
        )

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
        } thenReturn Future.successful(Some("12345"))

        when {
          TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
        } thenReturn Future.successful(CacheMap("", Map.empty))

        when {
          cache.getEntry[AmendVariationResponse](any())(any())
        } thenReturn Some(variationResponse)

        whenReady(TestSubmissionService.getVariation) {
          case Some((_, _, breakdownRows)) =>
            breakdownRows.head.label mustBe "confirmation.tradingpremises"
            breakdownRows.head.quantity mustBe 1
            breakdownRows.head.perItm mustBe Currency(tpFee)
            breakdownRows.head.total mustBe Currency(tpFee)
            breakdownRows.length mustBe 1
          case _ => false
        }
      }
      "a Trading Premises has been added with a half year fee" in new Fixture {
        val variationResponse = testVariationResponse.copy(
          halfYearlyTradingPremises = 1
        )

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
        } thenReturn Future.successful(Some("12345"))

        when {
          TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
        } thenReturn Future.successful(CacheMap("", Map.empty))

        when {
          cache.getEntry[AmendVariationResponse](any())(any())
        } thenReturn Some(variationResponse)

        whenReady(TestSubmissionService.getVariation) {
          case Some((_, _, breakdownRows)) =>
            breakdownRows.head.label mustBe "confirmation.tradingpremises.half"
            breakdownRows.head.quantity mustBe 1
            breakdownRows.head.perItm mustBe Currency(tpHalfFee)
            breakdownRows.head.total mustBe Currency(tpHalfFee)
            breakdownRows.length mustBe 1
          case _ => false
        }
      }
      "a Trading Premises has been added with a zero fee" in new Fixture {
        val variationResponse = testVariationResponse.copy(
          zeroRatedTradingPremises = 1
        )

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
        } thenReturn Future.successful(Some("12345"))

        when {
          TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
        } thenReturn Future.successful(CacheMap("", Map.empty))

        when {
          cache.getEntry[AmendVariationResponse](any())(any())
        } thenReturn Some(variationResponse)

        whenReady(TestSubmissionService.getVariation) {
          case Some((_, _, breakdownRows)) =>
            breakdownRows.head.label mustBe "confirmation.tradingpremises.zero"
            breakdownRows.head.quantity mustBe 1
            breakdownRows.head.perItm mustBe Currency(0)
            breakdownRows.head.total mustBe Currency(0)
            breakdownRows.length mustBe 1
          case _ => false
        }
      }
      "a Responsible Person has been added" in new Fixture {
        val variationResponse = testVariationResponse.copy(
          addedResponsiblePeople = 1
        )

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
        } thenReturn Future.successful(Some("12345"))

        when {
          TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
        } thenReturn Future.successful(CacheMap("", Map.empty))

        when {
          cache.getEntry[AmendVariationResponse](any())(any())
        } thenReturn Some(variationResponse)

        whenReady(TestSubmissionService.getVariation) {
          case Some((_, _, breakdownRows)) =>
            breakdownRows.head.label mustBe "confirmation.responsiblepeople"
            breakdownRows.head.quantity mustBe 1
            breakdownRows.head.perItm mustBe Currency(rpFee)
            breakdownRows.head.total mustBe Currency(rpFee)
            breakdownRows.length mustBe 1
          case _ => false
        }
      }
      "a Responsible Person Fit and Proper has been added" in new Fixture {
        val variationResponse = testVariationResponse.copy(
          addedResponsiblePeopleFitAndProper = 1
        )

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
        } thenReturn Future.successful(Some("12345"))

        when {
          TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
        } thenReturn Future.successful(CacheMap("", Map.empty))

        when {
          cache.getEntry[AmendVariationResponse](any())(any())
        } thenReturn Some(variationResponse)

        whenReady(TestSubmissionService.getVariation) {
          case Some((_, _, breakdownRows)) =>
            breakdownRows.head.label mustBe "confirmation.responsiblepeople"
            breakdownRows.head.quantity mustBe 1
            breakdownRows.head.perItm mustBe Currency(rpFee)
            breakdownRows.head.total mustBe Currency(rpFee)
            breakdownRows.length mustBe 2
            breakdownRows(1).label mustBe "confirmation.unpaidpeople"
            breakdownRows(1).quantity mustBe 1
            breakdownRows(1).perItm mustBe Currency(0 - rpFee)
            breakdownRows(1).total mustBe Currency(0 - rpFee)
            breakdownRows.length mustBe 2
          case _ => false
        }
      }
      "each of the categorised fees are in the response" in new Fixture {
        val variationResponse = testVariationResponse.copy(
          addedResponsiblePeople = 1,
          addedResponsiblePeopleFitAndProper = 1,
          addedFullYearTradingPremises = 1,
          halfYearlyTradingPremises = 1,
          zeroRatedTradingPremises = 1
        )

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
        } thenReturn Future.successful(Some("12345"))

        when {
          TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
        } thenReturn Future.successful(CacheMap("", Map.empty))

        when {
          cache.getEntry[AmendVariationResponse](any())(any())
        } thenReturn Some(variationResponse)

        whenReady(TestSubmissionService.getVariation) {
          case Some((_, _, breakdownRows)) =>

            breakdownRows.head.label mustBe "confirmation.responsiblepeople"
            breakdownRows.head.quantity mustBe 2
            breakdownRows.head.perItm mustBe Currency(rpFee)
            breakdownRows.head.total mustBe Currency(rpFee + rpFee)

            breakdownRows(1).label mustBe "confirmation.unpaidpeople"
            breakdownRows(1).quantity mustBe 1
            breakdownRows(1).perItm mustBe Currency(0 - rpFee)
            breakdownRows(1).total mustBe Currency(0 - rpFee)

            breakdownRows(2).label mustBe "confirmation.tradingpremises.zero"
            breakdownRows(2).quantity mustBe 1
            breakdownRows(2).perItm mustBe Currency(0)
            breakdownRows(2).total mustBe Currency(0)

            breakdownRows(3).label mustBe "confirmation.tradingpremises.half"
            breakdownRows(3).quantity mustBe 1
            breakdownRows(3).perItm mustBe Currency(tpHalfFee)
            breakdownRows(3).total mustBe Currency(tpHalfFee)

            breakdownRows.last.label mustBe "confirmation.tradingpremises"
            breakdownRows.last.quantity mustBe 1
            breakdownRows.last.perItm mustBe Currency(tpFee)
            breakdownRows.last.total mustBe Currency(tpFee)

          case _ => false
        }
      }
    }
  }
}
