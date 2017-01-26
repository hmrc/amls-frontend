package controllers.businessactivities

import connectors.DataCacheConnector
import models.Country
import models.businessactivities._
import models.businessmatching.{BusinessActivities => BMBusinessActivities, _}
import models.status.{SubmissionDecisionApproved, NotCompleted}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new SummaryController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService: StatusService = mock[StatusService]
    }
  }

  "Get" must {

    val mockCacheMap = mock[CacheMap]

    val completeModel = BusinessActivities(
      involvedInOther = Some(BusinessActivitiesValues.DefaultInvolvedInOther),
      expectedBusinessTurnover = Some(BusinessActivitiesValues.DefaultBusinessTurnover),
      expectedAMLSTurnover = Some(BusinessActivitiesValues.DefaultAMLSTurnover),
      businessFranchise = Some(BusinessActivitiesValues.DefaultBusinessFranchise),
      transactionRecord = Some(BusinessActivitiesValues.DefaultTransactionRecord),
      customersOutsideUK = Some(BusinessActivitiesValues.DefaultCustomersOutsideUK),
      ncaRegistered = Some(BusinessActivitiesValues.DefaultNCARegistered),
      accountantForAMLSRegulations = Some(BusinessActivitiesValues.DefaultAccountantForAMLSRegulations),
      riskAssessmentPolicy = Some(BusinessActivitiesValues.DefaultRiskAssessments),
      howManyEmployees = Some(BusinessActivitiesValues.DefaultHowManyEmployees),
      identifySuspiciousActivity = Some(BusinessActivitiesValues.DefaultIdentifySuspiciousActivity),
      whoIsYourAccountant = Some(BusinessActivitiesValues.DefaultWhoIsYourAccountant),
      taxMatters = Some(BusinessActivitiesValues.DefaultTaxMatters),
      hasChanged = false
    )

    val bmBusinessActivities = Some(BMBusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService)))

    "load the summary page when section data is available" in new Fixture {

      val model = BusinessActivities(None)
      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCache.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(activities = bmBusinessActivities)))

      when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
        .thenReturn(Some(model))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {
      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCache.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(activities = bmBusinessActivities)))

      when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
        .thenReturn(None)


      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }

    "hide edit link for involved in other, turnover expected from activities and amls turnover expected page" when {
      "application in variation mode" in new Fixture {
        when(controller.dataCache.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(activities = bmBusinessActivities)))

        when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
          .thenReturn(Some(completeModel))


        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementsByTag("section").get(0).getElementsByTag("a").hasClass("change-answer") must be(false)
        document.getElementsByTag("section").get(1).getElementsByTag("a").hasClass("change-answer") must be(false)
        document.getElementsByTag("section").get(2).getElementsByTag("a").hasClass("change-answer") must be(false)
      }
    }

    "show edit link" when {
      "application not in variation mode" in new Fixture {
        when(controller.dataCache.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(activities = bmBusinessActivities)))

        when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
          .thenReturn(Some(completeModel))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementsByTag("section").get(0).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(1).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(2).getElementsByTag("a").hasClass("change-answer") must be(true)
      }
    }

    "pre load Business matching business activities data in " +
      "'How much total net profit does your business expect in the next 12 months, from the following activities?'" in new Fixture {

      when(controller.dataCache.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(activities = bmBusinessActivities)))

      when(mockCacheMap.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
        .thenReturn(Some(completeModel))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      val listElement = document.getElementsByTag("section").get(2).getElementsByClass("list-bullet").get(0)
      listElement.children().size() must be(bmBusinessActivities.fold(0)(x => x.businessActivities.size))

    }
  }
}

object BusinessActivitiesValues {
  val DefaultFranchiseName = "DEFAULT FRANCHISE NAME"
  val DefaultSoftwareName = "DEFAULT SOFTWARE"
  val DefaultBusinessTurnover = ExpectedBusinessTurnover.First
  val DefaultAMLSTurnover = ExpectedAMLSTurnover.First
  val DefaultInvolvedInOtherDetails = "DEFAULT INVOLVED"
  val DefaultInvolvedInOther = InvolvedInOtherYes(DefaultInvolvedInOtherDetails)
  val DefaultBusinessFranchise = BusinessFranchiseYes(DefaultFranchiseName)
  val DefaultTransactionRecord = TransactionRecordYes(Set(Paper, DigitalSoftware(DefaultSoftwareName)))
  val DefaultCustomersOutsideUK = CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))
  val DefaultNCARegistered = NCARegistered(true)
  val DefaultAccountantForAMLSRegulations = AccountantForAMLSRegulations(true)
  val DefaultRiskAssessments = RiskAssessmentPolicyYes(Set(PaperBased))
  val DefaultHowManyEmployees = HowManyEmployees("5","4")
  val DefaultWhoIsYourAccountant = WhoIsYourAccountant(
    "Accountant's name",
    Some("Accountant's trading name"),
    UkAccountantsAddress("address1", "address2", Some("address3"), Some("address4"), "POSTCODE")
  )
  val DefaultIdentifySuspiciousActivity = IdentifySuspiciousActivity(true)
  val DefaultTaxMatters = TaxMatters(false)
}
