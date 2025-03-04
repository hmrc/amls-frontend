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

package controllers.renewal

import config.AmlsErrorHandler
import controllers.actions.SuccessfulAuthAction
import forms.renewal.InvolvedInOtherDetailsFormProvider
import models.businessmatching.BusinessActivity.{AccountancyServices, EstateAgentBusinessService, MoneyServiceBusiness}
import models.renewal.{InvolvedInOtherYes, Renewal}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.RenewalService
import services.RenewalService.BusinessAndOtherActivities
import utils.AmlsSpec
import views.html.renewal.InvolvedInOtherDetailsView

import scala.concurrent.Future

class InvolvedInOtherDetailsControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture {
    self =>

    val request = addToken(authRequest)

    lazy val mockRenewalService = mock[RenewalService]
    lazy val view               = inject[InvolvedInOtherDetailsView]
    val controller              = new InvolvedInOtherDetailsController(
      SuccessfulAuthAction,
      commonDependencies,
      mockMcc,
      inject[InvolvedInOtherDetailsFormProvider],
      view,
      mockRenewalService,
      inject[AmlsErrorHandler]
    )
  }

  "InvolvedInOtherDetailsController" must {
    "display the 'What other activities has your business been involved in page'" in new Fixture {
      when(mockRenewalService.getRenewal(any()))
        .thenReturn(Future.successful(Some(Renewal(Some(InvolvedInOtherYes(""))))))

      val result = controller.get()(request)

      status(result)          must be(OK)
      contentAsString(result) must include(messages("renewal.involvedinother.details.title"))
    }

    "display the involved in other with pre populated data" in new Fixture {
      when(mockRenewalService.getRenewal(any()))
        .thenReturn(Future.successful(Some(Renewal(Some(InvolvedInOtherYes("Selling software packages"))))))

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include("Selling software packages")
    }

    "display error page" when {
      "no renewal can be found" in new Fixture {
        when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))
        val result = controller.get()(request)
        status(result) must be(INTERNAL_SERVER_ERROR)
      }

      "no business activities can be found" in new Fixture {
        val postRequest = FakeRequest(POST, routes.InvolvedInOtherDetailsController.post().url)
          .withFormUrlEncodedBody("details" -> "trading")

        when(mockRenewalService.updateOtherBusinessActivities(any(), any())).thenReturn(Future.successful(None))

        val result = controller.post()(postRequest)
        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }

    "redirect to the Business Turnover page" in new Fixture {
      val postRequest = FakeRequest(POST, routes.InvolvedInOtherDetailsController.post().url)
        .withFormUrlEncodedBody("details" -> "trading")

      val businessAndOtherActivities: BusinessAndOtherActivities =
        BusinessAndOtherActivities(
          Set(AccountancyServices, EstateAgentBusinessService, MoneyServiceBusiness),
          InvolvedInOtherYes("trading")
        )

      when(mockRenewalService.updateOtherBusinessActivities(any(), any()))
        .thenReturn(Future.successful(Some(businessAndOtherActivities)))

      val result = controller.post()(postRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.BusinessTurnoverController.get().url))
    }

    "respond with Bad Request" when {
      "no details are given" in new Fixture {
        val postRequest = FakeRequest(POST, routes.InvolvedInOtherDetailsController.post().url)
          .withFormUrlEncodedBody("details" -> "")

        val result = controller.post()(postRequest)
        status(result) mustBe BAD_REQUEST

        val document = Jsoup.parse(contentAsString(result))
        document.select("a[href=#details]").html() must include(
          messages("error.required.renewal.ba.involved.in.other.text")
        )
      }

      "empty details are given" in new Fixture {
        val postRequest = FakeRequest(POST, routes.InvolvedInOtherDetailsController.post().url)
          .withFormUrlEncodedBody("details" -> "")

        val result = controller.post()(postRequest)
        status(result) mustBe BAD_REQUEST

        val document = Jsoup.parse(contentAsString(result))
        document.select("a[href=#details]").html() must include(
          messages("error.required.renewal.ba.involved.in.other.text")
        )
      }

      "too many details are given" in new Fixture {
        val longDetails =
          """
            |Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur pulvinar mauris nibh, in porta arcu elementum vitae.
            |Maecenas posuere sodales massa non lobortis. Mauris purus justo, vulputate sed risus a, molestie ornare magna.
            |Duis at convallis augue, vitae lacinia odio. Donec non vulputate metus, eget scelerisque dui.
            |Morbi in tellus eu velit mollis tristique. Nam malesuada maximus lorem in iaculis.
            |In quis tempor nunc, a feugiat nibh. Donec malesuada viverra ex a hendrerit.
            |""".stripMargin
        val postRequest = FakeRequest(POST, routes.InvolvedInOtherDetailsController.post().url)
          .withFormUrlEncodedBody("details" -> longDetails)

        val result = controller.post()(postRequest)
        status(result) mustBe BAD_REQUEST

        val document = Jsoup.parse(contentAsString(result))
        document.select("a[href=#details]").html() must include(
          messages("error.invalid.maxlength.255.renewal.ba.involved.in.other")
        )
      }
    }
  }
}
