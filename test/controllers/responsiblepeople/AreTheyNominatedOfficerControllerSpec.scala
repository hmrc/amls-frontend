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
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture


import scala.concurrent.Future

class AreTheyNominatedOfficerControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new AreTheyNominatedOfficerController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }

    object DefaultValues {
      val noNominatedOfficerPositions = Positions(Set(BeneficialOwner, InternalAccountant), startDate)
      val hasNominatedOfficerPositions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), startDate)
    }
    val personName = Some(PersonName("firstname", None, "lastname", None, None))
    val noNominatedOfficer = ResponsiblePeople(personName, None,None, None,None,None, None, Some(DefaultValues.noNominatedOfficerPositions), None,None, None,None, Some(true), false, Some(1), Some("test"))
    val hasNominatedOfficer = ResponsiblePeople(personName, None,None, None,None,None, None, Some(DefaultValues.hasNominatedOfficerPositions), None,None, None,None, Some(true), false, Some(1), Some("test"))
    val withPartnerShip = ResponsiblePeople(personName, None,None, None,None,None, None, Some(DefaultValues.hasNominatedOfficerPositions.copy(positions = DefaultValues.hasNominatedOfficerPositions.positions + Partner)), None,None, None,None, Some(true), false, Some(1), Some("test"))
  }

  val emptyCache = CacheMap("", Map.empty)

  val RecordId = 1

  private val startDate: Option[LocalDate] = Some(new LocalDate())
  "AreTheyNominatedOfficerController" when {

    val pageTitle = Messages("responsiblepeople.aretheynominatedofficer.title", "firstname lastname") + " - " +
      Messages("summary.responsiblepeople") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")

    "get is called" must {

      "display nominated officer fields" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))
        val result = controller.get(RecordId)(request)

        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title mustBe(pageTitle)
        document.select("input[value=true]") must not be(null)
        document.select("input[value=false]") must not be(null)

        document.body().html() must include(Messages("responsiblepeople.aretheynominatedofficer.title"))
      }
    }

    "post is called" must {
      "when edit is true" must {
        "redirect to the detailed answers controller" in new Fixture {
          val mockCacheMap = mock[CacheMap]
          val newRequest = request.withFormUrlEncodedBody("isNominatedOfficer" -> "true")
          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(hasNominatedOfficer)))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(hasNominatedOfficer))))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId,true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(RecordId).url))
        }
      }
      "when edit is false" must {
        "redirect to the vat registered controller when partnership is selected" in new Fixture {
          val mockCacheMap = mock[CacheMap]
          val newRequest = request.withFormUrlEncodedBody("isNominatedOfficer" -> "true")
          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(withPartnerShip)))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(withPartnerShip))))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.SoleProprietorOfAnotherBusinessController.get(RecordId).url))
        }

        "redirect to the vat registered controller when another type is selected" in new Fixture {
          val mockCacheMap = mock[CacheMap]
          val newRequest = request.withFormUrlEncodedBody("isNominatedOfficer" -> "true")
          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(noNominatedOfficer)))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(noNominatedOfficer))))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.SoleProprietorOfAnotherBusinessController.get(RecordId).url))
        }


      }

      "respond with BAD_REQUEST" when {
        "fail submission on empty string" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("isNominatedOfficer" -> "")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName)))))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(BAD_REQUEST)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.title mustBe(pageTitle)
          document.select("a[href=#isNominatedOfficer]").html() must include(Messages("error.required.rp.nominated_officer"))

        }
      }

      "respond with NOT_FOUND" when {
        "return not found when no rps" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("isNominatedOfficer" -> "true")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(None))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(NOT_FOUND)

        }

        "return not found when index out of bounds" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody("isNominatedOfficer" -> "true")

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.failed(new IndexOutOfBoundsException))

          val result = controller.post(RecordId)(newRequest)
          status(result) must be(NOT_FOUND)

        }

        "return not found" in new Fixture {
          val mockCacheMap = mock[CacheMap]
          val newRequest = request.withFormUrlEncodedBody("isNominatedOfficer" -> "true")
          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(withPartnerShip)))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(withPartnerShip))))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(0)(newRequest)
          status(result) must be(NOT_FOUND)

        }
      }
    }
  }
}
