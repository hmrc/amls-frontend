package controllers.renewal

import connectors.DataCacheConnector
import models.businessactivities._
import models.businessmatching.{BusinessActivities => Activities, _}
import models.renewal.AMLSTurnover
import models.renewal.AMLSTurnover.First
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class AMLSTurnoverControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]

    val controller = new AMLSTurnoverController(
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector
    )

    val mockCacheMap = mock[CacheMap]

    val businessMatching = BusinessMatching(
      activities = Some(Activities(Set.empty))
    )

    def model: Option[AMLSTurnover] = None

    when(mockDataCacheConnector.fetchAll(any(), any()))
      .thenReturn(Future.successful(Some(mockCacheMap)))

    when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
      .thenReturn(Some(businessMatching))

    when(mockCacheMap.getEntry[AMLSTurnover](eqTo(AMLSTurnover.key))(any()))
      .thenReturn(model)
  }

  val emptyCache = CacheMap("", Map.empty)

  "AMLSTurnoverController" must {

    "on get display the Turnover Expect In 12Months Related To AMLS page" in new Fixture {

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("renewal.turnover.title"))
    }

    "on get display the Role Within Business page with pre populated data" in new Fixture {

      override def model = Some(First)

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=01]").hasAttr("checked") must be(true)
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "AMLSTurnover" -> "01"
      )

      when(mockDataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(mockDataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.BusinessFranchiseController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "AMLSTurnover" -> "01"
      )

      when(mockDataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(mockDataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.renewal.routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      when(mockDataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(businessMatching)))

      val result = controller.post(true)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe BAD_REQUEST
      document.select(".amls-error-summary").size mustEqual 1
    }

    "on get display the business type is AccountancyServices" in new Fixture {

      val bMatching = BusinessMatching(
        activities = Some(Activities(Set(AccountancyServices)))
      )

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(mockDataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.01"))

    }

    "on get display the business type is BillPaymentServices" in new Fixture {

      val bMatching = BusinessMatching(
        activities = Some(Activities(Set(BillPaymentServices)))
      )

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(mockDataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.02"))

    }

    "on get display the business type is EstateAgentBusinessService" in new Fixture {

      val bMatching = BusinessMatching(
        activities = Some(Activities(Set(EstateAgentBusinessService)))
      )

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(mockDataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.03"))

    }

    "on get display the business type is HighValueDealing" in new Fixture {

      val bMatching = BusinessMatching(
        activities = Some(Activities(Set(HighValueDealing)))
      )

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(mockDataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.04"))

    }

    "on get display the business type is MoneyServiceBusiness" in new Fixture {

      val bMatching = BusinessMatching(
        activities = Some(Activities(Set(MoneyServiceBusiness)))
      )

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(mockDataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.05"))

    }

    "on get display the business type is TrustAndCompanyServices" in new Fixture {

      val bMatching = BusinessMatching(
        activities = Some(Activities(Set(TrustAndCompanyServices)))
      )

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(mockDataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.06"))

    }

    "on get display the business type is TelephonePaymentService" in new Fixture {

      val bMatching = BusinessMatching(
        activities = Some(Activities(Set(TelephonePaymentService)))
      )

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(mockDataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.07"))

    }
  }
}
