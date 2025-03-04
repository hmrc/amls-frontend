/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.tcsp

import controllers.actions.SuccessfulAuthAction
import models.tcsp._
import models.tcsp.ProvidedServices._
import models.tcsp.TcspTypes._
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.OptionValues
import play.api.test.Helpers._
import play.api.test.Injecting
import services.businessmatching.ServiceFlow
import utils.tcsp.CheckYourAnswersHelper
import utils.{AmlsSpec, DependencyMocks}
import views.html.tcsp.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class SummaryControllerSpec extends AmlsSpec with Injecting {

  trait Fixture extends DependencyMocks {
    self =>

    val request                       = addToken(authRequest)
    implicit val ec: ExecutionContext = inject[ExecutionContext]

    val defaultProvidedServices      = ProvidedServices(Set(PhonecallHandling, Other("other service")))
    val defaultServicesOfAnotherTCSP = ServicesOfAnotherTCSPYes(Some("12345678"))
    val mockTcsp                     = mock[Tcsp]

    val defaultCompanyServiceProviders = TcspTypes(Set(RegisteredOfficeEtc, CompanyFormationAgent))

    val model      = Tcsp(
      Some(defaultCompanyServiceProviders),
      Some(OnlyOffTheShelfCompsSoldYes),
      Some(ComplexCorpStructureCreationNo),
      Some(defaultProvidedServices),
      Some(true),
      Some(defaultServicesOfAnotherTCSP)
    )
    lazy val view  = app.injector.instanceOf[CheckYourAnswersView]
    val controller = new SummaryController(
      mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      mock[ServiceFlow],
      mockStatusService,
      cc = mockMcc,
      cyaHelper = inject[CheckYourAnswersHelper],
      view = view,
      error = errorView
    )

    when {
      controller.statusService.isPreSubmission(any(), any(), any())(any(), any(), any())
    } thenReturn Future.successful(false)
  }

  "Get" must {
    "load the summary page when section data is complete" in new Fixture {

      mockCacheFetch[Tcsp](Some(model))

      val result = controller.get()(request)
      status(result) must be(OK)
    }

    "redirect to registration progress if data is incomplete" in new Fixture with OptionValues {

      mockCacheFetch[Tcsp](Some(model.copy(providedServices = None)))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result).value mustBe controllers.routes.RegistrationProgressController.get().url
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      mockCacheFetch[Tcsp](None)

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }
  }

  "POST" must {
    "redirect to RegistrationProgressController" when {
      "the model has been updated with hasAccepted" in new Fixture {

        mockCacheFetch[Tcsp](Some(model))
        mockCacheSave[Tcsp]

        val result = controller.post()(request)

        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))

        verify(controller.dataCache).save[Tcsp](any(), any(), eqTo(model.copy(hasAccepted = true)))(any())

      }
    }
  }
}
