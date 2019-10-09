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

package controllers.responsiblepeople

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import generators.ResponsiblePersonGenerator
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, StatusConstants}

import scala.concurrent.Future

class PositionWithinBusinessStartDateControllerSpec extends AmlsSpec with MockitoSugar with ResponsiblePersonGenerator {

  trait Fixture {
    self =>
      val request = addToken(authRequest)

      val controller = new PositionWithinBusinessStartDateController (
        dataCacheConnector = mock[DataCacheConnector],
        authAction = SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc)

      object DefaultValues {
        val noNominatedOfficerPositions = Positions(Set(BeneficialOwner, InternalAccountant), startDate)
        val hasNominatedOfficerPositions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), startDate)
      }

      val responsiblePerson = ResponsiblePerson(
        personName = Some(PersonName("firstname", None, "lastname")),
        approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
        lineId = Some(1)
      )

      val noNominatedOfficer = responsiblePerson.copy(positions = Some(DefaultValues.noNominatedOfficerPositions))
      val hasNominatedOfficer = responsiblePerson.copy(positions = Some(DefaultValues.hasNominatedOfficerPositions))
      val hasNominatedOfficerButDeleted = responsiblePerson.copy(positions = Some(DefaultValues.hasNominatedOfficerPositions), status = Some(StatusConstants.Deleted))
  }

  val emptyCache = CacheMap("", Map.empty)

  val RecordId = 1

  private val startDate: Option[PositionStartDate] = Some(PositionStartDate(new LocalDate()))

  "PositionWithinBusinessStartDateController" when {

    val pageTitle = "When did this person start their role in the business? - " +
      Messages("summary.responsiblepeople") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")

    val pageHeader = "When did firstname lastname start their role in the business?"

    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {

      "display the 'When did this person start their role in the business?' page" in new Fixture {
        val mockCacheMap = mock[CacheMap]
        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.SoleProprietor),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))
        when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
          .thenReturn(Some(Seq(hasNominatedOfficer)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))
        val result = controller.get(RecordId)(request)

        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.title must include(pageTitle)
        document.body().html() must include(pageHeader)

      }

      "prepopulate form with a single saved data" in new Fixture {

        val positions = Positions(Set(BeneficialOwner), startDate)
        val responsiblePeople = ResponsiblePerson(personName = personName, positions = Some(positions))
        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any())).thenReturn(Some(Seq(responsiblePeople)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))
        when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title must include(pageTitle)
        document.select("input[id=startDate-day").`val`() must be(startDate.get.startDate.dayOfMonth().get().toString)
        document.select("input[id=startDate-month").`val`() must be(startDate.get.startDate.monthOfYear().get().toString)
        document.select("input[id=startDate-year").`val`() must be(startDate.get.startDate.getYear.toString)
      }
    }

    "post is called" must {
      "respond with BAD_REQUEST" when {
        "the year field has too few digits" in new Fixture {
          val newRequest = requestWithUrlEncodedBody(
            "startDate.day" -> "24",
            "startDate.month" -> "2",
            "startDate.year" -> "90")

            val mockBusinessMatching: BusinessMatching = mock[BusinessMatching]

            val mockCacheMap = mock[CacheMap]
            when (mockCacheMap.getEntry[Seq[ResponsiblePerson]] (any () ) (any () ) )
              .thenReturn (Some (Seq ( hasNominatedOfficer ) ) )
            when (mockCacheMap.getEntry[BusinessMatching] (BusinessMatching.key) )
              .thenReturn (Some (mockBusinessMatching) )
            when (controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
             .thenReturn (Future.successful (Some (mockCacheMap) ) )
            when (controller.dataCacheConnector.save[Seq[ResponsiblePerson]] (any(), any(), any()) (any(), any()))
              .thenReturn (Future.successful (mockCacheMap) )

            val result = controller.post (RecordId) (newRequest)

            status (result) must be (BAD_REQUEST)
            val document: Document = Jsoup.parse (contentAsString (result) )
            document.title must include (pageTitle)
            contentAsString (result) must include (Messages ("error.expected.jodadate.format") )
          }


          "the year field has too many digits" in new Fixture {
            val newRequest = requestWithUrlEncodedBody(
            "startDate.day" -> "24",
            "startDate.month" -> "2",
            "startDate.year" -> "19905")

            val mockBusinessMatching: BusinessMatching = mock[BusinessMatching]

            val mockCacheMap = mock[CacheMap]
            when (mockCacheMap.getEntry[Seq[ResponsiblePerson]] (any () ) (any () ) )
            .thenReturn (Some (Seq ( hasNominatedOfficer ) ) )
            when (mockCacheMap.getEntry[BusinessMatching] (BusinessMatching.key) )
            .thenReturn (Some (mockBusinessMatching) )
            when (controller.dataCacheConnector.fetchAll(any()) (any[HeaderCarrier] ) )
            .thenReturn (Future.successful (Some (mockCacheMap) ) )

            val result = controller.post (RecordId) (newRequest)
            status (result) must be (BAD_REQUEST)
            contentAsString (result) must include (Messages ("error.expected.jodadate.format") )
          }


          "the date fields are empty" in new Fixture {

            val newRequest = requestWithUrlEncodedBody("positions" -> "01", "startDate.day" -> "", "startDate.month" -> "", "startDate.year" -> "")

            val mockBusinessMatching: BusinessMatching = mock[BusinessMatching]

            val mockCacheMap = mock[CacheMap]
            when (mockCacheMap.getEntry[Seq[ResponsiblePerson]] (any () ) (any () ) )
            .thenReturn (Some (Seq ( hasNominatedOfficer ) ) )
            when (mockCacheMap.getEntry[BusinessMatching] (BusinessMatching.key) )
            .thenReturn (Some (mockBusinessMatching) )
            when (controller.dataCacheConnector.fetchAll(any()) (any[HeaderCarrier] ) )
            .thenReturn (Future.successful (Some (mockCacheMap) ) )
            val result = controller.post (RecordId) (newRequest)
            status (result) must be (BAD_REQUEST)
            val document: Document = Jsoup.parse (contentAsString (result) )
            document.body ().html () must include (Messages ("error.expected.jodadate.format") )
          }
      }

      "when edit is false" must {
        "redirect to the 'Sole proprietor of another business?' page" in new Fixture {
          val newRequest = requestWithUrlEncodedBody(
            "positions" -> "04",
            "startDate.day" -> "24",
            "startDate.month" -> "2",
            "startDate.year" -> "1990")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(hasNominatedOfficer))))

          val mockCacheMap = mock[CacheMap]

          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.SoleProprietorOfAnotherBusinessController.get(RecordId).url))
        }
      }

      "when edit is true" must {
        "redirect to the 'Check your answers' page" in new Fixture {
          val newRequest = requestWithUrlEncodedBody(
            "positions" -> "04",
            "startDate.day" -> "24",
            "startDate.month" -> "2",
            "startDate.year" -> "1990")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
            (any(), any())).thenReturn(Future.successful(Some(Seq(hasNominatedOfficer))))
          val mockCacheMap = mock[CacheMap]
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId, true, Some(flowFromDeclaration))(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(RecordId, Some(flowFromDeclaration)).url))
        }
      }
    }
  }
}
