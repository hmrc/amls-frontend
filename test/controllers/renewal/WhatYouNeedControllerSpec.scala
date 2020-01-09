/*
 * Copyright 2020 HM Revenue & Customs
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

import controllers.actions.SuccessfulAuthAction
import models.registrationprogress.{Completed, NotStarted, Section}
import models.renewal.Renewal
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.when
import play.api.i18n._
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.HeaderCarrier
import utils.AmlsSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class WhatYouNeedControllerSpec extends AmlsSpec {

  trait Fixture {
    self =>

    val renewalService = mock[RenewalService]

    val controller = new WhatYouNeedController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService = renewalService,
      cc = mock[MessagesControllerComponents])
  }
  "WhatYouNeedController" must {

    "get" must {

      "load the page" in new Fixture {

        when {
          renewalService.getSection(any())(any[HeaderCarrier], any())
        } thenReturn Future.successful(Section("renewal", NotStarted, Renewal().hasChanged, controllers.renewal.routes.SummaryController.get()))

        val result = controller.get(requestWithToken)
        status(result) must be(OK)

        val pageTitle = Messages("title.wyn") + " - " +
          Messages("summary.renewal") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        contentAsString(result) must include(pageTitle)
      }

      "redirect to progress page if renewal has been started" in new Fixture {

        when {
          renewalService.getSection(meq("internalId"))(any(), any())
        } thenReturn Future.successful(Section("renewal", Completed, Renewal().hasChanged, controllers.renewal.routes.SummaryController.get()))

        val result = controller.get(requestWithToken)
        status(result) must be(SEE_OTHER)

      }
    }
  }
}
