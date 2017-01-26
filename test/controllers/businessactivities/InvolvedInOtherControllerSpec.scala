package controllers.businessactivities

import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities, InvolvedInOtherYes}
import models.businessmatching.{BusinessActivities => BMActivities, _}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.PrivateMethodTester
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class InvolvedInOtherControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures with PrivateMethodTester{

  trait Fixture extends AuthorisedFixture {
    self =>

     val controller = new InvolvedInOtherController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
       override implicit val statusService: StatusService = mock[StatusService]
     }
  }

  val emptyCache = CacheMap("", Map.empty)

  "InvolvedInOtherController" must {

    "on get display the is your involved in other page" in new Fixture {
      val mockCacheMap = mock[CacheMap]
      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))
      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(None)
      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("businessactivities.involved.other.title"))
    }


    "on get display the involved in other with pre populated data" in new Fixture {

      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(Some(BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))))

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching()))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include ("test")

    }

    "redirect to Page not found" when {
      "application is in variation mode" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(NOT_FOUND)
      }
    }

    "on get display the business type is AccountancyServices" in new Fixture {

      val businessMatching = BusinessMatching(
        activities = Some(BMActivities(Set(AccountancyServices)))
      )

      val mockCacheMap = mock[CacheMap]

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(businessMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.01"))

    }

    "on get display the business type is BillPaymentServices" in new Fixture {

      val businessMatching = BusinessMatching(
        activities = Some(BMActivities(Set(BillPaymentServices)))
      )

      val mockCacheMap = mock[CacheMap]

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(businessMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.02"))

    }

    "on get display the business type is EstateAgentBusinessService" in new Fixture {

      val businessMatching = BusinessMatching(
        activities = Some(BMActivities(Set(EstateAgentBusinessService)))
      )

      val mockCacheMap = mock[CacheMap]

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(businessMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.03"))

    }

    "on get display the business type is HighValueDealing" in new Fixture {

      val businessMatching = BusinessMatching(
        activities = Some(BMActivities(Set(HighValueDealing)))
      )

      val mockCacheMap = mock[CacheMap]

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(businessMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.04"))

    }

    "on get display the business type is MoneyServiceBusiness" in new Fixture {

      val businessMatching = BusinessMatching(
        activities = Some(BMActivities(Set(MoneyServiceBusiness)))
      )

      val mockCacheMap = mock[CacheMap]

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(businessMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.05"))

    }

    "on get display the business type is TrustAndCompanyServices" in new Fixture {

      val businessMatching = BusinessMatching(
        activities = Some(BMActivities(Set(TrustAndCompanyServices)))
      )

      val mockCacheMap = mock[CacheMap]

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(businessMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.06"))

    }

    "on get display the business type is TelephonePaymentService" in new Fixture {

      val businessMatching = BusinessMatching(
        activities = Some(BMActivities(Set(TelephonePaymentService)))
      )

      val mockCacheMap = mock[CacheMap]

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
        .thenReturn(None)

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(businessMatching))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("businessmatching.registerservices.servicename.lbl.07"))

    }


    "on post with valid data with option yes" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "involvedInOther" -> "true",
        "details" -> "test"
      )

      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
      (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ExpectedBusinessTurnoverController.get().url))
    }

    "on post with valid data with option no" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "involvedInOther" -> "false"
      )

      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ExpectedAMLSTurnoverController.get().url))
    }

    "on post with valid data with option no in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "involvedInOther" -> "false"
      )

      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "involvedInOther" -> "test"
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#involvedInOther]").html() must include(Messages("error.required.ba.involved.in.other"))
    }

    "on post with required field not filled" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "involvedInOther" -> "true",
          "details" -> ""
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#details]").html() must include(Messages("error.required.ba.involved.in.other.text"))
    }


    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "involvedInOther" -> "true",
        "details" -> "test"
      )

      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
       (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
       (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ExpectedBusinessTurnoverController.get(true).url))
    }
  }
}
