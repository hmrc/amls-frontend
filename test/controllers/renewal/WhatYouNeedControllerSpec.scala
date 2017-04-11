package controllers.renewal

import models.registrationprogress.{Completed, NotStarted, Section}
import models.renewal.{InvolvedInOtherYes, Renewal}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.RenewalService
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class WhatYouNeedControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val renewalService = mock[RenewalService]

    val controller = new WhatYouNeedController(self.authConnector, renewalService)
  }
  "WhatYouNeedController" must {

    "get" must {

      "load the page" in new Fixture {

        when {
          renewalService.getSection(any(), any(), any())
        } thenReturn Future.successful(Section("renewal", NotStarted, Renewal().hasChanged, controllers.renewal.routes.SummaryController.get()))

        val result = controller.get(request)
        status(result) must be(OK)

        val pageTitle = Messages("title.wyn") + " - " +
          Messages("summary.renewal") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        contentAsString(result) must include(pageTitle)
      }

      "redirect to progress page if renewal has been started" in new Fixture {

        when {
          renewalService.getSection(any(), any(), any())
        } thenReturn Future.successful(Section("renewal", Completed, Renewal().hasChanged, controllers.renewal.routes.SummaryController.get()))

        val result = controller.get(request)
        status(result) must be(SEE_OTHER)

      }
    }
  }
}
