package controllers.renewal

import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities, InvolvedInOtherYes}
import models.businessmatching.{BusinessActivities => BMActivities, _}
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.PrivateMethodTester
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class InvolvedInOtherControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures with PrivateMethodTester {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val mockCacheMap = mock[CacheMap]

    val emptyCache = CacheMap("", Map.empty)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockStatusService = mock[StatusService]

    val controller = new InvolvedInOtherController(
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      statusService = mockStatusService
    )
  }

  "InvolvedInOtherController" must {

    "when get is called" must {
      "display the is your business involved in other activities page" in new Fixture {

        when(mockStatusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(None)
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(None)

        when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("renewal.involvedinother.title"))
      }


      "display the involved in other with pre populated data" in new Fixture {

        when(mockStatusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
          .thenReturn(Some(BusinessActivities(involvedInOther = Some(InvolvedInOtherYes("test")))))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching()))

        when(mockDataCacheConnector.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include("test")

      }

      "display the correct business type" when {
        "the business type is AccountancyServices" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(AccountancyServices)))
          )

          when(mockStatusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(None)
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.01"))

        }

        "the business type is BillPaymentServices" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(BillPaymentServices)))
          )

          when(mockStatusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(None)
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.02"))

        }

        "the business type is EstateAgentBusinessService" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(EstateAgentBusinessService)))
          )

          when(mockStatusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(None)
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.03"))

        }

        "the business type is HighValueDealing" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(HighValueDealing)))
          )

          when(mockStatusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(None)

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.04"))

        }

        "the business type is MoneyServiceBusiness" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(MoneyServiceBusiness)))
          )

          when(mockStatusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(None)
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.05"))

        }

        "the business type is TrustAndCompanyServices" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(TrustAndCompanyServices)))
          )

          when(mockStatusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(None)
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.06"))

        }

        "the business type is TelephonePaymentService" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(TelephonePaymentService)))
          )

          when(mockStatusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          when(mockCacheMap.getEntry[BusinessActivities](BusinessActivities.key))
            .thenReturn(None)
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))

          when(mockDataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.07"))

        }
      }

    }


    "when post is called" must {

      "redirect to BusinessTurnoverController with valid data with option yes" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "involvedInOther" -> "true",
          "details" -> "test"
        )

        when(mockDataCacheConnector.fetch[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(mockDataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.BusinessTurnoverController.get().url))
      }

      "redirect to AMLSTurnoverController with valid data with option no" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "involvedInOther" -> "false"
        )

        when(mockDataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(mockDataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.AMLSTurnoverController.get().url))
      }

      "redirect to SummaryController with valid data with option no in edit mode" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "involvedInOther" -> "false"
        )

        when(mockDataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(mockDataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))
      }

      "redirect to BusinessTurnoverController with valid data in edit mode" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "involvedInOther" -> "true",
          "details" -> "test"
        )

        when(mockDataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(mockDataCacheConnector.save[BusinessActivities](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.BusinessTurnoverController.get(true).url))
      }

      "respond with BAD_REQUEST" when {

        "with invalid data with business activities" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(AccountancyServices)))
          )

          when(mockDataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(businessMatching)))

          val newRequest = request.withFormUrlEncodedBody(
            "involvedInOther" -> "test"
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.01"))

          val document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#involvedInOther]").html() must include(Messages("error.required.ba.involved.in.other"))
        }

        "with invalid data without business activities" in new Fixture {

          when(mockDataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          val newRequest = request.withFormUrlEncodedBody(
            "involvedInOther" -> "test"
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

          val document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#involvedInOther]").html() must include(Messages("error.required.ba.involved.in.other"))
        }

        "with required field not filled with business activities" in new Fixture {

          val businessMatching = BusinessMatching(
            activities = Some(BMActivities(Set(AccountancyServices)))
          )

          when(mockDataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(businessMatching)))

          val newRequest = request.withFormUrlEncodedBody(
            "involvedInOther" -> "true",
            "details" -> ""
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("businessmatching.registerservices.servicename.lbl.01"))

          val document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#details]").html() must include(Messages("error.required.ba.involved.in.other.text"))
        }

      }

    }
  }
}
