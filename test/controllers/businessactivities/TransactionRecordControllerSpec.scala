package controllers.businessactivities

import connectors.DataCacheConnector
import models.businessactivities._
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class TransactionRecordControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new TransactionRecordController {

      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "TransactionRecordController" when {

    "get is called" must {
      "load the Customer Record Page with an empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))
        page.getElementById("isRecorded-true").hasAttr("checked") must be(false)
        page.getElementById("isRecorded-false").hasAttr("checked") must be(false)
        page.getElementById("transactions-01").hasAttr("checked") must be(false)
        page.getElementById("transactions-02").hasAttr("checked") must be(false)
        page.getElementById("transactions-03").hasAttr("checked") must be(false)
        page.getElementById("name").`val` must be("")
      }

      "pre-populate the Customer Record Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(BusinessActivities(
            transactionRecord = Some(TransactionRecordYes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("test"))))
          ))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val page = Jsoup.parse(contentAsString(result))
        page.getElementById("isRecorded-true").hasAttr("checked") must be(true)

      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {
        "given valid data not in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isRecorded" -> "true",
            "transactions[0]" -> "01",
            "transactions[1]" -> "02"
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any())
            (any(), any(), any())).thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.IdentifySuspiciousActivityController.get().url))
        }

        "given valid data in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isRecorded" -> "true",
            "transactions[0]" -> "01",
            "transactions[1]" -> "02",
            "transactions[2]" -> "03",
            "name" -> "test"
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any())
            (any(), any(), any())).thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post(true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get().url))
        }
      }

      "respond with BAD_REQUEST when given invalid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "transactions[0]" -> "01",
            "transactions[1]" -> "02"
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any())
            (any(), any(), any())).thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

        }

      }
    }
  }

}
