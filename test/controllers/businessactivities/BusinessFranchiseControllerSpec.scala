package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessactivities.{BusinessFranchiseYes, BusinessActivities}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.test.Helpers._
import play.api.i18n.Messages
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture
import scala.concurrent.Future

class BusinessFranchiseControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures{

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new BusinessFranchiseController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BusinessFranchiseController" when {

    "get is called" must {
      "on get display the is your business a franchise page" in new Fixture {
        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))
        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("businessactivities.businessfranchise.title"))

        val htmlValue = Jsoup.parse(contentAsString(result))

        htmlValue.getElementById("businessFranchise-true").hasAttr("checked") must be(false)
        htmlValue.getElementById("businessFranchise-false").hasAttr("checked") must be(false)
        htmlValue.select("input[name=franchiseName]").`val` must be("")
      }

      "on get display the is your business a franchise page with pre populated data" in new Fixture {
        when(controller.dataCacheConnector.fetch[BusinessActivities](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(BusinessActivities(businessFranchise = Some(BusinessFranchiseYes("test test"))))))
        val result = controller.get()(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))

        htmlValue.getElementById("businessFranchise-true").hasAttr("checked") must be(true)
        htmlValue.getElementById("businessFranchise-false").hasAttr("checked") must be(false)
        htmlValue.select("input[name=franchiseName]").`val` must be("test test")

      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {
        "edit is false and given valid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "businessFranchise" -> "true",
            "franchiseName" -> "test test"
          )

          when(controller.dataCacheConnector.fetch[BusinessActivities](any())
            (any(), any(), any())).thenReturn(Future.successful(None))

          when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controller.post(false)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.HowManyEmployeesController.get().url))
        }

        "edit is true and given valid data" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "businessFranchise" -> "true",
            "franchiseName" -> "test"
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

      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "businessFranchise" -> "test"
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("span").html() must include(Messages("error.required.ba.is.your.franchise"))
        }
      }
    }
  }

  it must {
    "use the correct services" in new Fixture {
      BusinessFranchiseController.authConnector must be(AMLSAuthConnector)
      BusinessFranchiseController.dataCacheConnector must be(DataCacheConnector)
    }
  }

}
