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

package views.businessactivities

import models.businessmatching.BusinessActivities
import models.businessmatching.BusinessActivity.{AccountancyServices, MoneyServiceBusiness}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.WhatYouNeedView

class WhatYouNeedViewSpec extends AmlsViewSpec with Matchers {

  lazy val need = app.injector.instanceOf[WhatYouNeedView]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      def view = need(Some(BusinessActivities(Set(AccountancyServices))))

      doc.title must startWith(messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture {
      def view = need(Some(BusinessActivities(Set(AccountancyServices))))

      heading.html    must be(messages("title.wyn"))
      subHeading.html must include(messages("summary.businessactivities"))
    }

    "contain the expected content elements when not ASP" in new ViewFixture {
      def view = need(Some(BusinessActivities(Set(MoneyServiceBusiness))))

      html must include(
        messages("about any business activities that are not covered by the Money Laundering Regulations")
      )
      html must include(
        messages(
          "the net profit you expect in the next 12 months, if you also carry out activities not covered by the regulations"
        )
      )
      html must include(
        messages("the net profit you expect in the next 12 months from the services you’re registering")
      )
      html must include(messages("the franchisor’s name, if your business is a franchise"))
      html must include(messages("how many people work on activities covered by the Money Laundering Regulations"))
      html must include(messages("how many people work for the business"))
      html must include(messages("how you record customer transactions"))
      html must include(messages("if you have written guidance on how to identify and report suspicious activity"))
      html must include(messages("if your business has registered with the National Crime Agency (NCA)"))
      html must include(messages("how you document your risk assessment policy and procedure, if your business has on"))
      html must include(messages("about your professional adviser for Money Laundering Regulations, if you have one"))
    }

    "contain the expected content elements when ASP" in new ViewFixture {
      def view = need(Some(BusinessActivities(Set(AccountancyServices))))

      html must include(
        messages("about any business activities that are not covered by the Money Laundering Regulations")
      )
      html must include(
        messages(
          "the net profit you expect in the next 12 months, if you also carry out activities not covered by the regulations"
        )
      )
      html must include(
        messages("the net profit you expect in the next 12 months from the services you’re registering")
      )
      html must include(messages("the franchisor’s name, if your business is a franchise"))
      html must include(messages("how many people work on activities covered by the Money Laundering Regulations"))
      html must include(messages("how many people work for the business"))
      html must include(messages("how you record customer transactions"))
      html must include(messages("if you have written guidance on how to identify and report suspicious activity"))
      html must include(messages("if your business has registered with the National Crime Agency (NCA)"))
      html must include(messages("how you document your risk assessment policy and procedure, if your business has on"))
    }

    behave like pageWithBackLink(need(Some(BusinessActivities(Set(AccountancyServices)))))
  }
}
