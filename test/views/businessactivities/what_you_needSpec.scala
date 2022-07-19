/*
 * Copyright 2022 HM Revenue & Customs
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

import forms.{EmptyForm, Form2}
import models.businessmatching.{AccountancyServices, BusinessActivities, MoneyServiceBusiness}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.what_you_need

class what_you_needSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val need = app.injector.instanceOf[what_you_need]
    implicit val requestWithToken = addTokenForView()
  }

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      def view = need("/next-page", Some(BusinessActivities(Set(AccountancyServices))))

      doc.title must startWith(Messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = need("/next-page", Some(BusinessActivities(Set(AccountancyServices))))

      heading.html must be (Messages("title.wyn"))
      subHeading.html must include (Messages("summary.businessactivities"))
    }

    "contain the expected content elements when not ASP" in new ViewFixture{
      def view = need("/next-page", Some(BusinessActivities(Set(MoneyServiceBusiness))))

      html must include(Messages("about any business activities that are not covered by the Money Laundering Regulations"))
      html must include(Messages("the net profit you expect in the next 12 months, if you also carry out activities not covered by the regulations"))
      html must include(Messages("the net profit you expect in the next 12 months from the services you’re registering"))
      html must include(Messages("the franchisor’s name, if your business is a franchise"))
      html must include(Messages("how many people work on activities covered by the Money Laundering Regulations"))
      html must include(Messages("how many people work for the business"))
      html must include(Messages("how you record customer transactions"))
      html must include(Messages("if you have written guidance on how to identify and report suspicious activity"))
      html must include(Messages("if your business has registered with the National Crime Agency (NCA)"))
      html must include(Messages("how you document your risk assessment policy and procedure, if your business has on"))
      html must include(Messages("about your professional adviser for Money Laundering Regulations, if you have one"))
    }

    "contain the expected content elements when ASP" in new ViewFixture{
      def view = need("/next-page", Some(BusinessActivities(Set(AccountancyServices))))

      html must include(Messages("about any business activities that are not covered by the Money Laundering Regulations"))
      html must include(Messages("the net profit you expect in the next 12 months, if you also carry out activities not covered by the regulations"))
      html must include(Messages("the net profit you expect in the next 12 months from the services you’re registering"))
      html must include(Messages("the franchisor’s name, if your business is a franchise"))
      html must include(Messages("how many people work on activities covered by the Money Laundering Regulations"))
      html must include(Messages("how many people work for the business"))
      html must include(Messages("how you record customer transactions"))
      html must include(Messages("if you have written guidance on how to identify and report suspicious activity"))
      html must include(Messages("if your business has registered with the National Crime Agency (NCA)"))
      html must include(Messages("how you document your risk assessment policy and procedure, if your business has on"))
    }

    "have a back link" in new ViewFixture {
      val form2: Form2[_] = EmptyForm

      def view = need("/next-page", Some(BusinessActivities(Set(AccountancyServices))))

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}