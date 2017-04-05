package controllers.renewal

import connectors.DataCacheConnector
import models.Country
import models.businessmatching.{BusinessActivities => BMBusinessActivities, _}
import models.renewal._
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val mockCacheMap = mock[CacheMap]

    val emptyCache = CacheMap("", Map.empty)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockRenewalService = mock[RenewalService]

    val controller = new SummaryController(
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      renewalService = mockRenewalService
    )

  }

    val mockCacheMap = mock[CacheMap]

    val bmBusinessActivities = Some(BMBusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService)))

  "Get" must {

    "load the summary page when there is data in the renewal" in new Fixture {

      when(mockDataCacheConnector.fetchAll(any(),any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(mockCacheMap.getEntry[Renewal](Renewal.key))
        .thenReturn(Some(Renewal(Some(models.renewal.InvolvedInOtherYes("test")))))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(activities = bmBusinessActivities)))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to the renewal progress page when section data is unavailable" in new Fixture {
      when(mockDataCacheConnector.fetchAll(any(),any()))
        .thenReturn(Future.successful(Some(emptyCache)))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }

    "pre load Business matching business activities data in " +
      "'How much total net profit does your business expect in the next 12 months, from the following activities?'" in new Fixture {
      when(mockDataCacheConnector.fetchAll(any(),any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(activities = bmBusinessActivities)))
      when(mockCacheMap.getEntry[Renewal](Renewal.key))
        .thenReturn(Some(
          Renewal(
            Some(models.renewal.InvolvedInOtherYes("test")),
            Some(BusinessTurnover.First),
            Some(AMLSTurnover.First),
            Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
            Some(PercentageOfCashPaymentOver15000.First),
            Some(ReceiveCashPayments(Some(PaymentMethods(true,true,Some("other"))))),
            Some(MsbThroughput("01")),
            false)))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      val listElement = document.getElementsByTag("section").get(2).getElementsByClass("list-bullet").get(0)
      listElement.children().size() must be(bmBusinessActivities.fold(0)(x => x.businessActivities.size))

    }
  }
}
