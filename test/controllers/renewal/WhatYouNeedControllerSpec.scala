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

import controllers.actions.SuccessfulAuthAction
import models.businessmatching.BusinessActivity.{MoneyServiceBusiness, TelephonePaymentService, TrustAndCompanyServices}
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.registrationprogress.{Completed, NotStarted, TaskRow}
import models.renewal.Renewal
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import services.RenewalService
import utils.{AmlsSpec, DependencyMocks}
import views.html.renewal.WhatYouNeedView

import scala.concurrent.Future

class WhatYouNeedControllerSpec extends AmlsSpec {

  trait Fixture extends DependencyMocks {
    self =>

    val renewalService = mock[RenewalService]
    lazy val view      = app.injector.instanceOf[WhatYouNeedView]
    val controller     = new WhatYouNeedController(
      dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService = renewalService,
      cc = mock[MessagesControllerComponents],
      view = view
    )
  }

  "WhatYouNeedController" must {

    "get" must {

      "load the page" in new Fixture {
        val BusinessActivitiesModel =
          BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
        val bm                      = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))

        when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any())) thenReturn (Future.successful(
          bm
        ))

        when {
          renewalService.getTaskRow(any())(any())
        } thenReturn Future.successful(
          TaskRow(
            "renewal",
            controllers.renewal.routes.SummaryController.get.url,
            Renewal().hasChanged,
            NotStarted,
            TaskRow.notStartedTag
          )
        )

        val result = controller.get(requestWithToken)
        status(result) must be(OK)

        val pageTitle = messages("title.wyn") + " - " +
          messages("summary.renewal") + " - " +
          messages("title.amls") + " - " + messages("title.gov")

        contentAsString(result) must include(pageTitle)
      }

      "redirect to progress page if renewal has been started" in new Fixture {
        val BusinessActivitiesModel =
          BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
        val bm                      = Some(BusinessMatching(activities = Some(BusinessActivitiesModel)))

        when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any())) thenReturn (Future.successful(
          bm
        ))

        when {
          renewalService.getTaskRow(meq("internalId"))(any())
        } thenReturn Future.successful(
          TaskRow(
            "renewal",
            controllers.renewal.routes.SummaryController.get.url,
            Renewal().hasChanged,
            Completed,
            TaskRow.completedTag
          )
        )

        val result = controller.get(requestWithToken)
        status(result) must be(SEE_OTHER)

      }

      "throw an error when data cannot be fetched" in new Fixture {
        when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any())) thenReturn (Future.successful(
          None
        ))

        when {
          renewalService.getTaskRow(meq("internalId"))(any())
        } thenReturn Future.successful(
          TaskRow(
            "renewal",
            controllers.renewal.routes.SummaryController.get.url,
            Renewal().hasChanged,
            Completed,
            TaskRow.completedTag
          )
        )

        a[Exception] must be thrownBy {
          ScalaFutures.whenReady(controller.get(requestWithToken))(x => x)
        }
      }

    }

  }

}
