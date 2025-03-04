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

package views.responsiblepeople

import models.businessmatching.BusinessActivities
import models.businessmatching.BusinessActivity.MoneyServiceBusiness
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.WhatYouNeedView

class WhatYouNeedViewSpec extends AmlsViewSpec with Matchers {

  lazy val what_you_need = inject[WhatYouNeedView]

  val call                                                  = controllers.responsiblepeople.routes.PersonNameController.get(1)
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "What you need View" must {

    "Have the correct title" in new ViewFixture {
      def view = what_you_need(call, None)

      doc.title must startWith(messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture {
      def view = what_you_need(call, None)

      heading.html    must be(messages("title.wyn"))
      subHeading.html must include(messages("summary.responsiblepeople"))
    }

    "contain the expected content elements" in new ViewFixture {
      def view = what_you_need(call, Some(BusinessActivities(Set(MoneyServiceBusiness))))

      html must include(messages("responsiblepeople.whatyouneed.requiredinfo"))

      html must include(messages("their name"))
      html must include(messages("if they have ever legally changed their name"))
      html must include(messages("other names they’re known by"))
      html must include(messages("their date of birth"))
      html must include(messages("if they’re a UK resident"))
      html must include(messages("their home addresses for the last 3 years"))
      html must include(messages("their telephone number and email address"))
      html must include(messages("their position in the business and start date"))
      html must include(messages("if they’re a sole proprietor of another business"))
      html must include(messages("if they’re registered for Self Assessment"))
      html must include(
        messages("their experience in the services you’re registering under the Money Laundering Regulations")
      )
      html must include(
        messages("their experience and training in anti-money laundering and counter-terrorism funding")
      )
      html must include(messages("if they have passed the fit and proper test"))
      html must include(messages("previous name and the date their name changed"))
      html must include(messages("National Insurance number"))
      html must include(messages("passport number"))
      html must include(messages("country of birth"))
      html must include(messages("nationality"))
      html must include(messages("VAT registration number"))
      html must include(messages("Self Assessment Unique Taxpayer Reference (UTR) number"))
    }

    "Contain approval check content for MSB or TCSP" in new ViewFixture {
      def view = what_you_need(call, Some(BusinessActivities(Set(MoneyServiceBusiness))))

      html must include(
        messages("if HMRC has charged your business or another business to do an approval check on them")
      )
    }

    behave like pageWithBackLink(what_you_need(call, None))
  }
}
