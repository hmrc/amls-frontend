package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.responsiblepeople.{SaRegisteredYes, ResponsiblePeople}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class RegisteredForSelfAssessmentControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new RegisteredForSelfAssessmentController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "RegisteredForSelfAssessmentController" must {

      "use correct services" in new Fixture {
        RegisteredForSelfAssessmentController.authConnector must be(AMLSAuthConnector)
        RegisteredForSelfAssessmentController.dataCacheConnector must be(DataCacheConnector)
      }

    "get" must {

      "load the page" in new Fixture {
        when(controller.dataCacheConnector.fetch[ResponsiblePeople](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
        val result = controller.get(RecordId)(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("responsiblepeople.registeredforselfassessment.title"))
      }
    }

    "on get display the page with pre populated data" in new Fixture {
      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(saRegistered = Some(SaRegisteredYes("0123456789")))))))
      val result = controller.get(RecordId)(request)
      status(result) must be(OK)
      contentAsString(result) must include ("0123456789")
    }

    "on post with valid data" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "saRegistered" -> "true",
        "utrNumber" -> "0123456789"
      )

      when(controller.dataCacheConnector.fetch[ResponsiblePeople](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[ResponsiblePeople](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ExperienceTrainingController.get(RecordId).url))
    }


    "on post with invalid data" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "saRegistered" -> "test"
      )

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document  = Jsoup.parse(contentAsString(result))
      document.select("span").html() must include(Messages("error.required.sa.registration"))
    }


    "on post with valid data in edit mode" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "saRegistered" -> "true",
        "utrNumber" -> "0123456789"
      )

      when(controller.dataCacheConnector.fetch[ResponsiblePeople](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[ResponsiblePeople](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(RecordId, true)(newRequest)
      status(result) must be(SEE_OTHER)
      //redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }
  }
}
