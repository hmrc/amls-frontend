package controllers.businessactivities


import models.businessactivities.ExpectedAMLSTurnover.First
import models.businessactivities._
import models.status.{NotCompleted, SubmissionDecisionApproved}
import connectors.DataCacheConnector
import models.businessmatching.{BusinessActivities => Activities, _}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class ExpectedAMLSTurnoverControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ExpectedAMLSTurnoverController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService: StatusService = mock[StatusService]
    }

    val cache = mock[CacheMap]

    val businessMatching = BusinessMatching(
      activities = Some(Activities(Set.empty))
    )

    def model: Option[BusinessActivities] = None

    when(controller.statusService.getStatus(any(), any(), any()))
      .thenReturn(Future.successful(NotCompleted))

    when(controller.dataCacheConnector.fetchAll(any(), any()))
      .thenReturn(Future.successful(Some(cache)))

    when(cache.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
      .thenReturn(Some(businessMatching))

    when(cache.getEntry[BusinessActivities](eqTo(BusinessActivities.key))(any()))
      .thenReturn(model)
  }

  val emptyCache = CacheMap("", Map.empty)

  "ExpectedAMLSTurnoverController" must {

    "on get display the Turnover Expect In 12Months Related To AMLS page" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("businessactivities.turnover.title"))
    }

    "on get display the Role Within Business page with pre populated data" in new Fixture {

      override def model = Some(BusinessActivities(expectedAMLSTurnover = Some(First)))
      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=01]").hasAttr("checked") must be(true)
    }

    "redirect to Page not found" when {
      "application is in variation mode" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(NOT_FOUND)
      }
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "expectedAMLSTurnover" -> "01"
      )

      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.BusinessFranchiseController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "expectedAMLSTurnover" -> "01"
      )

      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessactivities.routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      when(controller.dataCacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
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

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.01"))

    }

    "on get display the business type is BillPaymentServices" in new Fixture {

      val bMatching = BusinessMatching(
        activities = Some(Activities(Set(BillPaymentServices)))
      )

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.02"))

    }

    "on get display the business type is EstateAgentBusinessService" in new Fixture {

      val bMatching = BusinessMatching(
        activities = Some(Activities(Set(EstateAgentBusinessService)))
      )

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.03"))

    }

    "on get display the business type is HighValueDealing" in new Fixture {

      val bMatching = BusinessMatching(
        activities = Some(Activities(Set(HighValueDealing)))
      )

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.04"))

    }

    "on get display the business type is MoneyServiceBusiness" in new Fixture {

      val bMatching = BusinessMatching(
        activities = Some(Activities(Set(MoneyServiceBusiness)))
      )

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.05"))

    }

    "on get display the business type is TrustAndCompanyServices" in new Fixture {

      val bMatching = BusinessMatching(
        activities = Some(Activities(Set(TrustAndCompanyServices)))
      )

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.06"))

    }

    "on get display the business type is TelephonePaymentService" in new Fixture {

      val bMatching = BusinessMatching(
        activities = Some(Activities(Set(TelephonePaymentService)))
      )

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(bMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.07"))

    }
  }
}
