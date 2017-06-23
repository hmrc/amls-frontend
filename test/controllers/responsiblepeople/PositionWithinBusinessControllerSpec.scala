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

package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.Country
import models.businessactivities.BusinessActivities
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.{AuthorisedFixture, GenericTestHelper, StatusConstants}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class PositionWithinBusinessControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new PositionWithinBusinessController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }

    object DefaultValues {
      val noNominatedOfficerPositions = Positions(Set(BeneficialOwner, InternalAccountant), startDate)
      val hasNominatedOfficerPositions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), startDate)
    }

    val noNominatedOfficer = ResponsiblePeople(None, None, None,None, None, None, None, Some(DefaultValues.noNominatedOfficerPositions), None, None, None, None, Some(true), false, Some(1), Some("test"))
    val hasNominatedOfficer = ResponsiblePeople(None, None, None,None, None, None, None, Some(DefaultValues.hasNominatedOfficerPositions), None, None, None, None, Some(true), false, Some(1), Some("test"))
    val hasNominatedOfficerButDeleted = ResponsiblePeople(None, None,None, None, None, None, None, Some(DefaultValues.hasNominatedOfficerPositions), None, None, None, None, Some(true), false, Some(1), Some(StatusConstants.Deleted))
  }

  val emptyCache = CacheMap("", Map.empty)

  val RecordId = 1

  private val startDate: Option[LocalDate] = Some(new LocalDate())

  "PositionWithinBusinessController" when {

    val pageTitle = Messages("responsiblepeople.position_within_business.title", "firstname lastname") + " - " +
      Messages("summary.responsiblepeople") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")
    val personName = Some(PersonName("firstname", None, "lastname", None, None))

    "get is called" must {

      "display position within the business page" in new Fixture {
        val mockCacheMap = mock[CacheMap]
        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.SoleProprietor),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))
        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(Some(Seq(ResponsiblePeople(personName))))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))
        val result = controller.get(RecordId)(request)

        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must include(pageTitle)
        document.select("input[value=02]").isEmpty must be(true)
        document.select("input[value=03]").isEmpty must be(true)
        document.select("input[value=04]").isEmpty must be(false)
        document.select("input[value=05]").isEmpty must be(true)
        document.select("input[value=06]").isEmpty must be(false)

        document.body().html() must include(Messages("responsiblepeople.position_within_business.startDate.lbl"))

      }

      "display position within the business page when business Type is SoleProprietor" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.SoleProprietor),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(Some(Seq(ResponsiblePeople(personName))))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must include(pageTitle)
        document.select("input[value=04]").hasAttr("checked") must be(false)
        document.select("input[value=06]").hasAttr("checked") must be(false)
      }

      "display position within the business page when business Type is Partnership" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.Partnership),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(Some(Seq(ResponsiblePeople(personName))))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must include(pageTitle)
        document.select("input[value=04]").hasAttr("checked") must be(false)
        document.select("input[value=05]").hasAttr("checked") must be(false)
      }

      "display position within the business page when business Type is UnincorporatedBody" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.UnincorporatedBody),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(Some(Seq(ResponsiblePeople(personName))))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must include(pageTitle)
        document.select("input[value=01]").hasAttr("checked") must be(false)
        document.select("input[value=02]").hasAttr("checked") must be(false)
        document.select("input[value=04]").hasAttr("checked") must be(false)
      }

      "display position within the business page when business Type is LPrLLP" in new Fixture {

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LPrLLP),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))
        val positions = Positions(Set(BeneficialOwner, Director, NominatedOfficer, DesignatedMember), startDate)
        val responsiblePeople = ResponsiblePeople(personName = personName, positions = Some(positions))

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(Some(Seq(responsiblePeople)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must include(pageTitle)
        document.select("input[value=01]").hasAttr("checked") must be(true)
        document.select("input[value=02]").hasAttr("checked") must be(true)
        document.select("input[value=04]").hasAttr("checked") must be(true)
        document.select("input[value=07]").hasAttr("checked") must be(true)
      }

      "Prepopulate form with a single saved data" in new Fixture {

        val positions = Positions(Set(BeneficialOwner), startDate)
        val responsiblePeople = ResponsiblePeople(personName = personName, positions = Some(positions))
        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(Seq(responsiblePeople)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))
        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title must include(pageTitle)
        document.select("input[value=01]").hasAttr("checked") must be(true)
        document.select("input[value=02]").hasAttr("checked") must be(false)
        document.select("input[value=03]").hasAttr("checked") must be(false)
        document.select("input[value=04]").hasAttr("checked") must be(false)
        document.select("input[value=05]").hasAttr("checked") must be(false)
        document.select("input[value=06]").hasAttr("checked") must be(false)

        document.select("input[id=startDate-day").`val`() must be(startDate.get.dayOfMonth().get().toString)
        document.select("input[id=startDate-month").`val`() must be(startDate.get.monthOfYear().get().toString)
        document.select("input[id=startDate-year").`val`() must be(startDate.get.getYear.toString)
      }

      "Prepopulate form with multiple saved data" in new Fixture {

        val positions = Positions(Set(Director), startDate)
        val responsiblePeople = ResponsiblePeople(personName = personName, positions = Some(positions))

        val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
          Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
        val businessMatching = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[CacheMap]
        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any())).thenReturn(Some(Seq(responsiblePeople)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))
        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title must include(pageTitle)
        document.select("input[value=01]").hasAttr("checked") must be(false)
        document.select("input[value=02]").hasAttr("checked") must be(true)
        document.select("input[value=04]").hasAttr("checked") must be(false)
      }
    }

    "post is called" must {
      "respond with BAD_REQUEST" when {
        "year field is given too few numbers" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody("positions" -> "06",
            "startDate.day" -> "24",
            "startDate.month" -> "2",
            "startDate.year" -> "90")

          val mockBusinessMatching: BusinessMatching = mock[BusinessMatching]

          val mockCacheMap = mock[CacheMap]
          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(ResponsiblePeople(personName))))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(mockBusinessMatching))
          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId)(newRequest)

          status(result) must be(BAD_REQUEST)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title must include(pageTitle)
          contentAsString(result) must include(Messages("error.expected.jodadate.format"))
        }

        "year field is given too many characters" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody("positions" -> "06",
            "startDate.day" -> "24",
            "startDate.month" -> "2",
            "startDate.year" -> "19905")

          val mockBusinessMatching: BusinessMatching = mock[BusinessMatching]

          val mockCacheMap = mock[CacheMap]
          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(ResponsiblePeople(personName))))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(mockBusinessMatching))
          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.expected.jodadate.format"))
        }

        "positionWithinBusiness field is given an empty string" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("positionWithinBusiness" -> "")

          val mockBusinessMatching: BusinessMatching = mock[BusinessMatching]

          val mockCacheMap = mock[CacheMap]
          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(ResponsiblePeople(personName))))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(mockBusinessMatching))
          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(BAD_REQUEST)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#positions]").html() must include(Messages("error.required.positionWithinBusiness"))

        }

        "the date fields are given empty strings" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("positions" -> "01", "startDate.day" -> "", "startDate.month" -> "", "startDate.year" -> "")

          val mockBusinessMatching: BusinessMatching = mock[BusinessMatching]

          val mockCacheMap = mock[CacheMap]
          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(ResponsiblePeople(personName))))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(mockBusinessMatching))
          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))
          val result = controller.post(RecordId)(newRequest)
          status(result) must be(BAD_REQUEST)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.body().html() must include(Messages("error.expected.jodadate.format"))
        }

        "positionWithinBusiness is given an invalid string code" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("positionWithinBusiness" -> "10")
          val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
            Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
          val businessMatching = BusinessMatching(Some(reviewDtls))

          val mockCacheMap = mock[CacheMap]
          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(ResponsiblePeople(personName))))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))
          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(BAD_REQUEST)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#positions]").html() must include(Messages("error.required.positionWithinBusiness"))
        }
      }

      "when edit is false" must {
        "redirect to the sole proprietor another business Controller when Nominated Officer is selected" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "positions" -> "04",
            "startDate.day" -> "24",
            "startDate.month" -> "2",
            "startDate.year" -> "1990")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(hasNominatedOfficer))))
          val mockCacheMap = mock[CacheMap]
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.SoleProprietorOfAnotherBusinessController.get(RecordId).url))
        }

        "redirect to the AreTheyNominatedOfficerController when Nominated Officer is NOT selected" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("positions" -> "06",
            "startDate.day" -> "24",
            "startDate.month" -> "2",
            "startDate.year" -> "1990")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))
          val mockCacheMap = mock[CacheMap]
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.AreTheyNominatedOfficerController.get(RecordId).url))
        }

        "redirect to the AreTheyNominatedOfficerController when Nominated Officer is NOT selected and status is Deleted" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("positions" -> "06",
            "startDate.day" -> "24",
            "startDate.month" -> "2",
            "startDate.year" -> "1990")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(hasNominatedOfficerButDeleted))))
          val mockCacheMap = mock[CacheMap]
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.AreTheyNominatedOfficerController.get(RecordId).url))
        }

        "redirect to sole proprietor another business controller when another position is selected in addition to the nomindated Officer" in new Fixture {

          val positions = Positions(Set(Director, NominatedOfficer), startDate)
          val responsiblePeople = ResponsiblePeople(positions = Some(positions))

          val newRequest = request.withFormUrlEncodedBody(
            "positions" -> "06",
            "positions" -> "01",
            "startDate.day" -> "24",
            "startDate.month" -> "2",
            "startDate.year" -> "1990"
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          val mockCacheMap = mock[CacheMap]
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.SoleProprietorOfAnotherBusinessController.get(RecordId).url))

        }
      }

      "when edit is true" must {
        "redirect to the VAT Registered Controller when Nominated Officer is selected" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "positions" -> "04",
            "startDate.day" -> "24",
            "startDate.month" -> "2",
            "startDate.year" -> "1990")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(hasNominatedOfficer))))
          val mockCacheMap = mock[CacheMap]
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId, true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(RecordId).url))
        }

        "redirect to the AreTheyNominatedOfficerController when Nominated Officer is NOT selected" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("positions" -> "06",
            "startDate.day" -> "24",
            "startDate.month" -> "2",
            "startDate.year" -> "1990")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))
          val mockCacheMap = mock[CacheMap]
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId, true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.AreTheyNominatedOfficerController.get(RecordId, true).url))
        }
      }



    }
  }

  "hasNominatedOfficer" must {

    "return true when there is nominated officer" in new Fixture {
      controller.hasNominatedOfficer(Some(Seq(hasNominatedOfficer))) must be(true)
    }

    "return true when one rp is nominated officer" in new Fixture {
      controller.hasNominatedOfficer(Some(Seq(hasNominatedOfficer,noNominatedOfficer))) must be(true)
    }

    "return false when no responsible people" in new Fixture {
      controller.hasNominatedOfficer(Some(Nil)) must be(false)
    }

    "return false when there is no nominated officer" in new Fixture {
      controller.hasNominatedOfficer(Some(Seq(noNominatedOfficer.copy(positions =
        Some(DefaultValues.noNominatedOfficerPositions))))) must be(false)
    }
  }
}
