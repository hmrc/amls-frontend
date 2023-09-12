/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.responsiblepeople.address.CurrentAddressNonUKFormProvider
import models.businessactivities.BusinessActivities
import models.businessdetails.BusinessDetails
import models.businessmatching.BusinessActivity.BillPaymentServices
import models.businessmatching.{BusinessMatching, BusinessActivities => BMActivities}
import models.declaration.AddPerson
import models.declaration.release7.RoleWithinBusinessRelease7
import models.responsiblepeople.TimeAtAddress.{SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReadyForReview}
import models.{Country, ViewResponse}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.{AutoCompleteService, StatusService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.responsiblepeople.address.CurrentAddressNonUKView

import scala.collection.JavaConverters._
import scala.concurrent.Future

class CurrentAddressNonUKControllerSpec extends AmlsSpec with Injecting {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    implicit val hc = HeaderCarrier()
    val mockDataCacheConnector = mock[DataCacheConnector]
    val RecordId = 1

    val auditConnector = mock[AuditConnector]
    val autoCompleteService = mock[AutoCompleteService]
    val statusService = mock[StatusService]

    val viewResponse = ViewResponse(
      "",
      businessMatchingSection = BusinessMatching(
        activities = Some(BMActivities(
          Set(BillPaymentServices)
        ))
      ),
      businessDetailsSection = BusinessDetails(),
      bankDetailsSection = Seq.empty,
      businessActivitiesSection = BusinessActivities(),
      eabSection = None,
      aspSection = None,
      tcspSection = None,
      responsiblePeopleSection = None,
      tradingPremisesSection = None,
      msbSection = None,
      hvdSection = None,
      ampSection = None,
      supervisionSection = None,
      aboutYouSection = AddPerson("", None, "", RoleWithinBusinessRelease7(Set.empty))
    )
    lazy val view = inject[CurrentAddressNonUKView]
    val currentAddressController = new CurrentAddressNonUKController (
      dataCacheConnector = mockDataCacheConnector,
      auditConnector = auditConnector,
      authAction = SuccessfulAuthAction,
      statusService = statusService,
      autoCompleteService = autoCompleteService,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[CurrentAddressNonUKFormProvider],
      view = view,
      error = errorView
    )

    when {
      auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(Success)

    when {
      autoCompleteService.formOptionsExcludeUK
    } thenReturn Seq(
      SelectItem(Some("US"), "United States"),
      SelectItem(Some("ES"), "Spain")
    )
  }

  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99

  "CurrentAddressNonUKController" when {

    val pageTitle = messages("responsiblepeople.wherepersonlivescountry.title", "firstname lastname") + " - " +
      messages("summary.responsiblepeople") + " - " +
      messages("title.amls") + " - " + messages("title.gov")

    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {

      "respond with NOT_FOUND when called with an index that is out of bounds" in new Fixture {
        val responsiblePeople = ResponsiblePerson()

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(40)(request)
        status(result) must be(NOT_FOUND)
      }

      "display the address non uk page when no existing data in mongoCache" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must be(pageTitle)
        document.select("input[name=addressLine1]").`val` must be("")
        document.select("input[name=addressLine2]").`val` must be("")
        document.select("input[name=addressLine3]").`val` must be("")
        document.select("input[name=addressLine4]").`val` must be("")
        document.select("input[name=country]").`val` must be("")
      }

      "display the current home address with non-UK fields populated" in new Fixture {

        val nonukAddress = PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain", "ES"))
        val additionalAddress = ResponsiblePersonCurrentAddress(nonukAddress, Some(SixToElevenMonths))
        val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=addressLine1]").`val` must be("Line 1")
        document.select("input[name=addressLine2]").`val` must be("Line 2")
        document.select("input[name=addressLine3]").`val` must be("")
        document.select("input[name=addressLine4]").`val` must be("")
        document.select("select[name=country] > option[value=ES]").hasAttr("selected") must be(true)
      }
    }

    "post is called" must {
      "redirect to TimeAtAddressController" when {
        "all the mandatory non-UK parameters are supplied" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.CurrentAddressNonUKController.post(1).url)
          .withFormUrlEncodedBody(
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "country" -> "ES"
          )
          val ukAddress = PersonAddressNonUK("Line 1", "Line 2", Some("Line 3"), None, Country("Spain", "ES"))
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any(), any())
          } thenReturn Future.successful(Some(viewResponse))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.TimeAtCurrentAddressController.get(RecordId).url))

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "Line 1"
              d.detail("addressLine2") mustBe "Line 2"
              d.detail("country") mustBe "Spain"
          }
        }
      }

      "redirect to CurrentAddressDateOfChangeController" when {
        "address changed and in approved state" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.CurrentAddressNonUKController.post(1).url)
          .withFormUrlEncodedBody(
            "addressLine1" -> "New line 1",
            "addressLine2" -> "New line 2",
            "country" -> "PL"
          )
          val ukAddress = PersonAddressNonUK("Line 1", "Line 2", Some("Line 3"), None, Country("Spain", "ES"))
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history), lineId = Some(1))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any(), any())
          } thenReturn Future.successful(Some(viewResponse.copy(responsiblePeopleSection = Some(Seq(responsiblePeople)))))

          val result = currentAddressController.post(RecordId, true)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.get(RecordId, true).url))

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "New line 1"
              d.detail("addressLine2") mustBe "New line 2"
              d.detail("country") mustBe "Poland"
              d.detail("originalLine1") mustBe "Line 1"
              d.detail("originalLine2") mustBe "Line 2"
              d.detail("originalCountry") mustBe "Spain"
          }
        }
      }

      "redirect to CurrentAddressDateOfChangeController" when {
        "address changed and in ready for renewal state" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.CurrentAddressNonUKController.post(1).url)
          .withFormUrlEncodedBody(
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "country" -> "ES")

          val ukAddress = PersonAddressNonUK("Line 1", "Line 2", Some("Line 3"), None, Country("Spain", "ES"))
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history), lineId = Some(1))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(ReadyForRenewal(None)))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any(), any())
          } thenReturn Future.successful(Some(viewResponse))

          val result = currentAddressController.post(RecordId, true)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.get(RecordId,true).url))

        }
      }

      "redirect to CurrentAddressDateOfChangeController" when {
        "address changed and in eligible state for date of change and not in edit mode" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.CurrentAddressNonUKController.post(1).url)
          .withFormUrlEncodedBody(
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "country" -> "ES")

          val ukAddress = PersonAddressNonUK("Line 1", "Line 2", Some("Line 3"), None, Country("Spain", "ES"))
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history), lineId = Some(1))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any(), any())
          } thenReturn Future.successful(Some(viewResponse.copy(responsiblePeopleSection = Some(Seq(responsiblePeople)))))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.get(RecordId).url))
        }
      }

      "redirect to CurrentAddressDateOfChangeController" when {
        "changed address from uk to non-uk and in eligible state for date of change and not in edit mode" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.CurrentAddressNonUKController.post(1).url)
          .withFormUrlEncodedBody(
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "country" -> "ES")

          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history), lineId = Some(1))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any(), any())
          } thenReturn Future.successful(Some(viewResponse.copy(responsiblePeopleSection = Some(Seq(responsiblePeople)))))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.get(RecordId).url))
        }
      }

      "redirect to TimeAtCurrentAddressController" when {
        "not in edit mode and no line id defined" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.CurrentAddressNonUKController.post(1).url)
          .withFormUrlEncodedBody(
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "country" -> "ES")

          val ukAddress = PersonAddressNonUK("Line 1", "Line 2", Some("Line 3"), None, Country("Spain", "ES"))
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any(), any())
          } thenReturn Future.successful(Some(viewResponse))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.address.routes.TimeAtCurrentAddressController.get(RecordId).url))
        }
      }

      "redirect to DetailedAnswersController" when {
        "edit is true" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.CurrentAddressNonUKController.post(1).url)
          .withFormUrlEncodedBody(
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "country" -> "ES"
          )
          val ukAddress = PersonAddressNonUK("Line 1", "Line 2", Some("Line 3"), None, Country("Spain", "ES"))
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any(), any())
          } thenReturn Future.successful(Some(viewResponse))

          val result = currentAddressController.post(RecordId, true)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(RecordId).url))

        }
      }

      "respond with BAD_REQUEST" when {
        "given an invalid address" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.CurrentAddressNonUKController.post(1).url)
          .withFormUrlEncodedBody(
            "addressLine1" -> "Line &1",
            "addressLine2" -> "Line *2",
            "country" -> "ES"
          )
          val ukAddress = PersonAddressNonUK("Line 1", "Line 2", Some("Line 3"), None, Country("Spain", "ES"))
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithParams)
          status(result) must be(BAD_REQUEST)
          val document: Document  = Jsoup.parse(contentAsString(result))
          document.title mustBe s"Error: $pageTitle"
          val errorCount = 2
          val elementsWithError: Elements = document.getElementsByClass("govuk-error-message")
          elementsWithError.size() must be(errorCount)
          val elements = elementsWithError.asScala.map(_.text())
          elements(0) must include(messages("error.text.validation.address.line1"))
          elements(1) must include(messages("error.text.validation.address.line2"))
        }

        "there is no country supplied" in new Fixture {

          val requestWithMissingParams = FakeRequest(POST, routes.CurrentAddressNonUKController.post(1).url)
          .withFormUrlEncodedBody(
            "addressLine1" -> "",
            "addressLine2" -> "",
            "country" -> ""
          )
          val responsiblePeople = ResponsiblePerson(personName = personName)

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLine1]").html() must include(messages("error.required.address.line1"))
          document.select("a[href=#addressLine2]").html() must include(messages("error.required.address.line2"))
          document.select("a[href=#location-autocomplete]").html() must include(messages("error.required.country"))
        }
    }

      "respond with NOT_FOUND" when {
        "given an out of bounds index" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.CurrentAddressNonUKController.post(1).url)
          .withFormUrlEncodedBody(
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "country" -> "ES"
          )
          val ukAddress = PersonAddressNonUK("Line 1", "Line 2", Some("Line 3"), None, Country("Spain", "ES"))
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(outOfBounds, true)(requestWithParams)

          status(result) must be(NOT_FOUND)
        }
      }
    }
  }
}


