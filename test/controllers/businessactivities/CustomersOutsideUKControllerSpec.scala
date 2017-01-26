package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
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

class CustomersOutsideUKControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new CustomersOutsideUKController {

      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "CustomersOutsideUKController" must {

    "use correct services" in new Fixture {
      CustomersOutsideUKController.authConnector must be(AMLSAuthConnector)
      CustomersOutsideUKController.dataCacheConnector must be(DataCacheConnector)
    }

    "load the Customer Record Page" in new Fixture  {

      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))

      val pageTitle = Messages("businessactivities.customer.outside.uk.title") + " - " +
        Messages("summary.businessactivities") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      document.title() mustBe pageTitle
    }

    "pre-populate the Customer outside UK Page" in new Fixture  {

      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(BusinessActivities(customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB")))))))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=isOutside]").size mustEqual 2
      document.select("input[name=isOutside][checked]").`val` mustEqual "true"
      document.select("select[name=countries[0]] > option[value=GB]").hasAttr("selected") must be(true)

    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "isOutside" -> "true",
        "countries[0]" -> "GB",
        "countries[1]" -> "US"
      )

      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.TransactionRecordController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "isOutside" -> "true",
        "countries[0]" -> "GB",
        "countries[1]" -> "US"
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
        "isOutside" -> "true",
        "countries[0]" -> "",
        "countries[1]" -> ""
      )

      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#countries]").html() must include(Messages("error.required.country.name"))
    }

    "on post with invalid data1" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "isOutside" -> ""
      )

      when(controller.dataCacheConnector.fetch[BusinessActivities](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessActivities](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#isOutside]").html() must include(Messages("error.required.ba.select.country"))
    }
  }

}
