package services

import connectors.{AmlsConnector, DataCacheConnector}
import exceptions.NoEnrolmentException
import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businessactivities.{BusinessActivities => BusActivities}
import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessType.SoleProprietor
import models.businessmatching._
import models.confirmation.{BreakdownRow, Currency}
import models.estateagentbusiness.EstateAgentBusiness
import models.moneyservicebusiness.MoneyServiceBusiness
import models.renewal._
import models.responsiblepeople.{PersonName, ResponsiblePeople}
import models.tradingpremises.TradingPremises
import models.{AmendVariationResponse, Country, SubscriptionRequest, SubscriptionResponse}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
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

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.amounts.registration" -> 100))

  trait Fixture {

    val TestSubmissionService = new SubmissionService {
      override private[services] val cacheConnector = mock[DataCacheConnector]
      override private[services] val amlsConnector = mock[AmlsConnector]
      override private[services] val ggService = mock[GovernmentGatewayService]
      override private[services] val authEnrolmentsService = mock[AuthEnrolmentsService]
    }

    val rpFee: BigDecimal = 100
    val rpFeeWithRate: BigDecimal = 130
    val tpFee: BigDecimal = 115
    val tpFeeWithRate: BigDecimal = 125
    val tpHalfFee: BigDecimal = tpFee / 2
    val tpTotalFee: BigDecimal = tpFee + (tpHalfFee * 3)
    val totalFee: BigDecimal = rpFee + tpTotalFee

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
      fpFeeRate = None,
      premiseFee = 0,
      premiseFeeRate = None,
      totalFees = 0,
      paymentReference = ""
    )

    val subscriptionResponseWithFeeRate = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "amlsRef",
      registrationFee = 100,
      fpFee = Some(125.0),
      fpFeeRate = Some(130.0),
      premiseFee = 0,
      premiseFeeRate = Some(125.0),
      totalFees = 0,
      paymentReference = ""
    )

    val amendmentResponse = AmendVariationResponse(
      processingDate = "",
      etmpFormBundleNumber = "",
      registrationFee = 100,
      fpFee = None,
      fpFeeRate = None,
      premiseFee = 0,
      premiseFeeRate = None,
      totalFees = 100,
      paymentReference = Some("XA111123451111"),
      difference = Some(0)
    )

    val amendmentResponseWithRate = AmendVariationResponse(
      processingDate = "",
      etmpFormBundleNumber = "",
      registrationFee = 100,
      fpFee = Some(500),
      fpFeeRate = Some(250),
      premiseFee = 150,
      premiseFeeRate = Some(150),
      totalFees = 100,
      paymentReference = Some("XA111123451111"),
      difference = Some(0)
    )

    val variationResponse = AmendVariationResponse(
      processingDate = "",
      etmpFormBundleNumber = "",
      registrationFee = 100,
      fpFee = None,
      fpFeeRate = None,
      premiseFee = 0,
      premiseFeeRate = None,
      totalFees = 100,
      paymentReference = Some(""),
      difference = Some(0),
      addedResponsiblePeople = 0,
      addedFullYearTradingPremises = 0,
      halfYearlyTradingPremises = 0,
      zeroRatedTradingPremises = 0
    )

    val variationResponseWithRate = AmendVariationResponse(
      processingDate = "",
      etmpFormBundleNumber = "",
      registrationFee = 100,
      fpFee = None,
      fpFeeRate = Some(130),
      premiseFee = 0,
      premiseFeeRate = Some(125),
      totalFees = 100,
      paymentReference = Some(""),
      difference = Some(0),
      addedResponsiblePeople = 0,
      addedFullYearTradingPremises = 0,
      halfYearlyTradingPremises = 0,
      zeroRatedTradingPremises = 0
    )

    val safeId = "safeId"
    val amlsRegistrationNumber = "amlsRegNo"
    val businessType = SoleProprietor

    val reviewDetails = mock[ReviewDetails]
    val activities = mock[BusinessActivities]
    val businessMatching = mock[BusinessMatching]
    val cache = mock[CacheMap]

    when {
      reviewDetails.safeId
    } thenReturn safeId
    when {
      reviewDetails.businessType
    } thenReturn Some(businessType)

    when {
      businessMatching.reviewDetails
    } thenReturn Some(reviewDetails)
    when {
      businessMatching.activities
    } thenReturn Some(activities)
    when {
      activities.businessActivities
    } thenReturn Set[BusinessActivity]()

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

    "subscribe and enrol" in new Fixture {

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

    "submit amendment" in new Fixture {

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

    "submit variation" in new Fixture {

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

    "submit amendment returning submission data" in new Fixture {

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
        BreakdownRow("confirmation.tradingpremises", 1, 115, 0)
      )

      val response = Some(Some("XA111123451111"), Currency.fromBD(100), rows, Some(Currency.fromBD(0)))

      whenReady(TestSubmissionService.getAmendment) {
        result =>
          result must equal(response)
      }
    }

    "submit amendment returning submission data with dynamic fee rate" in new Fixture {

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        cache.getEntry[AmendVariationResponse](eqTo(AmendVariationResponse.key))(any())
      } thenReturn Some(amendmentResponseWithRate)

      when {
        cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
      } thenReturn Some(Seq(TradingPremises()))

      when {
        cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
      } thenReturn Some(Seq(ResponsiblePeople()))

      val rows = Seq(
        BreakdownRow("confirmation.submission", 1, 100, 100)
      ) ++ Seq(
        BreakdownRow("confirmation.responsiblepeople", 1, 250, 500)
      ) ++ Seq(
        BreakdownRow("confirmation.tradingpremises", 1, 150, 150)
      )

      val response = Some(Some("XA111123451111"), Currency.fromBD(100), rows, Some(Currency.fromBD(0)))

      whenReady(TestSubmissionService.getAmendment) {
        result =>
          result must equal(response)
      }
    }

    "not show negative fees for responsible people who have already been paid for" in new Fixture {

      val people = Seq(
        ResponsiblePeople(Some(PersonName("Unfit", Some("and"), "Unproper", None, None)), hasAlreadyPassedFitAndProper = Some(false)),
        ResponsiblePeople(Some(PersonName("Fit", Some("and"), "Proper", None, None)), hasAlreadyPassedFitAndProper = Some(true))
      )

      val amendResponseWithRPFees = amendmentResponse.copy(fpFee = Some(100))

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
      } thenReturn Some(Seq(TradingPremises()))

      when {
        cache.getEntry[AmendVariationResponse](eqTo(AmendVariationResponse.key))(any())
      } thenReturn Some(amendResponseWithRPFees)

      when {
        cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
      } thenReturn Some(people)

      val result = await(TestSubmissionService.getAmendment)

      whenReady(TestSubmissionService.getAmendment) {
        _ foreach {
          case (_, _, rows, _) =>
            val unpaidRow = rows.filter(_.label == "confirmation.unpaidpeople").head
            unpaidRow.perItm.value mustBe 0
            unpaidRow.total.value mustBe 0
        }
      }

    }

    "not include deleted premises in the amendment confirmation table" in new Fixture {

      val premises = Seq(
        TradingPremises(status = Some(StatusConstants.Deleted)),
        TradingPremises()
      )

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
      } thenReturn Some(premises)

      when {
        cache.getEntry[AmendVariationResponse](eqTo(AmendVariationResponse.key))(any())
      } thenReturn Some(amendmentResponse)

      when(cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())) thenReturn Some(Seq(ResponsiblePeople()))

      val result = await(TestSubmissionService.getAmendment)

      whenReady(TestSubmissionService.getAmendment) { result =>
        result foreach {
          case (_, _, rows, _) =>
            rows.filter(_.label == "confirmation.tradingpremises").head.quantity mustBe 1
        }
      }

    }

    "retrieve data from variation submission" in new Fixture {

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
      } thenReturn Some(variationResponse.copy(
        paymentReference = Some("12345"),
        addedResponsiblePeople = 1,
        addedFullYearTradingPremises = 1,
        halfYearlyTradingPremises = 3,
        zeroRatedTradingPremises = 1
      ))

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

    "not include responsible people in breakdown" when {

      "they have been deleted" in new Fixture {

        val people = Seq(
          ResponsiblePeople(Some(PersonName("Valid", None, "Person", None, None))),
          ResponsiblePeople(Some(PersonName("Deleted", None, "Person", None, None)), status = Some(StatusConstants.Deleted))
        )

        val amendResponseWithRPFees = amendmentResponse.copy(fpFee = Some(100))

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[AmendVariationResponse](eqTo(AmendVariationResponse.key))(any())
        } thenReturn Some(amendResponseWithRPFees)

        when(cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())) thenReturn Some(people)

        val result = await(TestSubmissionService.getAmendment)

        whenReady(TestSubmissionService.getAmendment)(_ foreach {
          case (_, _, rows, _) => rows.filter(_.label == "confirmation.responsiblepeople").head.quantity mustBe 1
        })

      }

      "there is no fee returned in a subscription response because business type is not msb or tcsp" in new Fixture {

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          cache.getEntry[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any())
        } thenReturn Some(subscriptionResponse)

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(Seq(ResponsiblePeople()))

        val result = await(TestSubmissionService.getSubscription)

        result match {
          case (_, _, rows) => rows foreach { row =>
            row.label must not equal "confirmation.responsiblepeople"
            row.label must not equal "confirmation.unpaidpeople"
          }
        }
      }

      "there is no fee returned in a amendment response because business type is not msb or tcsp" in new Fixture {

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          cache.getEntry[AmendVariationResponse](eqTo(AmendVariationResponse.key))(any())
        } thenReturn Some(amendmentResponse)

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(Seq(ResponsiblePeople()))

        val result = await(TestSubmissionService.getAmendment)

        result match {
          case Some((_, _, rows, _)) => rows foreach { row =>
            row.label must not equal "confirmation.responsiblepeople"
            row.label must not equal "confirmation.unpaidpeople"
          }
        }

      }

      "there is no fee returned in a variation response because business type is not msb or tcsp" in new Fixture {

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          cache.getEntry[AmendVariationResponse](eqTo(AmendVariationResponse.key))(any())
        } thenReturn Some(variationResponse.copy(fpFee = None))

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(Seq(ResponsiblePeople()))

        val result = await(TestSubmissionService.getVariation)

        result match {
          case Some((_, _, rows)) => rows foreach { row =>
            row.label must not equal "confirmation.responsiblepeople"
            row.label must not equal "confirmation.unpaidpeople"
          }
        }
      }

    }

    "notify user of subscription fees to pay in breakdown" when {

      "there is a Responsible People fee to pay" in new Fixture {

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          cache.getEntry[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any())
        } thenReturn Some(subscriptionResponse.copy(fpFee = Some(100)))

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(Seq(ResponsiblePeople()))

        val result = await(TestSubmissionService.getSubscription)

        result match {
          case (_, _, rows) => {
            rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
            rows.count(_.label.equals("confirmation.unpaidpeople")) must be(0)
          }
        }
      }

      "there is a Responsible People fee to pay and fpRate should be read dynamically" in new Fixture {

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          cache.getEntry[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any())
        } thenReturn Some(subscriptionResponseWithFeeRate)

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(Seq(ResponsiblePeople(), ResponsiblePeople()))

        val result = await(TestSubmissionService.getSubscription)

        case class Test(str: String)

        result match {
          case (_, _, rows) => {
            rows.head.label mustBe "confirmation.submission"
            rows.head.quantity mustBe 1
            rows.head.perItm mustBe Currency(rpFee)
            rows.head.total mustBe Currency(rpFee)

            rows(1).label mustBe "confirmation.responsiblepeople"
            rows(1).quantity mustBe 2
            rows(1).perItm mustBe Currency(rpFeeWithRate)
            rows(1).total mustBe Currency(125.0)

            rows.last.label mustBe "confirmation.tradingpremises"
            rows.last.quantity mustBe 1
            rows.last.perItm mustBe Currency(tpFeeWithRate)
            rows.last.total mustBe Currency(0)
            rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
            rows.count(_.label.equals("confirmation.unpaidpeople")) must be(0)
          }
        }
      }

      "the business type is MSB and there is not a Responsible Persons fee to pay from a subscription" in new Fixture {

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          cache.getEntry[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any())
        } thenReturn Some(subscriptionResponse)

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(Seq(ResponsiblePeople()))

        when {
          activities.businessActivities
        } thenReturn Set[BusinessActivity](models.businessmatching.MoneyServiceBusiness)

        val result = await(TestSubmissionService.getSubscription)

        result match {
          case (_, _, rows) => {
            rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
            rows.count(_.label.equals("confirmation.unpaidpeople")) must be(0)
          }
        }

      }

      "the business type is TCSP and there is not a Responsible Persons fee to pay from a subscription" in new Fixture {

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          cache.getEntry[SubscriptionResponse](eqTo(SubscriptionResponse.key))(any())
        } thenReturn Some(subscriptionResponse)

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(Seq(ResponsiblePeople()))

        when {
          activities.businessActivities
        } thenReturn Set[BusinessActivity](TrustAndCompanyServices)

        val result = await(TestSubmissionService.getSubscription)

        result match {
          case (_, _, rows) => {
            rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
            rows.count(_.label.equals("confirmation.unpaidpeople")) must be(0)
          }
        }

      }

    }

    "notify user of amendment fees to pay in breakdown" when {

      "there is a Responsible Persons fee to pay" in new Fixture {

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
        } thenReturn Some(variationResponse.copy(addedResponsiblePeople = 1))

        whenReady(TestSubmissionService.getVariation) {
          case Some((_, _, breakdownRows)) =>
            breakdownRows.head.label mustBe "confirmation.responsiblepeople"
            breakdownRows.head.quantity mustBe 1
            breakdownRows.head.perItm mustBe Currency(rpFee)
            breakdownRows.head.total mustBe Currency(rpFee)
            breakdownRows.length mustBe 1

            breakdownRows.count(row => row.label.equals("confirmation.unpaidpeople")) mustBe 0
          case _ => false
        }
      }

      "there is a Responsible Persons fee to pay with dynamic fpFeeRate" in new Fixture {

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
        } thenReturn Some(variationResponseWithRate.copy(addedResponsiblePeople = 1))

        whenReady(TestSubmissionService.getVariation) {
          case Some((_, _, breakdownRows)) =>
            breakdownRows.head.label mustBe "confirmation.responsiblepeople"
            breakdownRows.head.quantity mustBe 1
            breakdownRows.head.perItm mustBe Currency(rpFeeWithRate)
            breakdownRows.head.total mustBe Currency(rpFeeWithRate)
            breakdownRows.length mustBe 1

            breakdownRows.count(row => row.label.equals("confirmation.unpaidpeople")) mustBe 0
          case _ => false
        }
      }

      "the business type is MSB and there is not a Responsible Persons fee to pay from am amendment" in new Fixture {

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          cache.getEntry[AmendVariationResponse](eqTo(AmendVariationResponse.key))(any())
        } thenReturn Some(amendmentResponse)

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(Seq(ResponsiblePeople()))

        when {
          activities.businessActivities
        } thenReturn Set[BusinessActivity](models.businessmatching.MoneyServiceBusiness)

        val result = await(TestSubmissionService.getAmendment)

        result match {
          case Some((_, _, rows, _)) => {
            rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
            rows.count(_.label.equals("confirmation.unpaidpeople")) must be(0)
          }
        }

      }

      "the business type is TCSP and there is not a Responsible Persons fee to pay from am amendment" in new Fixture {

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          cache.getEntry[AmendVariationResponse](eqTo(AmendVariationResponse.key))(any())
        } thenReturn Some(amendmentResponse)

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(Seq(ResponsiblePeople()))

        when {
          activities.businessActivities
        } thenReturn Set[BusinessActivity](TrustAndCompanyServices)

        val result = await(TestSubmissionService.getAmendment)

        result match {
          case Some((_, _, rows, _)) => {
            rows.count(_.label.equals("confirmation.responsiblepeople")) must be(1)
            rows.count(_.label.equals("confirmation.unpaidpeople")) must be(0)
          }
        }

      }

    }

    "notify user of variation fees to pay" when {

      "a Trading Premises has been added with a full year fee" in new Fixture {

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
        } thenReturn Some(variationResponse.copy(addedFullYearTradingPremises = 1))

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
        } thenReturn Some(variationResponse.copy(halfYearlyTradingPremises = 1))

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
        } thenReturn Some(variationResponse.copy(zeroRatedTradingPremises = 1))

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

      "there is a Responsible Persons fee to pay" in new Fixture {

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
        } thenReturn Some(variationResponse.copy(addedResponsiblePeople = 1))

        whenReady(TestSubmissionService.getVariation) {
          case Some((_, _, breakdownRows)) =>
            breakdownRows.head.label mustBe "confirmation.responsiblepeople"
            breakdownRows.head.quantity mustBe 1
            breakdownRows.head.perItm mustBe Currency(rpFee)
            breakdownRows.head.total mustBe Currency(rpFee)
            breakdownRows.length mustBe 1

            breakdownRows.count(row => row.label.equals("confirmation.unpaidpeople")) mustBe 0
          case _ => false
        }
      }

      "the business type is MSB and there is not a Responsible Persons fee to pay from an variation" in new Fixture {

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          cache.getEntry[AmendVariationResponse](eqTo(AmendVariationResponse.key))(any())
        } thenReturn Some(variationResponse.copy(fpFee = None))

        when {
          cache.getEntry[AmendVariationResponse](any())(any())
        } thenReturn Some(variationResponse.copy(addedResponsiblePeopleFitAndProper = 1))

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(Seq(ResponsiblePeople()))

        when {
          activities.businessActivities
        } thenReturn Set[BusinessActivity](models.businessmatching.MoneyServiceBusiness)

        val result = await(TestSubmissionService.getVariation)

        result match {
          case Some((_, _, rows)) => {
            rows.count(_.label.equals("confirmation.responsiblepeople")) must be(0)
            rows.count(_.label.equals("confirmation.unpaidpeople")) must be(1)
          }
        }

      }

      "the business type is TCSP and there is not a Responsible Persons fee to pay from an variation" in new Fixture {

        when {
          TestSubmissionService.cacheConnector.fetchAll(any(), any())
        } thenReturn Future.successful(Some(cache))

        when {
          cache.getEntry[AmendVariationResponse](eqTo(AmendVariationResponse.key))(any())
        } thenReturn Some(variationResponse.copy(fpFee = None))

        when {
          cache.getEntry[AmendVariationResponse](any())(any())
        } thenReturn Some(variationResponse.copy(addedResponsiblePeopleFitAndProper = 1))

        when {
          cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
        } thenReturn Some(Seq(TradingPremises()))

        when {
          cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
        } thenReturn Some(Seq(ResponsiblePeople()))

        when {
          activities.businessActivities
        } thenReturn Set[BusinessActivity](TrustAndCompanyServices)

        val result = await(TestSubmissionService.getVariation)

        result match {
          case Some((_, _, rows)) => {
            rows.count(_.label.equals("confirmation.responsiblepeople")) must be(0)
            rows.count(_.label.equals("confirmation.unpaidpeople")) must be(1)
          }
        }

      }

      "each of the categorised fees are in the response" in new Fixture {

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
        } thenReturn Some(variationResponse.copy(
          addedResponsiblePeople = 1,
          addedResponsiblePeopleFitAndProper = 1,
          addedFullYearTradingPremises = 1,
          halfYearlyTradingPremises = 1,
          zeroRatedTradingPremises = 1
        ))

        whenReady(TestSubmissionService.getVariation) {
          case Some((_, _, breakdownRows)) =>

            breakdownRows.head.label mustBe "confirmation.responsiblepeople"
            breakdownRows.head.quantity mustBe 1
            breakdownRows.head.perItm mustBe Currency(rpFee)
            breakdownRows.head.total mustBe Currency(rpFee)

            breakdownRows(1).label mustBe "confirmation.unpaidpeople"
            breakdownRows(1).quantity mustBe 1
            breakdownRows(1).perItm mustBe Currency(0)
            breakdownRows(1).total mustBe Currency(0)

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

    "submit a renewal" in new Fixture {

      when {
        cache.getEntry[BusActivities](eqTo(BusActivities.key))(any())
      } thenReturn Some(BusActivities())

      when {
        cache.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any())
       } thenReturn Some(MoneyServiceBusiness())

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      } thenReturn Future.successful(Some(amlsRegistrationNumber))

      when {
        TestSubmissionService.cacheConnector.save[RenewalResponse](eqTo(RenewalResponse.key), any())(any(), any(), any())
      } thenReturn Future.successful(CacheMap("", Map.empty))

      when {
        TestSubmissionService.amlsConnector.renewal(any(), eqTo(amlsRegistrationNumber))(any(), any(), any())
      } thenReturn Future.successful(mock[RenewalResponse])

      val renewal = Renewal(
        turnover = Some(AMLSTurnover.First),
        businessTurnover = Some(BusinessTurnover.Second),
        msbThroughput = Some(MsbThroughput("02")),
        customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("Test", "T"))))),
        involvedInOtherActivities = Some(InvolvedInOtherNo)
      )

      val result = await(TestSubmissionService.renewal(renewal))

      val captor = ArgumentCaptor.forClass(classOf[SubscriptionRequest])
      verify(TestSubmissionService.amlsConnector).renewal(captor.capture(), any())(any(), any(), any())

      val submission = captor.getValue

      // The actual values of these are tested in renewals.models.Conversions
      submission.businessActivitiesSection mustBe defined
      submission.businessActivitiesSection.get.expectedAMLSTurnover mustBe defined
      submission.businessActivitiesSection.get.expectedBusinessTurnover mustBe defined
      submission.businessActivitiesSection.get.customersOutsideUK mustBe defined
      submission.businessActivitiesSection.get.involvedInOther mustBe defined
    }

  }
}
