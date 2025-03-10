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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.responsiblepeople.address.CurrentAddressUKFormProvider
import models.businessactivities.BusinessActivities
import models.businessdetails.BusinessDetails
import models.businessmatching.BusinessActivity.BillPaymentServices
import models.businessmatching.{BusinessActivities => BMActivities, BusinessMatching}
import models.declaration.AddPerson
import models.declaration.release7.RoleWithinBusinessRelease7
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import models.responsiblepeople._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReadyForReview}
import models.{Country, ViewResponse}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import services.cache.Cache
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.AmlsSpec
import views.html.responsiblepeople.address.CurrentAddressUKView

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class CurrentAddressUKControllerSpec extends AmlsSpec with ScalaFutures with MockitoSugar with Injecting {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockDataCacheConnector     = mock[DataCacheConnector]
  val RecordId                   = 1

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val viewResponse = ViewResponse(
      "",
      businessMatchingSection = BusinessMatching(
        activities = Some(
          BMActivities(
            Set(BillPaymentServices)
          )
        )
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

    val auditConnector           = mock[AuditConnector]
    val statusService            = mock[StatusService]
    lazy val view                = inject[CurrentAddressUKView]
    val currentAddressController = new CurrentAddressUKController(
      dataCacheConnector = mockDataCacheConnector,
      auditConnector = auditConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      statusService = statusService,
      cc = mockMcc,
      formProvider = inject[CurrentAddressUKFormProvider],
      view = view,
      error = errorView
    )

    when {
      auditConnector.sendEvent(any())(any(), any())
    } thenReturn Future.successful(Success)
  }

  val emptyCache  = Cache.empty
  val outOfBounds = 99

  "CurrentAddressUKController" when {

    val pageTitle = messages("responsiblepeople.wherepersonlivescountry.title", "firstname lastname") + " - " +
      messages("summary.responsiblepeople") + " - " +
      messages("title.amls") + " - " + messages("title.gov")

    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {

      "respond with NOT_FOUND when called with an index that is out of bounds" in new Fixture {
        val responsiblePeople = ResponsiblePerson()

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(40)(request)
        status(result) must be(NOT_FOUND)
      }

      "display the address uk page when no existing data in mongoCache" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title                                         must be(pageTitle)
        document.select("input[name=addressLine1]").`val`      must be("")
        document.select("input[name=addressLine2]").`val`      must be("")
        document.select("input[name=addressLine3]").`val`      must be("")
        document.select("input[name=addressLine4]").`val`      must be("")
        document.select("input[name=addressLineNonUK1]").`val` must be("")
        document.select("input[name=addressLineNonUK2]").`val` must be("")
        document.select("input[name=addressLineNonUK3]").`val` must be("")
        document.select("input[name=addressLineNonUK4]").`val` must be("")
        document.select("input[name=postcode]").`val`          must be("")
        document.select("input[name=country]").`val`           must be("")
      }

      "display the home address with UK fields populated" in new Fixture {

        val ukAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
        val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
        val history           = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title                                    must be(pageTitle)
        document.select("input[name=addressLine1]").`val` must be("Line 1")
        document.select("input[name=addressLine2]").`val` must be("Line 2")
        document.select("input[name=addressLine3]").`val` must be("Line 3")
        document.select("input[name=addressLine4]").`val` must be("")
        document.select("input[name=postcode]").`val`     must be("AA1 1AA")
      }
    }

    "post is called" must {
      "redirect to TimeAtAddressController" when {
        "all the mandatory UK parameters are supplied" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.CurrentAddressUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "Line 1",
              "postCode"     -> "AA1 1AA"
            )
          val ukAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any())
          } thenReturn Future.successful(Some(viewResponse))

          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.TimeAtCurrentAddressController.get(RecordId).url))

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "Line 1"
              d.detail("postCode") mustBe "AA1 1AA"
          }
        }
      }

      "redirect to CurrentAddressDateOfChangeController" when {
        "address changed and in approved state" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.CurrentAddressUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "New line 1",
              "addressLine2" -> "New line 2",
              "postCode"     -> "TE1 1ET"
            )
          val ukAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history), lineId = Some(1))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any())
          } thenReturn Future.successful(
            Some(viewResponse.copy(responsiblePeopleSection = Some(Seq(responsiblePeople))))
          )

          val result = currentAddressController.post(RecordId, true)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(
              controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.get(RecordId, true).url
            )
          )

          val captor = ArgumentCaptor.forClass(classOf[DataEvent])
          verify(auditConnector).sendEvent(captor.capture())(any(), any())

          captor.getValue match {
            case d: DataEvent =>
              d.detail("addressLine1") mustBe "New line 1"
              d.detail("addressLine2") mustBe "New line 2"
              d.detail("postCode") mustBe "TE1 1ET"
              d.detail("originalLine1") mustBe "Line 1"
              d.detail("originalLine2") mustBe "Line 2"
              d.detail("originalPostCode") mustBe "AA1 1AA"
          }
        }
      }

      "redirect to CurrentAddressDateOfChangeController" when {
        "address changed and in ready for renewal state" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.CurrentAddressUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "postCode"     -> "AA1 1AA"
            )
          val ukAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history), lineId = Some(1))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(ReadyForRenewal(None)))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any())
          } thenReturn Future.successful(Some(viewResponse))

          val result = currentAddressController.post(RecordId, true)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(
              controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.get(RecordId, true).url
            )
          )

        }
      }

      "redirect to CurrentAddressDateOfChangeController" when {
        "address changed and in eligible state for date of change and not in edit mode" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.CurrentAddressUKController.post(1).url)
            .withFormUrlEncodedBody("addressLine1" -> "Line 1", "addressLine2" -> "Line 2", "postCode" -> "AA1 1AA")

          val ukAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history), lineId = Some(1))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any())
          } thenReturn Future.successful(
            Some(viewResponse.copy(responsiblePeopleSection = Some(Seq(responsiblePeople))))
          )

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.get(RecordId).url)
          )
        }
      }

      "redirect to CurrentAddressDateOfChangeController" when {
        "changed address from non-uk to uk and in eligible state for date of change and not in edit mode" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.CurrentAddressUKController.post(1).url)
            .withFormUrlEncodedBody("addressLine1" -> "Line 1", "addressLine2" -> "Line 2", "postCode" -> "AA1 1AA")

          val ukAddress         = PersonAddressNonUK("", None, None, None, Country("", ""))
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history), lineId = Some(1))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any())
          } thenReturn Future.successful(
            Some(viewResponse.copy(responsiblePeopleSection = Some(Seq(responsiblePeople))))
          )

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.get(RecordId).url)
          )
        }
      }

      "redirect to TimeAtCurrentAddressController" when {
        "not in edit mode and no line id defined" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.CurrentAddressUKController.post(1).url)
            .withFormUrlEncodedBody("addressLine1" -> "Line 1", "addressLine2" -> "Line 2", "postCode" -> "AA1 1AA")

          val ukAddress         = PersonAddressNonUK("", None, None, None, Country("", ""))
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any())
          } thenReturn Future.successful(Some(viewResponse))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.address.routes.TimeAtCurrentAddressController.get(RecordId).url)
          )
        }
      }

      "redirect to DetailedAnswersController" when {
        "edit is true" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.CurrentAddressUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "postCode"     -> "AA1 1AA"
            )
          val ukAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when {
            currentAddressController.dataCacheConnector.fetch[ViewResponse](any(), eqTo(ViewResponse.key))(any())
          } thenReturn Future.successful(Some(viewResponse))

          val result = currentAddressController.post(RecordId, true)(requestWithParams)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(RecordId).url)
          )

        }
      }

      "respond with BAD_REQUEST" when {

        "given an invalid address" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.CurrentAddressUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "Line &1",
              "addressLine2" -> "Line *2",
              "postCode"     -> "AA1 1AA"
            )
          val ukAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))
          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithParams)
          status(result) must be(BAD_REQUEST)
          val document: Document          = Jsoup.parse(contentAsString(result))
          document.title mustBe s"Error: $pageTitle"
          val errorCount                  = 2
          val elementsWithError: Elements = document.getElementsByClass("govuk-error-message")
          elementsWithError.size() must be(errorCount)
          val elements = elementsWithError.asScala.map(_.text())
          elements(0) must include(messages("error.text.validation.address.line1"))
          elements(1) must include(messages("error.text.validation.address.line2"))
        }

        "the default fields for UK are not supplied" in new Fixture {

          val requestWithMissingParams = FakeRequest(POST, routes.CurrentAddressUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "",
              "postCode"     -> ""
            )

          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          when(statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLine1]").html() must include(messages("error.required.address.line1"))
          document.select("a[href=#postcode]").html()     must include(messages("error.required.postcode"))
        }
      }

      "respond with NOT_FOUND" when {
        "given an out of bounds index" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.CurrentAddressUKController.post(1).url)
            .withFormUrlEncodedBody(
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "postCode"     -> "AA1 1AA"
            )
          val ukAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
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
