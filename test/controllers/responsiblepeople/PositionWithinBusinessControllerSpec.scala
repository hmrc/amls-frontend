package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class PositionWithinBusinessControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new PositionWithinBusinessController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  val RecordId = 1

  "PositionWithinBusinessController" must {

    "on get()" must {

      "display position within the business page" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must include(Messages("responsiblepeople.position_within_business.title"))
        document.select("input[value=01]").hasAttr("checked") must be(false)
        document.select("input[value=02]").hasAttr("checked") must be(false)
        document.select("input[value=03]").hasAttr("checked") must be(false)
        document.select("input[value=04]").hasAttr("checked") must be(false)
        document.select("input[value=05]").hasAttr("checked") must be(false)
        document.select("input[value=06]").hasAttr("checked") must be(false)

      }

      "Prepopulate form with a single saved data" in new Fixture {

        val positions = Positions(Set(BeneficialOwner))
        val responsiblePeople = ResponsiblePeople(positions = Some(positions))

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title must include(Messages("responsiblepeople.position_within_business.title"))
        document.select("input[value=01]").hasAttr("checked") must be(true)
        document.select("input[value=02]").hasAttr("checked") must be(false)
        document.select("input[value=03]").hasAttr("checked") must be(false)
        document.select("input[value=04]").hasAttr("checked") must be(false)
        document.select("input[value=05]").hasAttr("checked") must be(false)
        document.select("input[value=06]").hasAttr("checked") must be(false)
      }

      "Prepopulate form with multiple saved data" in new Fixture {

        val positions = Positions(Set(Director, InternalAccountant))
        val responsiblePeople = ResponsiblePeople(positions = Some(positions))

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title must include(Messages("responsiblepeople.position_within_business.title"))
        document.select("input[value=01]").hasAttr("checked") must be(false)
        document.select("input[value=02]").hasAttr("checked") must be(true)
        document.select("input[value=03]").hasAttr("checked") must be(true)
        document.select("input[value=04]").hasAttr("checked") must be(false)
        document.select("input[value=05]").hasAttr("checked") must be(false)
        document.select("input[value=06]").hasAttr("checked") must be(false)
      }
    }

    "submit with valid data as a partnership" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("positions" -> "05")

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.VATRegisteredController.get(RecordId).url))
    }

    "submit with valid data as a sole proprietor" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("positions" -> "06")

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.VATRegisteredController.get(RecordId).url))
    }

    "submit with all other valid data types" in new Fixture {

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      //TODO: Update to Does this person have previous experience in these activities?
      for (i <- 1 to 4) {
        val newRequest = request.withFormUrlEncodedBody("positions" -> s"0$i")
        val result = controller.post(RecordId)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.SummaryController.get().url))
      }
    }

    "submit with mixture of data types selected" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "positions" -> "06",
        "positions" -> "01"
      )

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.VATRegisteredController.get(RecordId).url))

    }

    "fail submission on empty string" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("positionWithinBusiness" -> "")

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#positions]").html() must include(Messages("error.required.positionWithinBusiness"))
    }

    "fail submission on invalid string" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("positionWithinBusiness" -> "10")

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#positions]").html() must include(Messages("error.required.positionWithinBusiness"))
    }

    "submit with valid data with edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody("positions" -> "05")

      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(RecordId, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.SummaryController.get().url))
    }
  }
}
