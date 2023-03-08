/*
 * Copyright 2023 HM Revenue & Customs
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
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.what_you_need


class what_you_needSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val what_you_need = app.injector.instanceOf[what_you_need]
    implicit val requestWithToken = addTokenForView()
  }

  "What you need View" must {

    "have a back link" in new ViewFixture {
      def view = what_you_need(1, None, None)
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "Have the correct title" in new ViewFixture {
      def view = what_you_need(1, None, None)

      doc.title must startWith(Messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = what_you_need(1, None, None)

      heading.html must be (Messages("title.wyn"))
      subHeading.html must include (Messages("summary.responsiblepeople"))
    }

    "contain the expected content elements" in new ViewFixture{
      def view = what_you_need(1, None, Some(BusinessActivities(Set(MoneyServiceBusiness))))

      html must include(Messages("responsiblepeople.whatyouneed.requiredinfo"))

      html must include(Messages("their name"))
      html must include(Messages("if they have ever legally changed their name"))
      html must include(Messages("other names they’re known by"))
      html must include(Messages("their date of birth"))
      html must include(Messages("if they’re a UK resident"))
      html must include(Messages("their home addresses for the last 3 years"))
      html must include(Messages("their telephone number and email address"))
      html must include(Messages("their position in the business and start date"))
      html must include(Messages("if they’re a sole proprietor of another business"))
      html must include(Messages("if they’re registered for Self Assessment"))
      html must include(Messages("their experience in the services you’re registering under the Money Laundering Regulations"))
      html must include(Messages("their experience and training in anti-money laundering and counter-terrorism funding"))
      html must include(Messages("if they have passed the fit and proper test"))
      html must include(Messages("previous name and the date their name changed"))
      html must include(Messages("National Insurance number"))
      html must include(Messages("passport number"))
      html must include(Messages("country of birth"))
      html must include(Messages("nationality"))
      html must include(Messages("VAT registration number"))
      html must include(Messages("Self Assessment Unique Taxpayer Reference (UTR) number"))
    }

    "Contain approval check content for MSB or TCSP" in new ViewFixture{
      def view = what_you_need(1, None, Some(BusinessActivities(Set(MoneyServiceBusiness))))

      html must include(Messages("if HMRC has charged your business or another business to do an approval check on them"))
    }
  }
}
