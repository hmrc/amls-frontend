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
import forms.responsiblepeople.PositionWithinBusinessFormProvider
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
import views.html.responsiblepeople.PositionWithinBusinessView

import java.time.LocalDate
import scala.concurrent.Future

class PositionWithinBusinessControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with ResponsiblePersonGenerator
    with Injecting {

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val mockAuthConnector = authConnector
    lazy val view         = inject[PositionWithinBusinessView]
    val controller        = new PositionWithinBusinessController(
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[PositionWithinBusinessFormProvider],
      view = view,
      error = errorView
    )

    object DefaultValues {
      val noNominatedOfficerPositions  = Positions(Set(BeneficialOwner, InternalAccountant), startDate)
      val hasNominatedOfficerPositions =
        Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), startDate)
    }

    val responsiblePerson             = ResponsiblePerson(
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

  "PositionWithinBusinessController" when {

    val pageTitle  = messages("responsiblepeople.position_within_business.title", "firstname lastname") + " - " +
      messages("summary.responsiblepeople") + " - " +
      messages("title.amls") + " - " + messages("title.gov")
    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {

      "display 'What is this person's role?' page" in new Fixture {
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
          .thenReturn(Some(Seq(ResponsiblePerson(personName))))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))
        val result           = controller.get(RecordId)(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.title must include(pageTitle)
      }

      "Prepopulate form with a single saved data" in new Fixture {

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
        document.title must include(pageTitle)

        PositionWithinBusiness.all map { pos =>
          val checkboxIsChecked = document.select(s"input[value=${pos.toString}]").hasAttr("checked")
          if (pos == BeneficialOwner) checkboxIsChecked mustBe true else checkboxIsChecked mustBe false
        }
      }

      "Prepopulate form with multiple saved data" in new Fixture {

        val positions         = Positions(Set(BeneficialOwner, Director), startDate)
        val responsiblePeople = ResponsiblePerson(personName = personName, positions = Some(positions))

        val reviewDtls       = ReviewDetails(
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
        val businessMatching = BusinessMatching(Some(reviewDtls))

        val mockCacheMap = mock[Cache]
        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any())).thenReturn(Some(Seq(responsiblePeople)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(businessMatching))
        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.get(RecordId)(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.title must include(pageTitle)
        PositionWithinBusiness.all map { pos =>
          val checkboxIsChecked = document.select(s"input[value=${pos.toString}]").hasAttr("checked")
          if (pos == BeneficialOwner || pos == Director) checkboxIsChecked mustBe true
          else checkboxIsChecked mustBe false
        }
      }
    }

    "post is called" must {

      "respond with BAD_REQUEST" when {

        "positionWithinBusiness field is given an empty string" in new Fixture {

          val newRequest = FakeRequest(POST, routes.PositionWithinBusinessController.post(1).url)
            .withFormUrlEncodedBody("positionWithinBusiness" -> "")

          val mockBusinessMatching: BusinessMatching = mock[BusinessMatching]

          val mockCacheMap = mock[Cache]
          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(ResponsiblePerson(personName))))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(mockBusinessMatching))
          when(controller.dataCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(RecordId)(newRequest)
          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include(messages("error.required.positionWithinBusiness"))
        }

        "positionWithinBusiness is given an invalid option" in new Fixture {

          val newRequest       = FakeRequest(POST, routes.PositionWithinBusinessController.post(1).url)
            .withFormUrlEncodedBody("positionWithinBusiness" -> "foobar")
          val reviewDtls       = ReviewDetails(
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
          val businessMatching = BusinessMatching(Some(reviewDtls))

          val mockCacheMap = mock[Cache]
          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(ResponsiblePerson(personName))))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(businessMatching))
          when(controller.dataCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(RecordId)(newRequest)
          status(result)          must be(BAD_REQUEST)
          contentAsString(result) must include(messages("error.required.positionWithinBusiness"))
        }
      }

      "when edit is false" must {

        "redirect to 'When did this person start their role in the business?'" when {

          "Nominated Officer is selected" in new Fixture {

            val newRequest = FakeRequest(POST, routes.PositionWithinBusinessController.post(1).url)
              .withFormUrlEncodedBody(
                "positions[0]" -> NominatedOfficer.toString
              )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(hasNominatedOfficer))))

            val mockCacheMap = mock[Cache]

            when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
              .thenReturn(Future.successful(mockCacheMap))

            val result = controller.post(RecordId)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(controllers.responsiblepeople.routes.PositionWithinBusinessStartDateController.get(RecordId).url)
            )
          }

          "Nominated Officer and another role is selected" in new Fixture {

            val positions         = Positions(Set(Director, NominatedOfficer), startDate)
            val responsiblePeople = ResponsiblePerson(positions = Some(positions))

            val newRequest = FakeRequest(POST, routes.PositionWithinBusinessController.post(1).url)
              .withFormUrlEncodedBody(
                "positions[0]" -> SoleProprietor.toString,
                "positions[1]" -> NominatedOfficer.toString
              )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

            val mockCacheMap = mock[Cache]
            when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
              .thenReturn(Future.successful(mockCacheMap))

            val result = controller.post(RecordId)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(controllers.responsiblepeople.routes.PositionWithinBusinessStartDateController.get(RecordId).url)
            )
          }
        }
      }

      "when edit is true" must {

        "redirect to 'When did this person start their role in the business?'" in new Fixture {

          val newRequest = FakeRequest(POST, routes.PositionWithinBusinessController.post(1).url)
            .withFormUrlEncodedBody(
              "positions[0]" -> NominatedOfficer.toString
            )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(hasNominatedOfficer))))
          val mockCacheMap = mock[Cache]
          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(RecordId, true, Some(flowFromDeclaration))(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(
              controllers.responsiblepeople.routes.PositionWithinBusinessStartDateController
                .get(RecordId, true, Some(flowFromDeclaration))
                .url
            )
          )
        }
      }
    }
  }

  "hasNominatedOfficer" must {
    "return true" when {
      "there is nominated officer" in new Fixture {
        ResponsiblePerson.hasNominatedOfficer(Some(Seq(hasNominatedOfficer))) must be(true)
      }

      "one rp is nominated officer" in new Fixture {
        ResponsiblePerson.hasNominatedOfficer(Some(Seq(hasNominatedOfficer, noNominatedOfficer))) must be(true)
      }
    }

    "return false" when {
      "there are no responsible people" in new Fixture {
        ResponsiblePerson.hasNominatedOfficer(Some(Nil)) must be(false)
      }

      "there is no nominated officer" in new Fixture {
        ResponsiblePerson.hasNominatedOfficer(
          Some(Seq(noNominatedOfficer.copy(positions = Some(DefaultValues.noNominatedOfficerPositions))))
        ) must be(false)
      }
    }
  }

  "displayNominatedOfficer" must {
    "return true" when {
      "this responsible person is the nominated officer" in new Fixture {
        val rp = responsiblePersonWithPositionsGen(Some(Set(NominatedOfficer))).sample.get
        ResponsiblePerson.displayNominatedOfficer(rp, true) mustBe true
      }

      "this responsible person is not the nominated officer" when {
        "hasNominatedOfficer is false" in new Fixture {
          val rp = responsiblePersonWithPositionsGen(None).sample.get
          ResponsiblePerson.displayNominatedOfficer(rp, false) mustBe true
        }
      }
    }
    "return false" when {
      "this responsible person is not the nominated officer" when {
        "hasNominatedOfficer is true" in new Fixture {
          val rp = responsiblePersonWithPositionsGen(None).sample.get
          ResponsiblePerson.displayNominatedOfficer(rp, true) mustBe false
        }
      }
    }
  }
}
