package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.responsiblepeople.{BeneficialOwner, Positions, ResponsiblePeople}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class DetailedAnswersControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new DetailedAnswersController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "DetailedAnswersController" when {

    "get is called - from the yourAnswers controller" must {
      "respond with OK and show the detailed answers page with a 'confirm and continue' link to the YourAnswersController" in new Fixture {

        val model = ResponsiblePeople(None, None)
        when(controller.dataCache.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))

        val result = controller.get(1, true)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        contentAsString(result) must include(Messages("responsiblepeople.detailed_answers.title"))
        contentAsString(result) must include("/anti-money-laundering/responsible-people/your-answers")
        contentAsString(result) must not include "/anti-money-laundering/responsible-people/check-your-answers"
      }
    }

    "get is called - NOT from the yourAnswers controller" must {
      "respond with OK and show the detailed answers page with a 'confirm and continue' link to the YourAnswersController" in new Fixture {

        val model = ResponsiblePeople(None, None)
        when(controller.dataCache.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))

        val result = controller.get(1, false)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        contentAsString(result) must include(Messages("responsiblepeople.detailed_answers.title"))
        contentAsString(result) must not include "/anti-money-laundering/responsible-people/your-answers"
        contentAsString(result) must include("/anti-money-laundering/responsible-people/check-your-answers")
      }
    }

    "get is called from any location" when {
      "section data is available" must {
        "respond with OK and show the detailed answers page with the correct title" in new Fixture {

          val model = ResponsiblePeople(None, None)
          when(controller.dataCache.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(model))))

          val result = controller.get(1, false)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          contentAsString(result) must include(Messages("responsiblepeople.detailed_answers.title"))
        }

        "respond with OK and show the detailed answers page with a correctly formatted responsiblePerson startDate" in new Fixture {

          private val testStartDate = new LocalDate(1999,1,1)

          val model = ResponsiblePeople(positions = Some(Positions(Set(BeneficialOwner),Some(testStartDate))))
          when(controller.dataCache.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(model))))

          val result = controller.get(1, false)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          contentAsString(result) must include("1 January 1999")
        }
      }

      "respond with SEE_OTHER and show the registration progress page" when {
        "section data is unavailable" in new Fixture {
          when(controller.dataCache.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(None))
          val result = controller.get(1, false)(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
        }

      }
    }
  }

  it must {
    "use the correct services" in new Fixture {
      DetailedAnswersController.authConnector must be(AMLSAuthConnector)
      DetailedAnswersController.dataCache must be(DataCacheConnector)
    }
  }
}
