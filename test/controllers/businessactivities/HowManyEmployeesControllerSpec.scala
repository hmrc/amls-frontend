package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities, HowManyEmployees}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class HowManyEmployeesControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new HowManyEmployeesController {
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override val authConnector: AuthConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "HowManyEmployeesController" when {

    "get is called" must {
      "display the how many employees page with an empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=employeeCount]").`val` must be("")
        document.select("input[name=employeeCountAMLSSupervision]").`val` must be("")
      }

      "display the how many employees page with pre populated data" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(BusinessActivities(howManyEmployees = Some(HowManyEmployees("163", "17"))))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=employeeCount]").`val` must be("163")
        document.select("input[name=employeeCountAMLSSupervision]").`val` must be("17")

      }
    }

    "post is called" must {
      "on post with valid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "employeeCount" -> "456",
          "employeeCountAMLSSupervision" -> "123"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.TransactionRecordController.get().url))
      }

      "on post without data" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "employeeCount" -> "",
          "employeeCountAMLSSupervision" -> ""
        )
        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("span").html() must include(Messages("error.required.ba.employee.count1"))
      }


      "on post with data longer than field length of 11 permitted" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "employeeCount" -> "12345678912345"
        )
        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("span").html() must include(Messages("error.max.length.ba.employee.count"))
      }


      "on post with valid data when edit is true" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "employeeCount" -> "54321",
          "employeeCountAMLSSupervision" -> "12345"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val resultTrue = controller.post(true)(newRequest)
        status(resultTrue) must be(SEE_OTHER)
        redirectLocation(resultTrue) must be(Some(routes.SummaryController.get().url))

      }

      "on post with valid data when edit is false" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "employeeCount" -> "54321",
          "employeeCountAMLSSupervision" -> "12345"
        )

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val resultTrue = controller.post(false)(newRequest)
        status(resultTrue) must be(SEE_OTHER)
        redirectLocation(resultTrue) must be(Some(routes.TransactionRecordController.get().url))

      }
    }
  }

  it must {
    "use correct services" in new Fixture {
      BusinessFranchiseController.authConnector must be(AMLSAuthConnector)
      BusinessFranchiseController.dataCacheConnector must be(DataCacheConnector)
    }
  }
}
