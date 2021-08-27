/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package filters

import connectors.{AuthenticatorConnector, KeystoreConnector}
import models.status.ConfirmationStatus
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AbstractController, BaseController, ControllerComponents, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class ConfirmationFilterSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with Results with BaseController {

  val keystore = mock[KeystoreConnector]
  val authenticator = mock[AuthenticatorConnector]

  override lazy val app = new GuiceApplicationBuilder()
    .overrides(bind[KeystoreConnector].to(keystore))
    .bindings(bind[AuthenticatorConnector].to(authenticator))
    .build()

  trait TestFixture {

    implicit val materializer = app.materializer

    val confirmationStatusResult = ConfirmationStatus(Some(true))

    Seq(keystore, authenticator).foreach(reset(_))

    when(keystore.resetConfirmation(any(), any())) thenReturn Future.successful(())

    when(keystore.confirmationStatus(any(), any())) thenReturn Future.successful(confirmationStatusResult)

    when(authenticator.refreshProfile(any(), any())) thenReturn Future.successful(HttpResponse(OK))

  }

  "The confirmation filter" must {

    "redirect to the landing controller when the submission has been set" in new TestFixture {

      val filter = app.injector.instanceOf[ConfirmationFilter]
      val rh = FakeRequest().withSession(("sessionId", "SOME_SESSION_ID"))
      val nextFilter = Action(Ok("success"))

      val result = filter(nextFilter)(rh).run()

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.LandingController.get.url)
      verify(keystore).resetConfirmation(any(), any())
    }

    "run the original request when the submission has not been set" in new TestFixture {

      override val confirmationStatusResult = ConfirmationStatus(None)

      val filter = app.injector.instanceOf[ConfirmationFilter]
      val rh = FakeRequest()
      val nextFilter = Action(Ok("success"))

      val result = filter(nextFilter)(rh).run()

      status(result) mustBe OK

    }

    "run the original request if the user is already on the landing page" in new TestFixture {

      val filter = app.injector.instanceOf[ConfirmationFilter]
      val rh = FakeRequest(GET, controllers.routes.LandingController.get.url)
      val nextFilter = Action(Ok("success"))

      val result = filter(nextFilter)(rh).run()

      status(result) mustBe OK

    }

    "run the original request if the user is already on the confirmation page" in new TestFixture {

      val filter = app.injector.instanceOf[ConfirmationFilter]
      val rh = FakeRequest(GET, controllers.routes.ConfirmationController.get.url)
      val nextFilter = Action(Ok("success"))

      val result = filter(nextFilter)(rh).run()

      status(result) mustBe OK

    }

    "excludes anything that isn't a page" in new TestFixture {

      val filter = app.injector.instanceOf[ConfirmationFilter]
      val rh = FakeRequest(GET, "/amls.js").withSession(("sessionId", "SOME_SESSION_ID"))
      val nextFilter = Action(Ok("success"))

      val result = filter(nextFilter)(rh).run()

      status(result) mustBe OK

    }

    "refresh the user's profile" in new TestFixture {

      val filter = app.injector.instanceOf[ConfirmationFilter]
      val rh = FakeRequest().withSession(("sessionId", "SOME_SESSION_ID"))
      val nextFilter = Action(Ok("success"))

      val result = filter(nextFilter)(rh).run()

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.LandingController.get.url)

      verify(authenticator).refreshProfile(any(), any())

    }

    "excludes partial matching urls" in new TestFixture {

      val filter = app.injector.instanceOf[ConfirmationFilter]
      val rh = FakeRequest(GET, "/confirmation/payment-complete?ref=979ereruioj").withSession(("sessionId", "SOME_SESSION_ID"))
      val nextFilter = Action(Ok("success"))

      val result = filter(nextFilter)(rh).run()

      status(result) mustBe OK
    }

  }

  override protected def controllerComponents: ControllerComponents = ???

}
