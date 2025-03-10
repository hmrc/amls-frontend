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

package controllers.deregister

import connectors.{AmlsConnector, DataCacheConnector}
import controllers.actions.SuccessfulAuthAction
import models.ReadStatusResponse
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.deregister.DeregistrationReason
import models.registrationdetails.RegistrationDetails
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.Helpers._
import play.api.test.Injecting
import services.{AuthEnrolmentsService, StatusService}
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.deregister.DeregistrationConfirmationView

import scala.concurrent.Future

class DeregistrationConfirmationControllerSpec extends AmlsSpec with Injecting {

  trait TestFixture extends AuthorisedFixture {
    self =>

    val credentialId: String = SuccessfulAuthAction.credentialId

    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)

    val amlsConnector: AmlsConnector                 = mock[AmlsConnector]
    val authEnrolmentsService: AuthEnrolmentsService = mock[AuthEnrolmentsService]
    val dataCacheConnector: DataCacheConnector       = mock[DataCacheConnector]
    val statusService: StatusService                 = mock[StatusService]

    lazy val view = inject[DeregistrationConfirmationView]

    lazy val controller = new DeregistrationConfirmationController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = statusService,
      enrolmentService = authEnrolmentsService,
      cc = mockMcc,
      view = view
    )(
      dataCacheConnector = dataCacheConnector,
      amlsConnector = amlsConnector
    )
  }

  "when entering to the page it displays confirmation page" in new TestFixture {
    when(dataCacheConnector.fetch[DeregistrationReason](eqTo(credentialId), eqTo(DeregistrationReason.key))(any()))
      .thenReturn(Future.successful(Some(DeregistrationReason.OutOfScope)))

    private val amlsRefrence = "AMLS-Reference-number"

    when(authEnrolmentsService.amlsRegistrationNumber(any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(amlsRefrence)))

    private val readStatusResponse: ReadStatusResponse = mock[ReadStatusResponse]
    when(readStatusResponse.safeId).thenReturn(Some("safeIf"))

    when(statusService.getReadStatus(any[String](), any())(any(), any()))
      .thenReturn(Future.successful(readStatusResponse))
    private val companyName: String = "Company Name LTD"

    val registrationDetails = mock[RegistrationDetails]
    when(registrationDetails.companyName).thenReturn(companyName)
    when(amlsConnector.registrationDetails(any, any)(any, any)).thenReturn(Future.successful(registrationDetails))

    when(dataCacheConnector.fetch[BusinessMatching](any(), eqTo(BusinessMatching.key))(any())).thenReturn(
      Future.successful(
        Some(
          BusinessMatching(
            reviewDetails = Some(ReviewDetails(companyName, None, mock[Address], ""))
          )
        )
      )
    )

    val result: Future[Result] = controller.get()(request)
    status(result) must be(OK)

    private val content: String = contentAsString(result)

    content must include("You have deregistered")
    content must include(amlsRefrence)
    content must include(companyName)

    val document: Document = Jsoup.parse(content)
    document.select("h1").text mustBe "You have deregistered" withClue "ensure the page displays right content"
  }
}
