/*
 * Copyright 2017 HM Revenue & Customs
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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.declaration.{AddPerson, Director}
import models.status.{SubmissionReady, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class AddPersonControllerSpec extends GenericTestHelper with MockitoSugar {

  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]

  override lazy val app = FakeApplication(additionalConfiguration = Map(
    "microservice.services.feature-toggle.release7" -> false
  ))

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val addPersonController = new AddPersonController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "AddPersonController" must {

    "use the correct services" in new Fixture {
      AddPersonController.dataCacheConnector must be(DataCacheConnector)
      AddPersonController.authConnector must be(AMLSAuthConnector)
    }

    "on get display the persons page" when {
      "status is pending" in new Fixture {
        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LPrLLP),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))

        when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(businessMatching)))

        when(addPersonController.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        val result = addPersonController.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title() must be (Messages("declaration.addperson.amendment.title") + " - " + Messages("title.amls") + " - " + Messages("title.gov"))

        contentAsString(result) must include(Messages("submit.amendment.application"))
      }
      "status is pre-submission" in new Fixture {

        when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(addPersonController.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReady))

        val result = addPersonController.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title() must be (Messages("declaration.addperson.title") + " - " + Messages("title.amls") + " - " + Messages("title.gov"))

        contentAsString(result) must include(Messages("submit.registration"))
      }
    }

    "on get display the persons page with blank fields" in new Fixture {

      when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(addPersonController.statusService.getStatus(any(),any(),any()))
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

      when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(addPersonController.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = addPersonController.getWithAmendment()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=firstName]").`val` must be("")
      document.select("input[name=middleName]").`val` must be("")
      document.select("input[name=lastName]").`val` must be("")
      document.select("input[name=roleWithinBusiness][checked]").`val` must be("")
    }

    "must pass on post with all the mandatory parameters supplied" when {
      "status is pending" in new Fixture {

        val requestWithParams = request.withFormUrlEncodedBody(
          "firstName" -> "firstName",
          "lastName" -> "lastName",
          "roleWithinBusiness" -> "01"
        )

        when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        when(addPersonController.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        val result = addPersonController.post()(requestWithParams)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.DeclarationController.getWithAmendment().url)
      }
      "status is pre-submission" in new Fixture {

        val requestWithParams = request.withFormUrlEncodedBody(
          "firstName" -> "firstName",
          "lastName" -> "lastName",
          "roleWithinBusiness" -> "01"
        )

        when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        when(addPersonController.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReady))

        val result = addPersonController.post()(requestWithParams)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.DeclarationController.get().url)
      }
    }

    "must fail on post if first name not supplied" in new Fixture {

      val firstNameMissingInRequest = request.withFormUrlEncodedBody(
        "lastName" -> "lastName",
        "roleWithinBusiness" -> "01"
      )

      when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(addPersonController.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = addPersonController.post()(firstNameMissingInRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#firstName]").html() must include("This field is required")
    }

    "must fail on post if last name not supplied" in new Fixture {

      val lastNameNissingInRequest = request.withFormUrlEncodedBody(
        "firstName" -> "firstName",
        "roleWithinBusiness" -> "01"
      )

      when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(addPersonController.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = addPersonController.post()(lastNameNissingInRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#lastName]").html() must include("This field is required")
    }

    "must fail on post if roleWithinBusiness not supplied" in new Fixture {

      val roleMissingInRequest = request.withFormUrlEncodedBody(
        "firstName" -> "firstName",
        "lastName" -> "lastName"
      )

      when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(addPersonController.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = addPersonController.post()(roleMissingInRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#roleWithinBusiness]").html() must include("This field is required")

    }

  }

}

class AddPersonRelease7Spec extends GenericTestHelper with MockitoSugar {

  override lazy val app = FakeApplication(additionalConfiguration = Map(
    "microservice.services.feature-toggle.release7" -> true
  ))

  trait Fixture extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)

    val addPersonController = new AddPersonController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }

    val emptyCache = CacheMap("", Map.empty)

    when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())
      (any(), any(), any())).thenReturn(Future.successful(Some(mock[BusinessMatching])))

  }

  "must pass on post with all the mandatory parameters supplied" when {
    "status is pending" in new Fixture {

      val requestWithParams = request.withFormUrlEncodedBody(
        "firstName" -> "firstName",
        "lastName" -> "lastName",
        "roleWithinBusiness[]" -> "BeneficialShareholder"
      )

      when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(addPersonController.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReadyForReview))

      val result = addPersonController.post()(requestWithParams)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) mustBe Some(routes.DeclarationController.getWithAmendment().url)
    }
    "status is pre-submission" in new Fixture {

      val requestWithParams = request.withFormUrlEncodedBody(
        "firstName" -> "firstName",
        "lastName" -> "lastName",
        "roleWithinBusiness[]" -> "BeneficialShareholder"
      )

      when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      when(addPersonController.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = addPersonController.post()(requestWithParams)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) mustBe Some(routes.DeclarationController.get().url)
    }
  }

}

class AddPersonControllerWithoutAmendmentSpec extends GenericTestHelper with MockitoSugar {

  override lazy val app = FakeApplication(additionalConfiguration = Map(
    "microservice.services.feature-toggle.amendments" -> false,
    "microservice.services.feature-toggle.release7" -> false)
  )

  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val addPersonController = new AddPersonController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "AddPersonController" must {
    "on get display the persons page" when {
      "status is pending" in new Fixture {

        when(addPersonController.dataCacheConnector.fetch[BusinessMatching](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(addPersonController.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        val result = addPersonController.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title() must be (Messages("declaration.addperson.title") + " - " + Messages("title.amls") + " - " + Messages("title.gov"))

        contentAsString(result) must include(Messages("submit.registration"))
      }
    }
    "must pass on post with all the mandatory parameters supplied" when {
      "status is pending" in new Fixture {

        val requestWithParams = request.withFormUrlEncodedBody(
          "firstName" -> "firstName",
          "lastName" -> "lastName",
          "roleWithinBusiness" -> "01"
        )

        when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        when(addPersonController.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        val result = addPersonController.post()(requestWithParams)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.DeclarationController.get().url)
      }
    }
  }

}

class AddPersonControllerWithoutAmendmentSpecRelease7 extends GenericTestHelper with MockitoSugar {

  override lazy val app = FakeApplication(additionalConfiguration = Map(
    "microservice.services.feature-toggle.amendments" -> false,
    "microservice.services.feature-toggle.release7" -> true)
  )

  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val addPersonController = new AddPersonController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "AddPersonController (release 7)" must {

    "must pass on post with all the mandatory parameters supplied" when {
      "status is pending" in new Fixture {

        val requestWithParams = request.withFormUrlEncodedBody(
          "firstName" -> "firstName",
          "lastName" -> "lastName",
          "roleWithinBusiness[]" -> "BeneficialShareholder"
        )

        when(addPersonController.dataCacheConnector.save[AddPerson](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        when(addPersonController.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        val result = addPersonController.post()(requestWithParams)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.DeclarationController.get().url)
      }
    }
  }

}
