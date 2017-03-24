package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.responsiblepeople.{PersonName, ResponsiblePeople, VATRegisteredNo}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future


class VATRegisteredControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val controller = new VATRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  val pageTitle = Messages("responsiblepeople.registeredforvat.title", "firstname lastname") + " - " +
    Messages("summary.responsiblepeople") + " - " +
    Messages("title.amls") + " - " + Messages("title.gov")
  val personName = Some(PersonName("firstname", None, "lastname", None, None))

  "BusinessRegisteredForVATController" when {

    "get is called" must {
      "on get display the registered for VAT page" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))
        val result = controller.get(1)(request)
        status(result) must be(OK)
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title must be(pageTitle)
      }


      "on get display the registered for VAT page with pre populated data" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName,vatRegistered = Some(VATRegisteredNo))))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must be(pageTitle)
        document.select("input[value=false]").hasAttr("checked") must be(true)
      }
    }

    "when post is called" must {
      "respond with BAD_REQUEST when given invalid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "registeredForVATYes" -> "1234567890",
          "personName" -> "Person Name"
        )
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = personName)))))

        val result = controller.post(1)(newRequest)
        status(result) must be(BAD_REQUEST)
        val document: Document = Jsoup.parse(contentAsString(result))
        document.title must be(pageTitle)

        contentAsString(result) must include(Messages("error.required.rp.registered.for.vat", "firstname lastname"))
      }

      "when given valid data and edit = false redirect to the RegisteredForSelfAssessmentController" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "registeredForVAT" -> "true",
          "vrnNumber" -> "123456789",
          "personName" -> "Person Name"
        )

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(vatRegistered = Some(VATRegisteredNo))))))

        when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.RegisteredForSelfAssessmentController.get(1).url))
      }


      "when given valid data and edit = true redirect to the RegisteredForSelfAssessmentController" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "registeredForVAT" -> "true",
          "vrnNumber" -> "123456789",
          "personName" -> "Person Name"
        )

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

        when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post(1, true)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url))
      }
    }
  }
}


