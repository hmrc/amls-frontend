/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.UUID

import connectors.DataCacheConnector
import forms.{EmptyForm, InvalidForm}
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany, Partnership, SoleProprietor, UnincorporatedBody}
import models.declaration.AddPerson
import models.status.{ReadyForRenewal, SubmissionReady, SubmissionReadyForReview}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future


class AddPersonControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)

    val addPersonController = new AddPersonController (
      dataCacheConnector = mock[DataCacheConnector],
      authConnector = self.authConnector,
      statusService = mock[StatusService]
    )

    val emptyCache = CacheMap("", Map.empty)

    val bm1 = BusinessMatching(
      reviewDetails = Some(
        ReviewDetails(
          businessName = "",
          businessType = Some(LimitedCompany),
          businessAddress = Address (
            line_1 = "",
            line_2 = "",
            line_3 = None,
            line_4 = None,
            postcode = None,
            country = Country(
              name = "",
              code = ""
            )),
          safeId=""
        )
      )
    )
    when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(bm1)))
  }

  "AddPersonController" when {

    "get is called" must {
      "display the persons page" when {
        "status is pre-submission" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "firstName" -> "firstName",
            "lastName" -> "lastName",
            "roleWithinBusiness[]" -> "ExternalAccountant"
          )

          when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = addPersonController.get()(requestWithParams)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("submit.registration"))

        }

        "status is pending" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "firstName" -> "firstName",
            "lastName" -> "lastName",
            "roleWithinBusiness[]" -> "ExternalAccountant"
          )

          when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = addPersonController.get()(requestWithParams)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("submit.amendment.application"))
        }

        "status is ready for renewal" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "firstName" -> "firstName",
            "lastName" -> "lastName",
            "roleWithinBusiness[]" -> "ExternalAccountant"
          )

          when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(ReadyForRenewal(Some(new LocalDate))))

          val result = addPersonController.get()(requestWithParams)
          status(result) must be(OK)
          contentAsString(result) must include(Messages("submit.renewal.application"))
        }


      }

      "on get display the persons page with blank fields" in new Fixture {

        when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(addPersonController.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReady))

        val result = addPersonController.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=firstName]").`val` must be("")
        document.select("input[name=middleName]").`val` must be("")
        document.select("input[name=lastName]").`val` must be("")
        document.select("input[name=roleWithinBusiness][checked]").`val` must be("")
      }


      "on getWithAmendment display the persons page with blank fields" in new Fixture {

        when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(addPersonController.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReady))

        val result = addPersonController.getWithAmendment()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=firstName]").`val` must be("")
        document.select("input[name=middleName]").`val` must be("")
        document.select("input[name=lastName]").`val` must be("")
        document.select("input[name=roleWithinBusiness][checked]").`val` must be("")
      }
    }

    "post is called" must {
      "redirect to a new place" when {
        "the role type selected is Director" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "firstName" -> "firstName",
            "lastName" -> "lastName",
            "positions" -> "02"
          )

          when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = addPersonController.post()(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(routes.RegisterResponsiblePersonController.get().url)
        }
      }

      "redirect to DeclarationController when all the mandatory parameters supplied" when {
        "status is pending" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "firstName" -> "firstName",
            "lastName" -> "lastName",
            "positions" -> "08"
          )

          when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = addPersonController.post()(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(routes.DeclarationController.getWithAmendment().url)
        }

        "status is pre-submission" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "firstName" -> "firstName",
            "lastName" -> "lastName",
            "positions" -> "08"
          )

          when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = addPersonController.post()(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(routes.DeclarationController.get().url)
        }
      }

      "respond with BAD_REQUEST" when {
        "first name not supplied" in new Fixture {

          val firstNameMissingInRequest = request.withFormUrlEncodedBody(
            "lastName" -> "lastName",
            "positions" -> "08"
          )

          when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = addPersonController.post()(firstNameMissingInRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#firstName]").html() must include("This field is required")
        }

        "last name not supplied" in new Fixture {

          val lastNameNissingInRequest = request.withFormUrlEncodedBody(
            "firstName" -> "firstName",
            "positions" -> "08"
          )

          when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = addPersonController.post()(lastNameNissingInRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#lastName]").html() must include("This field is required")
        }

        "business type is LimitedCompany and position is not filled" in new Fixture {
          val bm = BusinessMatching(
            reviewDetails = Some(
              ReviewDetails(
                businessName = "",
                businessType = Some(LimitedCompany),
                businessAddress = Address(
                  line_1 = "",
                  line_2 = "",
                  line_3 = None,
                  line_4 = None,
                  postcode = None,
                  country = Country(
                    name = "",
                    code = ""
                  )),
                safeId = ""
              )
            )
          )
          when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(bm)))

          val roleMissingInRequest = request.withFormUrlEncodedBody(
            "firstName" -> "firstName",
            "lastName" -> "lastName"
          )

          when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = addPersonController.post()(roleMissingInRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#positions]").html() must include("Select if you are a beneficial shareholder, a director, an external accountant, a nominated officer, or other")
        }

        "business type is SoleProprietor and position is not filled" in new Fixture {
          val bm = BusinessMatching(
            reviewDetails = Some(
              ReviewDetails(
                businessName = "",
                businessType = Some(SoleProprietor),
                businessAddress = Address(
                  line_1 = "",
                  line_2 = "",
                  line_3 = None,
                  line_4 = None,
                  postcode = None,
                  country = Country(
                    name = "",
                    code = ""
                  )),
                safeId = ""
              )
            )
          )
          when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(bm)))

          val roleMissingInRequest = request.withFormUrlEncodedBody(
            "firstName" -> "firstName",
            "lastName" -> "lastName"
          )

          when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = addPersonController.post()(roleMissingInRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#positions]").html() must include("Select if you are an external accountant, a nominated officer, a sole proprietor or other")
        }

        "business type is Partnership and position is not filled" in new Fixture {
          val bm = BusinessMatching(
            reviewDetails = Some(
              ReviewDetails(
                businessName = "",
                businessType = Some(Partnership),
                businessAddress = Address(
                  line_1 = "",
                  line_2 = "",
                  line_3 = None,
                  line_4 = None,
                  postcode = None,
                  country = Country(
                    name = "",
                    code = ""
                  )),
                safeId = ""
              )
            )
          )
          when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(bm)))

          val roleMissingInRequest = request.withFormUrlEncodedBody(
            "firstName" -> "firstName",
            "lastName" -> "lastName"
          )

          when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = addPersonController.post()(roleMissingInRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#positions]").html() must include("Select if you are an external accountant, a nominated officer, a partner or other")
        }

        "business type is LPrLLP and position is not filled" in new Fixture {
          val bm = BusinessMatching(
            reviewDetails = Some(
              ReviewDetails(
                businessName = "",
                businessType = Some(LPrLLP),
                businessAddress = Address(
                  line_1 = "",
                  line_2 = "",
                  line_3 = None,
                  line_4 = None,
                  postcode = None,
                  country = Country(
                    name = "",
                    code = ""
                  )),
                safeId = ""
              )
            )
          )
          when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(bm)))

          val roleMissingInRequest = request.withFormUrlEncodedBody(
            "firstName" -> "firstName",
            "lastName" -> "lastName"
          )

          when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = addPersonController.post()(roleMissingInRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#positions]").html() must include("Select if you are a designated member, an external accountant, a nominated officer, or other")
        }

        "business type is UnincorporatedBody and position is not filled" in new Fixture {
          val bm = BusinessMatching(
            reviewDetails = Some(
              ReviewDetails(
                businessName = "",
                businessType = Some(UnincorporatedBody),
                businessAddress = Address(
                  line_1 = "",
                  line_2 = "",
                  line_3 = None,
                  line_4 = None,
                  postcode = None,
                  country = Country(
                    name = "",
                    code = ""
                  )),
                safeId = ""
              )
            )
          )
          when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(bm)))

          val roleMissingInRequest = request.withFormUrlEncodedBody(
            "firstName" -> "firstName",
            "lastName" -> "lastName"
          )

          when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = addPersonController.post()(roleMissingInRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#positions]").html() must include("Select if you are an external accountant, a nominated officer, or other")
        }

        "throw an exception if business type is not defined" in new Fixture {
          val invalidForm = InvalidForm(Map.empty, Seq.empty)

          a[IllegalArgumentException] must be thrownBy {
            addPersonController.updateFormErrors(invalidForm, None)
          }
        }
      }
    }
  }
}

class AddPersonControllerWithoutAmendmentSpec extends AmlsSpec with MockitoSugar {

  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val addPersonController = new AddPersonController (
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      statusService = mock[StatusService]
    )
  }

  val emptyCache = CacheMap("", Map.empty)

  "AddPersonController" when {
    "get is called" must {
      "on get display the persons page" when {
        "status is pending" in new Fixture {

          when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())
            (any(), any(), any())).thenReturn(Future.successful(None))

          when(addPersonController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = addPersonController.get()(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.title() must be(Messages("declaration.addperson.title") + " - " + Messages("title.amls") + " - " + Messages("title.gov"))

          contentAsString(result) must include(Messages("submit.amendment.application"))
        }
      }
    }
  }
}
