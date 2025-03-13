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

package controllers.responsiblepeople

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.responsiblepeople.PositionWithinBusinessStartDateFormProvider
import generators.ResponsiblePersonGenerator
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, StatusConstants}
import views.html.responsiblepeople.PositionWithinBusinessStartDateView

import java.time.LocalDate
import scala.concurrent.Future

class PositionWithinBusinessStartDateControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with ResponsiblePersonGenerator
    with Injecting {

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[PositionWithinBusinessStartDateView]
    val controller = new PositionWithinBusinessStartDateController(
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[PositionWithinBusinessStartDateFormProvider],
      view = view,
      error = errorView
    )

    object DefaultValues {
      val noNominatedOfficerPositions  = Positions(Set(BeneficialOwner, InternalAccountant), startDate)
      val hasNominatedOfficerPositions =
        Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), startDate)
    }

    val responsiblePerson = ResponsiblePerson(
      personName = Some(PersonName("firstname", None, "lastname")),
      approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
      lineId = Some(1)
    )

    val noNominatedOfficer            = responsiblePerson.copy(positions = Some(DefaultValues.noNominatedOfficerPositions))
    val hasNominatedOfficer           = responsiblePerson.copy(positions = Some(DefaultValues.hasNominatedOfficerPositions))
    val hasNominatedOfficerButDeleted = responsiblePerson.copy(
      positions = Some(DefaultValues.hasNominatedOfficerPositions),
      status = Some(StatusConstants.Deleted)
    )
  }

  val emptyCache = Cache.empty

  val RecordId = 1

  private val startDate: Option[PositionStartDate] = Some(PositionStartDate(LocalDate.now()))

  val pageTitle = "When did this person start their role in the business? - " +
    messages("summary.responsiblepeople") + " - " +
    messages("title.amls") + " - " + messages("title.gov")

  val pageHeader = "When did firstname lastname start their role in the business?"

  val personName = Some(PersonName("firstname", None, "lastname"))

  "PositionWithinBusinessStartDateController" when {

    "get is called" must {

      "display the 'When did this person start their role in the business?' page" in new Fixture {
        val mockCacheMap     = mock[Cache]
        val reviewDtls       = ReviewDetails(
          "BusinessName",
          Some(BusinessType.SoleProprietor),
          Address(
            "line1",
            Some("line2"),
            Some("line3"),
            Some("line4"),
            Some("AA11 1AA"),
            Country("United Kingdom", "GB")
          ),
          "ghghg"
        )
        val businessMatching = BusinessMatching(Some(reviewDtls))
        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
          .thenReturn(Some(Seq(hasNominatedOfficer)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))
        val result           = controller.get(RecordId)(request)

        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.title         must include(pageTitle)
        document.body().html() must include(pageHeader)
      }

      "display the 'When did this person start their role in the business?' page when no business matching available" in new Fixture {
        val mockCacheMap = mock[Cache]

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
          .thenReturn(Some(Seq(hasNominatedOfficer)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(None)

        val result = controller.get(RecordId)(request)

        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.title         must include(pageTitle)
        document.body().html() must include(pageHeader)
      }

      "display the 'When did this person start their role in the business?' page when no start date" in new Fixture {
        val mockCacheMap = mock[Cache]

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
          .thenReturn(
            Some(
              Seq(hasNominatedOfficer.copy(positions = Some(hasNominatedOfficer.positions.get.copy(startDate = None))))
            )
          )
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(None)

        val result = controller.get(RecordId)(request)

        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.title         must include(pageTitle)
        document.body().html() must include(pageHeader)
      }

      "prepopulate form with a single saved data" in new Fixture {

        val positions         = Positions(Set(BeneficialOwner), startDate)
        val responsiblePeople = ResponsiblePerson(personName = personName, positions = Some(positions))
        val reviewDtls        = ReviewDetails(
          "BusinessName",
          Some(BusinessType.LimitedCompany),
          Address(
            "line1",
            Some("line2"),
            Some("line3"),
            Some("line4"),
            Some("AA11 1AA"),
            Country("United Kingdom", "GB")
          ),
          "ghghg"
        )
        val businessMatching  = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[Cache]
        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any())).thenReturn(Some(Seq(responsiblePeople)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))
        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title                                       must include(pageTitle)
        document.select("input[id=startDate.day]").`val`()   must be(startDate.get.startDate.getDayOfMonth().toString)
        document.select("input[id=startDate.month]").`val`() must be(startDate.get.startDate.getMonthValue().toString)
        document.select("input[id=startDate.year]").`val`()  must be(startDate.get.startDate.getYear.toString)
      }
    }

    "post is called" must {
      "respond with BAD_REQUEST" when {
        "the year field has too few digits" in new Fixture {
          val newRequest = FakeRequest(POST, routes.PositionWithinBusinessStartDateController.post(1).url)
            .withFormUrlEncodedBody("startDate.day" -> "24", "startDate.month" -> "2", "startDate.year" -> "90")

          val mockBusinessMatching: BusinessMatching = mock[BusinessMatching]

          val mockCacheMap = mock[Cache]
          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(hasNominatedOfficer)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(mockBusinessMatching))
          when(controller.dataCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId)(newRequest)

          status(result) must be(BAD_REQUEST)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title must include(pageTitle)
        }

        "the year field has too many digits" in new Fixture {
          val newRequest = FakeRequest(POST, routes.PositionWithinBusinessStartDateController.post(1).url)
            .withFormUrlEncodedBody("startDate.day" -> "24", "startDate.month" -> "2", "startDate.year" -> "19905")

          val mockBusinessMatching: BusinessMatching = mock[BusinessMatching]

          val mockCacheMap = mock[Cache]
          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(hasNominatedOfficer)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(mockBusinessMatching))
          when(controller.dataCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(BAD_REQUEST)
        }

        "the date fields are empty" in new Fixture {

          val newRequest = FakeRequest(POST, routes.PositionWithinBusinessStartDateController.post(1).url)
            .withFormUrlEncodedBody("startDate.day" -> "", "startDate.month" -> "", "startDate.year" -> "")

          val mockBusinessMatching: BusinessMatching = mock[BusinessMatching]

          val mockCacheMap = mock[Cache]
          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(hasNominatedOfficer)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(mockBusinessMatching))
          when(controller.dataCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))
          val result       = controller.post(RecordId)(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }

      "when edit is false" must {
        "redirect to the 'Sole proprietor of another business?' page" in new Fixture {
          val newRequest = FakeRequest(POST, routes.PositionWithinBusinessStartDateController.post(1).url)
            .withFormUrlEncodedBody("startDate.day" -> "24", "startDate.month" -> "2", "startDate.year" -> "1990")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(hasNominatedOfficer))))

          val mockCacheMap = mock[Cache]

          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.SoleProprietorOfAnotherBusinessController.get(RecordId).url)
          )
        }
      }

      "when edit is true" must {
        "redirect to the 'Check your answers' page" in new Fixture {
          val newRequest = FakeRequest(POST, routes.PositionWithinBusinessStartDateController.post(1).url)
            .withFormUrlEncodedBody("startDate.day" -> "24", "startDate.month" -> "2", "startDate.year" -> "1990")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(hasNominatedOfficer))))
          val mockCacheMap = mock[Cache]
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId, true, Some(flowFromDeclaration))(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(
              controllers.responsiblepeople.routes.DetailedAnswersController
                .get(RecordId, Some(flowFromDeclaration))
                .url
            )
          )
        }
      }
    }
  }
}
