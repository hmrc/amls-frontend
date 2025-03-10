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

package controllers.declaration

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.declaration.AddPersonFormProvider
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany, Partnership, SoleProprietor, UnincorporatedBody}
import models.declaration.AddPerson
import models.declaration.release7.{Director, ExternalAccountant}
import models.status.{ReadyForRenewal, SubmissionReady, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.declaration.AddPersonView

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class AddPersonControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks {
    self =>

    val request             = addToken(authRequest)
    lazy val view           = inject[AddPersonView]
    val addPersonController = new AddPersonController(
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = mockStatusService,
      cc = mockMcc,
      formProvider = inject[AddPersonFormProvider],
      view = view
    )

    val emptyCache = Cache.empty

    val defaultReviewDetails = ReviewDetails(
      businessName = "",
      businessType = Some(LimitedCompany),
      businessAddress = Address(
        line_1 = "",
        line_2 = None,
        line_3 = None,
        line_4 = None,
        postcode = None,
        country = Country(
          name = "",
          code = ""
        )
      ),
      safeId = ""
    )

    val defaultBM = BusinessMatching(reviewDetails = Some(defaultReviewDetails))
    when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
      .thenReturn(Future.successful(Some(defaultBM)))
    when(addPersonController.dataCacheConnector.save[AddPerson](any(), any(), any())(any()))
      .thenReturn(Future.successful(emptyCache))
    mockApplicationStatus(SubmissionReady)
  }

  "AddPersonController" when {

    "get is called" must {
      "display the persons page" when {
        "status is pre-submission" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AddPersonController.post().url)
            .withFormUrlEncodedBody(
              "firstName"            -> "firstName",
              "lastName"             -> "lastName",
              "roleWithinBusiness[]" -> "ExternalAccountant"
            )

          val result = addPersonController.get()(requestWithParams)
          status(result)          must be(OK)
          contentAsString(result) must include(messages("submit.registration"))

        }

        "status is pending" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AddPersonController.post().url)
            .withFormUrlEncodedBody(
              "firstName"            -> "firstName",
              "lastName"             -> "lastName",
              "roleWithinBusiness[]" -> "ExternalAccountant"
            )

          mockApplicationStatus(SubmissionReadyForReview)

          val result = addPersonController.get()(requestWithParams)
          status(result)          must be(OK)
          contentAsString(result) must include(messages("submit.amendment.application"))
        }

        "status is ready for renewal" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AddPersonController.post().url)
            .withFormUrlEncodedBody(
              "firstName"            -> "firstName",
              "lastName"             -> "lastName",
              "roleWithinBusiness[]" -> "ExternalAccountant"
            )

          mockApplicationStatus(ReadyForRenewal(Some(LocalDate.now())))

          val result = addPersonController.get()(requestWithParams)
          status(result)          must be(OK)
          contentAsString(result) must include(messages("submit.renewal.application"))
        }
      }

      "on get display the persons page with blank fields" in new Fixture {

        val result = addPersonController.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=firstName]").`val`                   must be("")
        document.select("input[name=middleName]").`val`                  must be("")
        document.select("input[name=lastName]").`val`                    must be("")
        document.select("input[name=roleWithinBusiness][checked]").`val` must be("")
      }

      "on getWithAmendment display the persons page with blank fields" in new Fixture {

        val result = addPersonController.getWithAmendment()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=firstName]").`val`                   must be("")
        document.select("input[name=middleName]").`val`                  must be("")
        document.select("input[name=lastName]").`val`                    must be("")
        document.select("input[name=roleWithinBusiness][checked]").`val` must be("")
      }
    }

    "post is called" must {
      "redirect to a new place" when {
        "the role type selected is Director" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AddPersonController.post().url)
            .withFormUrlEncodedBody(
              "firstName"    -> "firstName",
              "lastName"     -> "lastName",
              "positions[0]" -> Director.toString
            )

          val result = addPersonController.post()(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(routes.RegisterResponsiblePersonController.get().url)
        }
      }

      "redirect to DeclarationController when all the mandatory parameters supplied" when {
        "status is pending" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AddPersonController.post().url)
            .withFormUrlEncodedBody(
              "firstName"    -> "firstName",
              "lastName"     -> "lastName",
              "positions[0]" -> ExternalAccountant.toString
            )

          mockApplicationStatus(SubmissionReadyForReview)

          val result = addPersonController.post()(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(routes.DeclarationController.getWithAmendment().url)
        }

        "status is pre-submission" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.AddPersonController.post().url)
            .withFormUrlEncodedBody(
              "firstName"    -> "firstName",
              "lastName"     -> "lastName",
              "positions[0]" -> ExternalAccountant.toString
            )

          val result = addPersonController.post()(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(routes.DeclarationController.get().url)
        }
      }

      "respond with BAD_REQUEST" when {
        "first name not supplied" in new Fixture {

          val firstNameMissingInRequest = FakeRequest(POST, routes.AddPersonController.post().url)
            .withFormUrlEncodedBody(
              "lastName"     -> "lastName",
              "positions[0]" -> ExternalAccountant.toString
            )

          val result = addPersonController.post()(firstNameMissingInRequest)
          status(result) must be(BAD_REQUEST)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.getElementById("firstName-error").text() must include("Enter your first name")
        }

        "last name not supplied" in new Fixture {

          val lastNameNissingInRequest = FakeRequest(POST, routes.AddPersonController.post().url)
            .withFormUrlEncodedBody(
              "firstName"    -> "firstName",
              "positions[0]" -> ExternalAccountant.toString
            )

          val result = addPersonController.post()(lastNameNissingInRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.getElementById("lastName-error").text() must include("Enter your last name")
        }

        "business type is LimitedCompany and position is not filled" in new Fixture {
          val rd = defaultReviewDetails.copy(businessType = Some(LimitedCompany))
          val bm = BusinessMatching(reviewDetails = Some(rd))
          when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(Some(bm)))

          val roleMissingInRequest = FakeRequest(POST, routes.AddPersonController.post().url)
            .withFormUrlEncodedBody(
              "firstName" -> "firstName",
              "lastName"  -> "lastName"
            )

          val result = addPersonController.post()(roleMissingInRequest)
          status(result) must be(BAD_REQUEST)
        }

        "business type is SoleProprietor and position is not filled" in new Fixture {
          val rd = defaultReviewDetails.copy(businessType = Some(SoleProprietor))
          val bm = BusinessMatching(reviewDetails = Some(rd))
          when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(Some(bm)))

          val roleMissingInRequest = FakeRequest(POST, routes.AddPersonController.post().url)
            .withFormUrlEncodedBody(
              "firstName" -> "firstName",
              "lastName"  -> "lastName"
            )

          val result = addPersonController.post()(roleMissingInRequest)
          status(result) must be(BAD_REQUEST)
        }

        "business type is Partnership and position is not filled" in new Fixture {
          val rd = defaultReviewDetails.copy(businessType = Some(Partnership))
          val bm = BusinessMatching(reviewDetails = Some(rd))
          when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(Some(bm)))

          val roleMissingInRequest = FakeRequest(POST, routes.AddPersonController.post().url)
            .withFormUrlEncodedBody(
              "firstName" -> "firstName",
              "lastName"  -> "lastName"
            )

          val result = addPersonController.post()(roleMissingInRequest)
          status(result) must be(BAD_REQUEST)
        }

        "business type is LPrLLP and position is not filled" in new Fixture {
          val rd = defaultReviewDetails.copy(businessType = Some(LPrLLP))
          val bm = BusinessMatching(reviewDetails = Some(rd))
          when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(Some(bm)))

          val roleMissingInRequest = FakeRequest(POST, routes.AddPersonController.post().url)
            .withFormUrlEncodedBody(
              "firstName" -> "firstName",
              "lastName"  -> "lastName"
            )

          val result = addPersonController.post()(roleMissingInRequest)
          status(result) must be(BAD_REQUEST)
        }

        "business type is UnincorporatedBody and position is not filled" in new Fixture {
          val rd = defaultReviewDetails.copy(businessType = Some(UnincorporatedBody))
          val bm = BusinessMatching(reviewDetails = Some(rd))
          when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(Some(bm)))

          val roleMissingInRequest = FakeRequest(POST, routes.AddPersonController.post().url)
            .withFormUrlEncodedBody(
              "firstName" -> "firstName",
              "lastName"  -> "lastName"
            )

          val result = addPersonController.post()(roleMissingInRequest)
          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }
}

class AddPersonControllerWithoutAmendmentSpec extends AmlsSpec with MockitoSugar with Injecting {

  val userId                 = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends DependencyMocks {
    self =>
    val request             = addToken(authRequest)
    lazy val view           = inject[AddPersonView]
    val addPersonController = new AddPersonController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = mockStatusService,
      cc = mockMcc,
      formProvider = inject[AddPersonFormProvider],
      view = view
    )
  }

  val emptyCache = Cache.empty

  "AddPersonController" when {
    "get is called" must {
      "on get display the persons page" when {
        "status is pending" in new Fixture {

          when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
            .thenReturn(Future.successful(None))

          mockApplicationStatus(SubmissionReadyForReview)

          val result = addPersonController.get()(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(
            messages("declaration.addperson.title") + " - " + messages("title.amls") + " - " + messages("title.gov")
          )

          contentAsString(result) must include(messages("submit.amendment.application"))
        }
      }
    }
  }
}
