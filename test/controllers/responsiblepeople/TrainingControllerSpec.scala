package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.responsiblepeople.{ResponsiblePeople, TrainingNo, TrainingYes}
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
import utils.AuthorisedFixture

import scala.concurrent.Future

class TrainingControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new TrainingController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }

    when(controller.dataCacheConnector.fetchAll(any(), any())).thenReturn(Future.successful(Some(CacheMap("testCacheMap", Map()))))
  }

  val emptyCache = CacheMap("", Map.empty)

  "TrainingController" must {

      "use correct services" in new Fixture {
        TrainingController.authConnector must be(AMLSAuthConnector)
        TrainingController.dataCacheConnector must be(DataCacheConnector)
      }

    "get" must {

      "load the page" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))
        val result = controller.get(RecordId)(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("responsiblepeople.training.title"))
      }
    }

    "on get display the page with pre populated data for the Yes Option" in new Fixture {
      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(training = Some(TrainingYes("I do not remember when I did the training")))))))
      val result = controller.get(RecordId)(request)
      status(result) must be(OK)
      contentAsString(result) must include ("I do not remember when I did the training")
    }


    "on get display the page with pre populated data with No Data for the information" in new Fixture {
      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(training = Some(TrainingNo))))))
      val result = controller.get(RecordId)(request)
      status(result) must be(OK)
      contentAsString(result) must not include ("I do not remember when I did the training")
    }


    "on post with valid data and training selected yes" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "training" -> "true",
        "information" -> "I do not remember when I did the training"
      )

      when(controller.dataCacheConnector.fetch[ResponsiblePeople](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[ResponsiblePeople](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(RecordId)(newRequest)
      status(result) must be(SEE_OTHER)
      //redirectLocation(result) must be(Some(routes.HowManyEmployeesController.get().url))
    }

    "on post with invalid data" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "training" -> "not a boolean value"
      )
      val result = controller.post(RecordId)(newRequest)
      status(result) must be(BAD_REQUEST)
    }


    "on post with valid data in edit mode" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "training" -> "true",
        "information" -> "I do not remember when I did the training"
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
