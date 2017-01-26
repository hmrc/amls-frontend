package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.responsiblepeople.{VATRegisteredNo, ResponsiblePeople}
import org.jsoup.Jsoup
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


class VATRegisteredControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new VATRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BusinessRegisteredForVATController" must {

    "on get display the registered for VAT page" in new Fixture {
      when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))
      val result = controller.get(1)(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("responsiblepeople.registeredforvat.title"))
    }


  "on get display the registered for VAT page with pre populated data" in new Fixture {

    when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(vatRegistered = Some(VATRegisteredNo))))))

    val result = controller.get(1)(request)
    status(result) must be(OK)

    val document = Jsoup.parse(contentAsString(result))
    document.select("input[value=false]").hasAttr("checked") must be(true)
  }

  "on post with valid data" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody(
      "registeredForVAT" -> "true",
      "vrnNumber" -> "123456789"
    )

    when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(vatRegistered = Some(VATRegisteredNo))))))

    when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
      (any(), any(), any())).thenReturn(Future.successful(emptyCache))

    val result = controller.post(1)(newRequest)
    status(result) must be(SEE_OTHER)
    redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.RegisteredForSelfAssessmentController.get(1).url))
  }

  "on post with invalid data" in new Fixture {

    val newRequest = request.withFormUrlEncodedBody(
      "registeredForVATYes" -> "1234567890"
    )

    val result = controller.post(1)(newRequest)
    status(result) must be(BAD_REQUEST)

    contentAsString(result) must include(Messages("error.required.atb.registered.for.vat"))
  }

   "on post with valid data in edit mode" in new Fixture {

     val newRequest = request.withFormUrlEncodedBody(
       "registeredForVAT" -> "true",
       "vrnNumber" -> "123456789"
     )

     when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
       (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

     when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
       (any(), any(), any())).thenReturn(Future.successful(emptyCache))

     val result = controller.post(1, true)(newRequest)
     status(result) must be(SEE_OTHER)
     redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.RegisteredForSelfAssessmentController.get(1, true).url))
   }
  }
}


